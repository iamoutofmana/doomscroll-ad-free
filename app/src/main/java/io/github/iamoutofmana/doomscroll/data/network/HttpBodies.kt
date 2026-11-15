package io.github.iamoutofmana.doomscroll.data.network

import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream

/**
 * Reads a response body with a hard byte cap so hostile or misconfigured servers
 * cannot exhaust memory (weakness #3 in the security review).
 */
internal object HttpBodies {
    fun readCapped(
        body: ResponseBody,
        maxBytes: Int,
    ): ByteArray? {
        if (body.contentLength() > maxBytes) return null
        return try {
            body.byteStream().use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > maxBytes) return null
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            }
        } catch (e: Exception) {
            null
        }
    }
}
