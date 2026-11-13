package io.github.iamoutofmana.doomscroll.data

import io.github.iamoutofmana.doomscroll.data.network.GnRedirectResolver
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class GnRedirectResolverTest {
    private val resolver = GnRedirectResolver(OkHttpClient())

    @Test
    fun extractsArticleId() {
        assertEquals("CBMiTEST", resolver.articleId("https://news.google.com/rss/articles/CBMiTEST?oc=5"))
        assertNull(resolver.articleId("https://example.com/feed/item"))
    }

    @Test
    fun decodesClassicProtobufPayloadOffline() {
        val url = "https://www.reuters.com/world/example-article"
        val urlBytes = url.toByteArray(Charsets.UTF_8)
        val payload = byteArrayOf(0x08, 0x13, 0x22, urlBytes.size.toByte()) + urlBytes
        val id = Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
        assertEquals(url, resolver.decodeOffline(id))
    }

    @Test
    fun returnsNullForOpaqueNewStylePayload() {
        val opaque = "AU_yqLPCxyz123"
        val payload = byteArrayOf(0x08, 0x13, 0x22, opaque.length.toByte()) + opaque.toByteArray()
        val id = Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
        assertNull(resolver.decodeOffline(id))
    }

    @Test
    fun invalidBase64ReturnsNull() {
        assertNull(resolver.decodeOffline("!!!not-base64!!!"))
    }

    @Test
    fun parsesBatchexecuteEnvelope() {
        val body = """)]}'

[["wrb.fr","FbvAvj","[\"rbr0\",null,null,\"https://www.theverge.com/2026/11/13/example\"]",null,null,null,"generic"]]"""
        assertEquals(
            "https://www.theverge.com/2026/11/13/example",
            resolver.parseBatchexecuteBody(body),
        )
    }

    @Test
    fun batchexecuteGarbageReturnsNull() {
        assertNull(resolver.parseBatchexecuteBody("total garbage"))
    }
}
