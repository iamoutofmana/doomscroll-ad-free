package io.github.iamoutofmana.doomscroll.data

import io.github.iamoutofmana.doomscroll.data.network.FeedFetcher
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * HTTP-layer hardening tests: conditional GET, transport requirements, and the
 * response-size cap that protects against hostile/oversized payloads.
 */
class FeedFetcherTest {
    private lateinit var server: MockWebServer
    private lateinit var fetcher: FeedFetcher

    @Before
    fun setUp() {
        val heldCertificate = HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .addSubjectAlternativeName("127.0.0.1")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(heldCertificate)
            .build()
        server = MockWebServer()
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        server.start()

        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(heldCertificate.certificate)
            .build()
        val client = OkHttpClient.Builder()
            .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
            .build()
        fetcher = FeedFetcher(client)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun returnsContentWithValidators() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/rss+xml; charset=utf-8")
                .addHeader("ETag", "\"v1\"")
                .setBody("<rss version=\"2.0\"><channel><title>T</title></channel></rss>"),
        )

        val result = fetcher.fetch(server.url("/feed.xml").toString(), null, null)

        val content = result as FeedFetcher.Result.Content
        assertTrue(content.body.startsWith("<rss"))
        assertEquals("\"v1\"", content.etag)
    }

    @Test
    fun handlesNotModified() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(304))

        val result = fetcher.fetch(server.url("/feed.xml").toString(), "\"v1\"", null)

        assertTrue(result is FeedFetcher.Result.NotModified)
    }

    @Test
    fun rejectsOversizedBodies() = runBlocking {
        val huge = "x".repeat(6 * 1024 * 1024)
        server.enqueue(MockResponse().setResponseCode(200).setBody(huge))

        val result = fetcher.fetch(server.url("/huge.xml").toString(), null, null)

        assertTrue("oversized feed must be rejected", result is FeedFetcher.Result.Failure)
    }

    @Test
    fun rejectsCleartextFeeds() = runBlocking {
        val result = fetcher.fetch("http://example.com/feed.xml", null, null)

        assertTrue("http feeds must be refused", result is FeedFetcher.Result.Failure)
    }

    @Test
    fun reportsHttpErrors() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = fetcher.fetch(server.url("/feed.xml").toString(), null, null)

        assertTrue(result is FeedFetcher.Result.Failure)
        assertEquals("HTTP 500", (result as FeedFetcher.Result.Failure).message)
    }
}
