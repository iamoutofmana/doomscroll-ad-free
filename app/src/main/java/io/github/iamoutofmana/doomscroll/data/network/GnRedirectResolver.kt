package io.github.iamoutofmana.doomscroll.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Base64

/**
 * Resolves news.google.com redirect links to publisher URLs (plan §2.3 / §11).
 *
 * Three tiers, cheapest first:
 * 1. Offline protobuf/base64 decode of classic article IDs.
 * 2. Google's internal batchexecute endpoint — the same technique used by open-source
 *    decoders (googlenewsdecoder et al.) for post-2024 article IDs.
 * 3. Give up -> callers open the redirect URL directly, which any browser resolves fine.
 *
 * Everything is best-effort: failures return null and never throw into the sync loop.
 * SyncEngine applies a circuit breaker after repeated failures.
 */
class GnRedirectResolver(private val client: OkHttpClient) {
    suspend fun resolve(redirectUrl: String): String? {
        if (!redirectUrl.contains("news.google.com")) return null
        val id = articleId(redirectUrl) ?: return null
        decodeOffline(id)?.let { return it }
        return resolveViaBatchexecute(redirectUrl, id)
    }

    fun articleId(url: String): String? =
        ARTICLE_ID.find(url)?.groupValues?.get(1)

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

    /** Parses the batchexecute JS envelope; public for unit testing with canned responses. */
    fun parseBatchexecuteBody(body: String): String? {
        val line = body.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("[") }
            ?: return fallbackUrl(body)
        return try {
            val root = Json.parseToJsonElement(line) as? JsonArray ?: return fallbackUrl(body)
            var found: String? = null
            for (entry in root) {
                val arr = entry as? JsonArray ?: continue
                if (arr.size > 2 && (arr[1] as? JsonPrimitive)?.contentOrNull == RPC_ID) {
                    val payload = (arr[2] as? JsonPrimitive)?.contentOrNull ?: continue
                    val inner = Json.parseToJsonElement(payload) as? JsonArray ?: continue
                    for (el in inner) {
                        val s = (el as? JsonPrimitive)?.contentOrNull
                        if (s != null && s.startsWith("http")) {
                            found = s
                            break
                        }
                    }
                }
                if (found != null) break
            }
            found ?: fallbackUrl(body)
        } catch (e: Exception) {
            fallbackUrl(body)
        }
    }

    private suspend fun resolveViaBatchexecute(
        pageUrl: String,
        articleId: String,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val page = client.newCall(
                Request.Builder().url(pageUrl).header("User-Agent", BROWSER_UA).build(),
            ).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return@withContext null

            val sg = ATTR_SG.find(page)?.groupValues?.get(1) ?: return@withContext null
            val ts = ATTR_TS.find(page)?.groupValues?.get(1) ?: return@withContext null

            val inner = "[\"$articleId\",\"$ts\",\"$sg\"]"
            val escaped = inner.replace("\\", "\\\\").replace("\"", "\\\"")
            val freq = "[[[\"$RPC_ID\",\"$escaped\",null,\"generic\"]]]"
            val form = FormBody.Builder().add("f.req", freq).build()

            val body = client.newCall(
                Request.Builder()
                    .url(BATCHEXECUTE_URL)
                    .header("User-Agent", BROWSER_UA)
                    .post(form)
                    .build(),
            ).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return@withContext null

            parseBatchexecuteBody(body)
        } catch (e: Exception) {
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
        const val RPC_ID = "FbvAvj"
        const val BATCHEXECUTE_URL = "https://news.google.com/_/DotsSplashUi/data/batchexecute"
        const val BROWSER_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
        val ARTICLE_ID = Regex("""/(?:articles?|read)/([^/?#]+)""")
        val ATTR_SG = Regex("""data-n-a-sg="([^"]+)"""")
        val ATTR_TS = Regex("""data-n-a-ts="([^"]+)"""")
        val URL_IN_BODY = Regex("""https?://(?:[^"\\\s]|\\.)+""")
    }
}
