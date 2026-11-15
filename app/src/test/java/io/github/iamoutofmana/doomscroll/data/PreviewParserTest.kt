package io.github.iamoutofmana.doomscroll.data

import io.github.iamoutofmana.doomscroll.data.network.PreviewParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewParserTest {
    @Test
    fun parsesOpenGraphMetadata() {
        val html = """
            <html><head>
              <meta property="og:description" content="A short summary of the article." />
              <meta property="og:image" content="https://cdn.example.com/hero.jpg" />
            </head><body><p>text</p></body></html>
        """.trimIndent()

        val preview = PreviewParser.parse(html, "https://example.com/a")

        assertEquals("A short summary of the article.", preview.description)
        assertEquals("https://cdn.example.com/hero.jpg", preview.imageUrl)
    }

    @Test
    fun resolvesRelativeImageUrl() {
        val html = """<html><head><meta property="og:image" content="/img/hero.jpg" /></head></html>"""

        val preview = PreviewParser.parse(html, "https://example.com/news/a")

        assertEquals("https://example.com/img/hero.jpg", preview.imageUrl)
    }

    @Test
    fun fallsBackToMetaDescription() {
        val html = """<html><head><meta name="description" content="Plain meta description." /></head></html>"""

        val preview = PreviewParser.parse(html, "https://example.com/")

        assertEquals("Plain meta description.", preview.description)
    }

    @Test
    fun fallsBackToFirstLongParagraph() {
        val longText = "This is a reasonably long paragraph used as a fallback summary when no metadata exists."
        val html = "<html><body><p>$longText</p></body></html>"

        val preview = PreviewParser.parse(html, "https://example.com/")

        assertEquals(longText, preview.description)
    }

    @Test
    fun emptyDocumentYieldsNulls() {
        val preview = PreviewParser.parse("<html></html>", "https://example.com/")

        assertNull(preview.description)
        assertNull(preview.imageUrl)
    }

    @Test
    fun descriptionIsSanitizedAndCapped() {
        val longDescription = "<p>Hello <b>world</b> " + "x".repeat(600) + "</p>"
        val html = """<html><head><meta property="og:description" content="$longDescription" /></head></html>"""

        val preview = PreviewParser.parse(html, "https://example.com/")
        val description = preview.description

        assertNotNull(description)
        val text = requireNotNull(description)
        assertFalse(text.contains("<"))
        assertTrue(text.length <= 401)
        assertTrue(text.endsWith("…"))
    }
}
