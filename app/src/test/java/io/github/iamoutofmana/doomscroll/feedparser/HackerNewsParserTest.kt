package io.github.iamoutofmana.doomscroll.feedparser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HackerNewsParserTest {
    private val fixture = """
        {
          "hits": [
            {
              "objectID": "42000001",
              "title": "Show HN: A tiny Rust web framework",
              "url": "https://example.com/rust-framework",
              "author": "rustacean",
              "created_at": "2026-11-13T10:00:00Z"
            },
            {
              "objectID": "42000002",
              "title": "Ask HN: What are you working on?",
              "url": null,
              "author": "pg",
              "created_at": "2026-11-13T09:00:00Z",
              "story_text": "<p>Tell us</p>"
            },
            {
              "objectID": "42000003",
              "title": "The Verge: AI news roundup",
              "url": "https://www.theverge.com/ai-news",
              "author": "vergefan",
              "created_at": "2026-11-13T08:30:00Z"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesHackerNewsAndSkipsSelfPosts() {
        val feed = HackerNewsParser.parse(fixture)
        assertEquals("Hacker News", feed.title)
        assertEquals("https://news.ycombinator.com/", feed.siteUrl)
        // Ask HN self-post (url=null) is filtered out
        assertEquals(2, feed.items.size)

        val first = feed.items[0]
        assertEquals("hn-42000001", first.guid)
        assertEquals("Show HN: A tiny Rust web framework", first.title)
        assertEquals("https://example.com/rust-framework", first.link)
        assertEquals("rustacean", first.author)
        assertEquals(Instant.parse("2026-11-13T10:00:00Z").toEpochMilli(), first.publishedAt)
    }

    @Test
    fun dispatchedByUrl() {
        val feed = FeedParser.parse(
            fixture,
            sourceUrl = "https://hn.algolia.com/api/v1/search?tags=front_page",
        )
        assertEquals("Hacker News", feed.title)
        assertTrue(feed.items.isNotEmpty())
    }
}
