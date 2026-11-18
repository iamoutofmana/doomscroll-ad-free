package io.github.iamoutofmana.doomscroll.data

import io.github.iamoutofmana.doomscroll.data.network.FeedDiscovery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedDiscoveryTest {
    @Test
    fun findsAlternateRssLink() {
        val html = """
            <html><head>
              <title>Example News</title>
              <link rel="alternate" type="application/rss+xml" href="/feed.xml" />
            </head></html>
        """.trimIndent()

        val found = FeedDiscovery.fromHtml(html, "https://example.com/news/story")

        assertEquals("https://example.com/feed.xml", found?.feedUrl)
        assertEquals("Example News", found?.pageTitle)
    }

    @Test
    fun findsAtomAndJsonFeedTypes() {
        val atom = """<html><head><link rel="alternate" type="application/atom+xml" href="https://example.com/atom"></head></html>"""
        val json = """<html><head><link rel="alternate" type="application/feed+json" href="https://example.com/feed.json"></head></html>"""

        assertEquals("https://example.com/atom", FeedDiscovery.fromHtml(atom, "https://example.com/")?.feedUrl)
        assertEquals("https://example.com/feed.json", FeedDiscovery.fromHtml(json, "https://example.com/")?.feedUrl)
    }

    @Test
    fun ignoresNonFeedAlternates() {
        val html = """<html><head><link rel="alternate" type="text/html" href="https://example.com/amp"></head></html>"""
        assertNull(FeedDiscovery.fromHtml(html, "https://example.com/"))
    }

    @Test
    fun normalizesBareHostsAndUpgradesHttp() {
        assertEquals("https://example.com", FeedDiscovery.normalizeUrl("example.com"))
        assertEquals("https://example.com/feed", FeedDiscovery.normalizeUrl("http://example.com/feed"))
        assertEquals("https://example.com/a", FeedDiscovery.normalizeUrl("https://example.com/a"))
        assertNull(FeedDiscovery.normalizeUrl("ftp://example.com"))
        assertNull(FeedDiscovery.normalizeUrl("   "))
    }

    @Test
    fun detectsXAndTwitter() {
        assertTrue(FeedDiscovery.isXUrl("https://x.com/someone"))
        assertTrue(FeedDiscovery.isXUrl("https://twitter.com/someone/status/1"))
        assertFalse(FeedDiscovery.isXUrl("https://example.com/x.com"))
    }

    @Test
    fun candidatePathsIncludeCommonLocationsAndPageDirectory() {
        val candidates = FeedDiscovery.candidatesFor("https://example.com/blog/post")

        assertTrue(candidates.contains("https://example.com/feed"))
        assertTrue(candidates.contains("https://example.com/rss.xml"))
        assertTrue(candidates.contains("https://example.com/blog/post/feed"))
        assertEquals(candidates.size, candidates.toSet().size)
    }

    @Test
    fun recognizesFeedBodies() {
        assertTrue(FeedDiscovery.looksLikeFeed("<?xml version=\"1.0\"?><rss version=\"2.0\">"))
        assertTrue(FeedDiscovery.looksLikeFeed("<feed xmlns=\"http://www.w3.org/2005/Atom\">"))
        assertTrue(FeedDiscovery.looksLikeFeed("<rdf:RDF></rdf:RDF>"))
        assertTrue(FeedDiscovery.looksLikeFeed("{\"version\": \"https://jsonfeed.org/version/1.1\", \"items\": []}"))
        assertFalse(FeedDiscovery.looksLikeFeed("<html><body>hello</body></html>"))
    }

    @Test
    fun extractsFeedTitle() {
        assertEquals("My Feed", FeedDiscovery.feedTitle("<rss><channel><title><![CDATA[My Feed]]></title></channel></rss>"))
        assertEquals("Tom & Jerry", FeedDiscovery.feedTitle("<feed><title>Tom &amp; Jerry</title></feed>"))
        assertEquals("JSON Feed", FeedDiscovery.feedTitle("{\"version\": \"1.1\", \"title\": \"JSON Feed\", \"items\": []}"))
        assertNull(FeedDiscovery.feedTitle("<rss></rss>"))
    }
}
