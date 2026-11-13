package io.github.iamoutofmana.doomscroll.feedparser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class JsonFeedParserTest {
    private val fixture = """
        {
          "version": "https://jsonfeed.org/version/1.1",
          "title": "Example JSON Feed",
          "home_page_url": "https://example.org/",
          "items": [
            {
              "id": "42",
              "url": "https://example.org/42",
              "title": "Hello world",
              "content_html": "<p>Body &amp; more</p>",
              "date_published": "2026-11-12T10:00:00Z",
              "image": "https://example.org/img.jpg",
              "authors": [{ "name": "Jane" }],
              "tags": ["meta"]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesJsonFeed() {
        val feed = FeedParser.parse(fixture)
        assertEquals("Example JSON Feed", feed.title)
        assertEquals("https://example.org/", feed.siteUrl)
        assertEquals(1, feed.items.size)

        val item = feed.items[0]
        assertEquals("42", item.guid)
        assertEquals("Hello world", item.title)
        assertEquals("https://example.org/42", item.link)
        assertEquals("Jane", item.author)
        assertEquals(Instant.parse("2026-11-12T10:00:00Z").toEpochMilli(), item.publishedAt)
        assertEquals("Body & more", item.summaryText)
        assertEquals("https://example.org/img.jpg", item.imageUrl)
        assertEquals(listOf("meta"), item.categories)
    }
}
