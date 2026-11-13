package io.github.iamoutofmana.doomscroll.feedparser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class XmlFeedParserTest {
    private val rssFixture = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
          <channel>
            <title>Google News - Technology</title>
            <link>https://news.google.com/?hl=en-US&amp;gl=US&amp;ceid=US:en</link>
            <description>Google News</description>
            <item>
              <title>New AI chip announced - The Verge</title>
              <link>https://news.google.com/rss/articles/CBMiTEST?oc=5</link>
              <guid isPermaLink="false">tag:news.google.com,2005:cluster/CBMiTEST</guid>
              <pubDate>Fri, 13 Nov 2026 09:30:00 GMT</pubDate>
              <description><![CDATA[<a href="https://news.google.com/x">Full coverage</a>]]></description>
              <source url="https://www.theverge.com">The Verge</source>
            </item>
            <item>
              <title>Q&amp;A: Robots in factories</title>
              <link>https://example.com/robots</link>
              <pubDate>13 Nov 2026 08:00:00 +0100</pubDate>
              <description>&lt;p&gt;Robots are neat&lt;/p&gt;</description>
              <media:content url="https://cdn.example.com/robot.jpg" medium="image" type="image/jpeg"/>
              <category>tech</category>
              <category>sponsored</category>
              <enclosure url="https://cdn.example.com/pod.mp3" type="audio/mpeg" length="12345"/>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    private val atomFixture = """
        <?xml version="1.0" encoding="utf-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">
          <title>Example Atom Feed</title>
          <link rel="self" href="https://example.com/feed.atom"/>
          <link rel="alternate" type="text/html" href="https://example.com/"/>
          <updated>2026-11-12T18:30:02Z</updated>
          <entry>
            <title>First entry</title>
            <id>urn:uuid:aaa-bbb</id>
            <link rel="alternate" type="text/html" href="https://example.com/first"/>
            <published>2026-11-12T10:00:00Z</published>
            <author><name>Jane Doe</name></author>
            <summary>Short summary here</summary>
            <content type="html">&lt;p&gt;Full &lt;b&gt;content&lt;/b&gt;&lt;/p&gt;</content>
            <media:thumbnail url="https://example.com/thumb.jpg"/>
            <category term="android"/>
          </entry>
        </feed>
    """.trimIndent()

    @Test
    fun parsesGoogleNewsStyleRss() {
        val feed = XmlFeedParser.parse(rssFixture)
        assertEquals("Google News - Technology", feed.title)
        assertEquals(2, feed.items.size)

        val first = feed.items[0]
        assertEquals("New AI chip announced - The Verge", first.title)
        assertEquals("https://news.google.com/rss/articles/CBMiTEST?oc=5", first.link)
        assertEquals("tag:news.google.com,2005:cluster/CBMiTEST", first.guid)
        assertEquals("The Verge", first.publisher)
        assertEquals(Instant.parse("2026-11-13T09:30:00Z").toEpochMilli(), first.publishedAt)

        val second = feed.items[1]
        assertEquals("Q&A: Robots in factories", second.title)
        assertEquals(Instant.parse("2026-11-13T07:00:00Z").toEpochMilli(), second.publishedAt)
        assertEquals("https://cdn.example.com/robot.jpg", second.imageUrl)
        assertEquals("https://cdn.example.com/pod.mp3", second.audioUrl)
        assertEquals(listOf("tech", "sponsored"), second.categories)
        assertEquals("https://example.com/robots", second.guid)
        assertEquals("Robots are neat", second.summaryText)
        assertNull(second.publisher)
    }

    @Test
    fun parsesAtomEntries() {
        val feed = XmlFeedParser.parse(atomFixture)
        assertEquals("Example Atom Feed", feed.title)
        assertEquals("https://example.com/", feed.siteUrl)
        assertEquals(1, feed.items.size)

        val entry = feed.items[0]
        assertEquals("First entry", entry.title)
        assertEquals("urn:uuid:aaa-bbb", entry.guid)
        assertEquals("https://example.com/first", entry.link)
        assertEquals("Jane Doe", entry.author)
        assertEquals(Instant.parse("2026-11-12T10:00:00Z").toEpochMilli(), entry.publishedAt)
        assertEquals("Short summary here", entry.summaryText)
        assertEquals("https://example.com/thumb.jpg", entry.imageUrl)
        assertEquals(listOf("android"), entry.categories)
    }

    @Test
    fun unknownRootDegradesToEmptyFeed() {
        val feed = XmlFeedParser.parse("<nothing><child>text</child></nothing>")
        assertTrue(feed.items.isEmpty())
    }
}
