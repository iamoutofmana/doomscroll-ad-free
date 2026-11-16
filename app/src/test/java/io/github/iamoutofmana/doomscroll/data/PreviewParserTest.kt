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
    fun prefersSubstantiveArticleBodyOverShortMetaBlurb() {
        // User feedback #1: og:description echoes the headline; article paragraphs add substance.
        val html = """
            <html><head><meta property="og:description" content="Short blurb." /></head>
            <body><article>
              <p>First substantive paragraph of the story that goes into real detail about what happened.</p>
              <p>Second paragraph continuing the story with more context and background information.</p>
            </article></body></html>
        """.trimIndent()

        val preview = PreviewParser.parse(html, "https://example.com/a")
        val description = requireNotNull(preview.description)

        assertTrue(description.contains("First substantive paragraph"))
        assertTrue(description.contains("Second paragraph continuing"))
    }

    @Test
    fun filtersCookieAndSignupJunkParagraphs() {
        val html = """
            <html><body><article>
              <p>We use cookies to improve your experience on this website and personalize content.</p>
              <p>The actual article paragraph that has real news content worth showing to the reader.</p>
            </article></body></html>
        """.trimIndent()

        val preview = PreviewParser.parse(html, "https://example.com/a")
        val description = requireNotNull(preview.description)

        assertFalse(description.contains("cookies"))
        assertTrue(description.contains("actual article paragraph"))
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
        val longDescription = "<p>Hello <b>world</b> " + "x".repeat(800) + "</p>"
        val html = """<html><head><meta property="og:description" content="$longDescription" /></head></html>"""

        val preview = PreviewParser.parse(html, "https://example.com/")
        val description = preview.description

        assertNotNull(description)
        val text = requireNotNull(description)
        assertFalse(text.contains("<"))
        assertTrue(text.length <= 601)
        assertTrue(text.endsWith("…"))
    }

    @Test
    fun fallsBackToFirstContentImage() {
        val html = """
            <html><head></head><body><article>
              <p>A paragraph of readable article text long enough to be used as a summary here.</p>
              <img data-src="/images/story.jpg" />
            </article></body></html>
        """.trimIndent()

        val preview = PreviewParser.parse(html, "https://example.com/news/a")

        assertEquals("https://example.com/images/story.jpg", preview.imageUrl)
    }

    @Test
    fun ignoresLogoImagesForHeroFallback() {
        val html = """
            <html><body><article>
              <img src="/static/logo.png" width="600" height="80" />
              <p>A paragraph of readable article text long enough to be used as a summary here.</p>
            </article></body></html>
        """.trimIndent()

        val preview = PreviewParser.parse(html, "https://example.com/news/a")

        assertNull(preview.imageUrl)
    }
}
