package io.github.iamoutofmana.doomscroll.data

import io.github.iamoutofmana.doomscroll.data.network.GnRedirectResolver
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
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
    fun buildFreqUsesCurrentRpcAndEnvelope() {
        val freq = resolver.buildFreq("CBMiTEST", "1794759515", "AdwtSYPwdNUyUTIzP")

        // Structure: [[ envelope ]] where envelope = [rpcId, payloadJsonString, null, requestId]
        val outer = Json.parseToJsonElement(freq).jsonArray
        val envelope = outer[0].jsonArray[0].jsonArray
        assertEquals("Fbv4je", envelope[0].jsonPrimitive.content)
        assertEquals(JsonNull, envelope[2])
        assertEquals("0", envelope[3].jsonPrimitive.content)

        // Payload: ["garturlreq", <context>, articleId, timestamp(number), signature]
        val payload = Json.parseToJsonElement(envelope[1].jsonPrimitive.content).jsonArray
        assertEquals("garturlreq", payload[0].jsonPrimitive.content)
        assertEquals("CBMiTEST", payload[2].jsonPrimitive.content)
        assertEquals(1794759515L, payload[3].jsonPrimitive.long)
        assertEquals("AdwtSYPwdNUyUTIzP", payload[4].jsonPrimitive.content)
    }

    @Test
    fun parsesBatchexecuteEnvelope() {
        val body = """)]}'

[["wrb.fr","Fbv4je","[\"garturlres\",\"https://www.engadget.com/2315452/motorola-signature-27-hands-on/\",null,null,[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,[true]]]",null,null,null,"0"]]"""
        assertEquals(
            "https://www.engadget.com/2315452/motorola-signature-27-hands-on/",
            resolver.parseBatchexecuteBody(body),
        )
    }

    @Test
    fun toleratesTrailingMarkerRows() {
        val body = """)]}'

[["wrb.fr","Fbv4je","[\"garturlres\",\"https://example.com/story\",null]",null,null,null,"0"],["di",123],["e",456]]"""
        assertEquals("https://example.com/story", resolver.parseBatchexecuteBody(body))
    }

    @Test
    fun batchexecuteGarbageReturnsNull() {
        assertNull(resolver.parseBatchexecuteBody("total garbage"))
    }
}
