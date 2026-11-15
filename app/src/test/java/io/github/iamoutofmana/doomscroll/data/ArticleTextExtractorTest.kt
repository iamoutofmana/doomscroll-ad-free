package io.github.iamoutofmana.doomscroll.data

import io.github.iamoutofmana.doomscroll.data.network.ArticleTextExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleTextExtractorTest {
    @Test
    fun extractsArticleBlocks() {
        val paragraph1 =
            "This is the first paragraph of the article and it contains enough text to be " +
                "considered real content by the extractor."
        val paragraph2 =
            "This is the second paragraph, adding more context and detail so the extractor " +
                "sees a proper article body."
        val html = """
            <html><body>
              <nav><p>Home | News | Sport | Subscribe now for full access</p></nav>
              <article>
                <h1>The headline of the story</h1>
                <p>$paragraph1</p>
                <p>$paragraph2</p>
              </article>
            </body></html>
        """.trimIndent()

        val extracted = ArticleTextExtractor.extract(html, "https://example.com/a")

        assertNotNull(extracted)
        val result = requireNotNull(extracted)
        assertEquals("The headline of the story", result.title)
        assertTrue(result.paragraphs.contains(paragraph1))
        assertTrue(result.paragraphs.contains(paragraph2))
        assertFalse(result.paragraphs.any { it.contains("Subscribe") })
    }

    @Test
    fun fallsBackToMainElement() {
        val paragraph = "A single long paragraph in the main element that still qualifies as readable article text."
        val html = "<html><body><main><p>$paragraph</p></main></body></html>"

        val extracted = ArticleTextExtractor.extract(html, "https://example.com/a")

        assertNotNull(extracted)
        assertEquals(paragraph, requireNotNull(extracted).paragraphs.first())
    }

    @Test
    fun filtersJunkParagraphs() {
        val good = "The real article paragraph that has actual news content worth reading by any measure."
        val html = """
            <html><body><article>
              <p>We use cookies to improve your experience on this website.</p>
              <p>$good</p>
              <p>Download the app for the best experience on mobile devices today.</p>
            </article></body></html>
        """.trimIndent()

        val extracted = ArticleTextExtractor.extract(html, "https://example.com/a")
        val paragraphs = requireNotNull(extracted).paragraphs

        assertEquals(listOf(good), paragraphs)
    }

    @Test
    fun returnsNullWhenNothingReadable() {
        val html = "<html><body><div>hi</div></body></html>"
        assertNull(ArticleTextExtractor.extract(html, "https://example.com/a"))
    }

    @Test
    fun skipsRepeatedTitleRelatedLinksAndBylines() {
        val title = "Big Box Office Weekend For Everyone Involved In The Movie"
        val body = "Johnny Depp returned to the top of box office charts this weekend with a solid opening for the new film."
        val html = """
            <html><body><article>
              <h1>$title</h1>
              <p>Rebecca Rubin</p>
              <p>Senior Film and Media Reporter</p>
              <p>$title</p>
              <p>Another story from the related list that should not appear 2 hours ago</p>
              <p>$body</p>
            </article></body></html>
        """.trimIndent()

        val extracted = ArticleTextExtractor.extract(html, "https://example.com/a")
        val paragraphs = requireNotNull(extracted).paragraphs

        assertEquals(listOf(body), paragraphs)
    }
}
