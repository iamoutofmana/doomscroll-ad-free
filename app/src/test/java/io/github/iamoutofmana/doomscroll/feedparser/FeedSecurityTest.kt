package io.github.iamoutofmana.doomscroll.feedparser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Security: hostile feed payloads. These tests pin the parser's behavior, not exact
 * error messages — SyncEngine treats any parse failure as a per-source error.
 */
class FeedSecurityTest {
    @Test
    fun externalEntitiesAreNeverResolved() {
        val xml = """
            <?xml version="1.0"?>
            <!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <rss version="2.0"><channel><title>T</title>
              <item><title>&xxe;</title><link>https://example.com/a</link></item>
            </channel></rss>
        """.trimIndent()

        val feed = runCatching { XmlFeedParser.parse(xml) }.getOrNull()
        val title = feed?.items?.firstOrNull()?.title.orEmpty()
        assertFalse("file contents must never appear", title.contains("root:"))
    }

    @Test
    fun entityExpansionStaysBounded() {
        // Nested internal entities (classic "billion laughs" shape, kept small enough
        // to be a safe test): ~10^4 characters if expanded, or rejected outright.
        val xml = buildString {
            append("""<?xml version="1.0"?>""")
            append("""<!DOCTYPE rss [<!ENTITY a "AAAAAAAAAA">""")
            for (i in 1..4) {
                val previous = if (i == 1) "a" else "e${i - 1}"
                append("<!ENTITY e$i ")
                append('"')
                repeat(10) { append("&$previous;") }
                append("\">")
            }
            append("]>")
            append("""<rss version="2.0"><channel><title>T</title>""")
            append("""<item><title>&e4;</title><link>https://example.com/a</link></item>""")
            append("</channel></rss>")
        }

        val started = System.currentTimeMillis()
        val feed = runCatching { XmlFeedParser.parse(xml) }.getOrNull()
        val elapsed = System.currentTimeMillis() - started

        assertTrue("expansion must not stall the parser (took ${elapsed}ms)", elapsed < 5_000)
        assertTrue(
            "expanded content must stay bounded",
            (feed?.items?.firstOrNull()?.title?.length ?: 0) <= 20_000,
        )
    }

    @Test
    fun truncatedXmlFailsCleanlyWithoutHanging() {
        val started = System.currentTimeMillis()
        val result = runCatching { XmlFeedParser.parse("<rss><channel><item>oops") }
        val elapsed = System.currentTimeMillis() - started

        assertTrue("must not hang (took ${elapsed}ms)", elapsed < 5_000)
        // If anything was produced from the fragment, it must be undisplayable
        // (no link), so the sync layer drops it rather than storing garbage.
        result.getOrNull()?.items?.forEach { item ->
            assertTrue("truncated items must carry no link", item.link.isNullOrBlank())
        }
    }

    @Test
    fun binaryGarbageDoesNotCrash() {
        val junk = String(ByteArray(500) { ((it * 37) % 256).toByte() }, Charsets.ISO_8859_1)
        val result = runCatching { XmlFeedParser.parse(junk) }
        assertTrue(
            "garbage input is rejected, not fatal",
            result.isFailure || result.getOrNull()?.items?.isEmpty() != false,
        )
    }
}
