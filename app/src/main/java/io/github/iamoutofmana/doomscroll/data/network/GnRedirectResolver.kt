package io.github.iamoutofmana.doomscroll.data.network

import android.util.Log
import io.github.iamoutofmana.doomscroll.BuildConfig
import io.github.iamoutofmana.doomscroll.domain.AppMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Base64

/**
 * Resolves news.google.com redirect links to publisher URLs.
 *
 * Three tiers, cheapest first:
 * 1. Offline protobuf/base64 decode of classic article IDs.
 * 2. Google's internal batchexecute endpoint — the format used by the maintained
 *    open-source decoders (googlenewsdecoder): rpcid `Fbv4je` with a `garturlreq`
 *    envelope built from the article page's `data-n-a-sg` / `data-n-a-ts` attributes.
 * 3. Give up -> callers open the redirect URL directly (any browser resolves it).
 *
 * Everything is best-effort: failures return null and never throw into the sync loop.
 * SyncEngine applies a circuit breaker after repeated failures.
 */
class GnRedirectResolver(private val client: OkHttpClient) {
    suspend fun resolve(redirectUrl: String): String? {
        if (!redirectUrl.contains("news.google.com")) return null
        val id = articleId(redirectUrl) ?: return null
        decodeOffline(id)?.let { return it }
        return resolveViaBatchexecute(id)
    }

    fun articleId(url: String): String? = ARTICLE_ID.find(url)?.groupValues?.get(1)

    /**
     * Classic IDs are base64url-encoded protobuf whose string field is the publisher URL.
     * Newer opaque IDs (AU_...) yield no http string -> null -> batchexecute tier.
     */
    fun decodeOffline(id: String): String? {
        val bytes = try {
            Base64.getUrlDecoder().decode(padBase64(id))
        } catch (e: IllegalArgumentException) {
            return null
        }
        var i = 0
        while (i < bytes.size) {
            val tag = bytes[i].toInt() and 0xFF
            i++
            when (tag and 0x07) {
                0 -> { // varint field: skip value
                    while (i < bytes.size && bytes[i].toInt() and 0x80 != 0) i++
                    i++
                }
                2 -> { // length-delimited field
                    var len = 0
                    var shift = 0
                    while (i < bytes.size) {
                        val b = bytes[i].toInt() and 0xFF
                        i++
                        len = len or ((b and 0x7F) shl shift)
                        shift += 7
                        if (b and 0x80 == 0) break
                    }
                    if (len in 8..2048 && i + len <= bytes.size) {
                        val candidate = String(bytes, i, len, Charsets.ISO_8859_1)
                        if ((candidate.startsWith("http://") || candidate.startsWith("https://")) &&
                            candidate.all { it.code in 0x20..0x7E }
                        ) {
                            return candidate
                        }
                    }
                    i += len.coerceAtLeast(0)
                }
                5 -> i += 4 // 32-bit field
                1 -> i += 8 // 64-bit field
                else -> return null // unknown wire type: opaque new-format payload
            }
        }
        return null
    }

    /**
     * Builds the `f.req` payload for one article. Pure so tests can pin the wire format.
     * Mirrors googlenewsdecoder's `build_batchexecute_body` (Fbv4je + garturlreq).
     */
    fun buildFreq(
        articleId: String,
        timestamp: String,
        signature: String,
    ): String {
        val ts = timestamp.toLongOrNull()
        val requestContext = buildJsonArray {
            add(
                buildJsonArray {
                    add(JsonPrimitive("X"))
                    add(JsonPrimitive("X"))
                    add(
                        buildJsonArray {
                            add(JsonPrimitive("X"))
                            add(JsonPrimitive("X"))
                        },
                    )
                    add(JsonNull)
                    add(JsonNull)
                    add(JsonPrimitive(1))
                    add(JsonPrimitive(1))
                    add(JsonPrimitive("US:en"))
                    add(JsonNull)
                    add(JsonPrimitive(1))
                    add(JsonNull)
                    add(JsonNull)
                    add(JsonNull)
                    add(JsonNull)
                    add(JsonNull)
                    add(JsonPrimitive(0))
                    add(JsonPrimitive(1))
                },
            )
            add(JsonPrimitive("X"))
            add(JsonPrimitive("X"))
            add(JsonPrimitive(1))
            add(
                buildJsonArray {
                    add(JsonPrimitive(1))
                    add(JsonPrimitive(1))
                    add(JsonPrimitive(1))
                },
            )
            add(JsonPrimitive(1))
            add(JsonPrimitive(1))
            add(JsonNull)
            add(JsonPrimitive(0))
            add(JsonPrimitive(0))
            add(JsonNull)
            add(JsonPrimitive(0))
        }
        val payload = buildJsonArray {
            add(JsonPrimitive("garturlreq"))
            add(requestContext)
            add(JsonPrimitive(articleId))
            add(if (ts != null) JsonPrimitive(ts) else JsonPrimitive(timestamp))
            add(JsonPrimitive(signature))
        }
        val envelope = buildJsonArray {
            add(JsonPrimitive(RPC_ID))
            add(JsonPrimitive(payload.toString()))
            add(JsonNull)
            add(JsonPrimitive("0"))
        }
        return buildJsonArray { add(buildJsonArray { add(envelope) }) }.toString()
    }

    /** Parses the batchexecute JS envelope; public for unit testing with canned responses. */
    fun parseBatchexecuteBody(body: String): String? {
        val cleaned = body
            .let { if (it.contains("\n\n")) it.substringAfter("\n\n") else it }
            .trimStart()
            .let { if (it.startsWith(")]}'")) it.substringAfter('\n', it.removePrefix(")]}'")).trimStart() else it }
        return try {
            val rows = Json.parseToJsonElement(cleaned) as? JsonArray
                ?: return fallbackUrl(body)
            var list = rows.toList()
            // Google appends marker rows ("di" / "e") at the end of the response.
            while (list.isNotEmpty()) {
                val last = list.last() as? JsonArray ?: break
                val marker = (last.firstOrNull() as? JsonPrimitive)?.contentOrNull
                if (marker == "di" || marker == "e") list = list.dropLast(1) else break
            }
            for (row in list) {
                val arr = row as? JsonArray ?: continue
                if (arr.size < 3) continue
                val row0 = (arr[0] as? JsonPrimitive)?.contentOrNull
                val row1 = (arr[1] as? JsonPrimitive)?.contentOrNull
                if (row0 != "wrb.fr" && row1 != RPC_ID) continue
                val payloadText = (arr[2] as? JsonPrimitive)?.contentOrNull ?: continue
                val payload = runCatching { Json.parseToJsonElement(payloadText) as? JsonArray }.getOrNull()
                    ?: continue
                val marker = (payload.firstOrNull() as? JsonPrimitive)?.contentOrNull
                val url = (payload.getOrNull(1) as? JsonPrimitive)?.contentOrNull
                if (marker == "garturlres" && url != null && url.startsWith("http")) return url
            }
            fallbackUrl(body)
        } catch (e: Exception) {
            fallbackUrl(body)
        }
    }

    private suspend fun resolveViaBatchexecute(articleId: String): String? = withContext(Dispatchers.IO) {
        try {
            // /articles/ and /read/ redirect to a sorry page for non-browser clients;
            // /rss/articles/ with locale params returns the real page.
            val pageUrl = "$PAGE_URL_PREFIX$articleId?hl=en-US&gl=US&ceid=US:en"
            val page = client.newCall(
                Request.Builder().url(pageUrl).header("User-Agent", AppMetadata.BROWSER_UA).build(),
            ).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return@withContext null

            val sg = ATTR_SG.find(page)?.groupValues?.get(1) ?: return@withContext null
            val ts = ATTR_TS.find(page)?.groupValues?.get(1) ?: return@withContext null

            val form = FormBody.Builder().add("f.req", buildFreq(articleId, ts, sg)).build()
            val response = client.newCall(
                Request.Builder()
                    .url(BATCHEXECUTE_URL)
                    .header("User-Agent", AppMetadata.BROWSER_UA)
                    .post(form)
                    .build(),
            ).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return@withContext null

            parseBatchexecuteBody(response).also { url ->
                if (BuildConfig.DEBUG) Log.d(LOG_TAG, "GN redirect $articleId -> ${url ?: "UNRESOLVED"}")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "GN redirect $articleId failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun fallbackUrl(body: String): String? =
        URL_IN_BODY.findAll(body)
            .map { it.value.replace("\\/", "/").trimEnd('"', '\\', ',') }
            .firstOrNull {
                it.startsWith("http") && !it.contains("news.google.com") && !it.contains("google.com/_/")
            }

    private fun padBase64(id: String): String {
        var s = id
        while (s.length % 4 != 0) s += "="
        return s
    }

    private companion object {
        const val LOG_TAG = "DoomscrollGn"
        const val RPC_ID = "Fbv4je"
        const val BATCHEXECUTE_URL = "https://news.google.com/_/DotsSplashUi/data/batchexecute"
        const val PAGE_URL_PREFIX = "https://news.google.com/rss/articles/"
        val ARTICLE_ID = Regex("""/(?:articles?|read)/([^/?#]+)""")
        val ATTR_SG = Regex("""data-n-a-sg="([^"]+)"""")
        val ATTR_TS = Regex("""data-n-a-ts="([^"]+)"""")
        val URL_IN_BODY = Regex("""https?://(?:[^"\\\s]|\\.)+""")
    }
}
