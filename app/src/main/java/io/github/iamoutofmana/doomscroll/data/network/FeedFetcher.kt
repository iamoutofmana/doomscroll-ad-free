package io.github.iamoutofmana.doomscroll.data.network

import io.github.iamoutofmana.doomscroll.domain.AppMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.Charset

/**
 * Fetches feed URLs with conditional GET (ETag / If-Modified-Since) so unchanged feeds
 * cost almost nothing. HTTPS only — Android blocks cleartext and so do we (plan §9).
 */
class FeedFetcher(private val client: OkHttpClient) {
    sealed interface Result {
        data object NotModified : Result

        data class Content(val body: String, val etag: String?, val lastModified: String?) : Result

        data class Failure(val message: String) : Result
    }

    suspend fun fetch(
        url: String,
        etag: String?,
        lastModified: String?,
    ): Result = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.Failure("Only https:// feeds are supported (Android blocks cleartext HTTP)")
        }
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", AppMetadata.USER_AGENT)
                .apply {
                    etag?.let { header("If-None-Match", it) }
                    lastModified?.let { header("If-Modified-Since", it) }
                }
                .build()
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 304 -> Result.NotModified
                    response.code in 200..299 -> {
                        val bytes = response.body?.bytes() ?: ByteArray(0)
                        if (bytes.isEmpty()) {
                            Result.Failure("Empty response body")
                        } else {
                            Result.Content(
                                body = decode(bytes, response.header("Content-Type")),
                                etag = response.header("ETag"),
                                lastModified = response.header("Last-Modified"),
                            )
                        }
                    }
                    else -> Result.Failure("HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Result.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    internal fun decode(
        bytes: ByteArray,
        contentType: String?,
    ): String {
        // BOM sniffing first — feeds lie about charset often enough.
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        val charset = contentType
            ?.let { ct -> CHARSET_PARAM.find(ct)?.groupValues?.get(1) }
            ?.let { name -> runCatching { Charset.forName(name.trim('"')) }.getOrNull() }
        return String(bytes, charset ?: Charsets.UTF_8)
    }

    private companion object {
        val CHARSET_PARAM = Regex("""charset\s*=\s*"?([A-Za-z0-9_\-:.]+)"?""", RegexOption.IGNORE_CASE)
    }
}
