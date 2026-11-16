package io.github.iamoutofmana.doomscroll.data

import io.github.iamoutofmana.doomscroll.data.network.ArticleTextExtractor
import io.github.iamoutofmana.doomscroll.data.network.ReaderBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleTextExtractorTest {
    @Test
    fun extractsStructuredParagraphs() {
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

        val extracted = requireNotNull(ArticleTextExtractor.extract(html, "https://example.com/a"))

        assertEquals("The headline of the story", extracted.title)
        val paragraphs = extracted.blocks.filter { it.type == ReaderBlock.PARAGRAPH }.mapNotNull { it.text }
        assertTrue(paragraphs.contains(paragraph1))
        assertTrue(paragraphs.contains(paragraph2))
        assertFalse(paragraphs.any { it.contains("Subscribe") })
    }

    @Test
    fun includesHeadingsAndBullets() {
        val html = """
            <html><body><article>
              <h1>Article title that is long enough</h1>
              <p>A lead paragraph with enough characters to qualify as real readable content here.</p>
              <h2>Section heading</h2>
              <ul><li>A bullet item that is long enough to be kept in the reader output.</li></ul>
              <p>Another paragraph that continues the story with plenty of detail and context for readers.</p>
            </article></body></html>
        """.trimIndent()

        val extracted = requireNotNull(ArticleTextExtractor.extract(html, "https://example.com/a"))

        assertTrue(extracted.blocks.any { it.type == ReaderBlock.HEADING && it.text == "Section heading" })
        assertTrue(
            extracted.blocks.any { it.type == ReaderBlock.BULLET && it.text.orEmpty().startsWith("A bullet item") },
        )
    }

    @Test
    fun addsHeroImageFromOpenGraph() {
        val html = """
            <html><head><meta property="og:image" content="/images/hero.jpg" /></head>
            <body><article>
              <h1>Headline that is long enough for comparison</h1>
              <p>A paragraph of readable article text long enough to satisfy the extractor limits.</p>
            </article></body></html>
        """.trimIndent()

        val extracted = requireNotNull(ArticleTextExtractor.extract(html, "https://example.com/news/story"))

        val first = extracted.blocks.first()
        assertEquals(ReaderBlock.IMAGE, first.type)
        assertEquals("https://example.com/images/hero.jpg", first.imageUrl)
    }

    @Test
    fun filtersJunkAndSkipsRepeatedTitle() {
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

        val extracted = requireNotNull(ArticleTextExtractor.extract(html, "https://example.com/a"))
        val paragraphs = extracted.blocks.filter { it.type == ReaderBlock.PARAGRAPH }.mapNotNull { it.text }

        assertEquals(listOf(body), paragraphs)
    }

    @Test
    fun fallsBackToMainElement() {
        val paragraph = "A single long paragraph in the main element that still qualifies as readable article text."
        val html = "<html><body><main><p>$paragraph</p></main></body></html>"

        val extracted = requireNotNull(ArticleTextExtractor.extract(html, "https://example.com/a"))

        assertEquals(paragraph, extracted.blocks.first { it.type == ReaderBlock.PARAGRAPH }.text)
    }

    @Test
    fun returnsNullWhenNothingReadable() {
        val html = "<html><body><div>hi</div></body></html>"
        assertNull(ArticleTextExtractor.extract(html, "https://example.com/a"))
    }

    @Test
    fun extractedBlocksAreNotEmpty() {
        val html =
            "<html><body><article><p>A readable paragraph with enough content to be kept " +
                "here for sure, and long enough to satisfy the minimum extracted text budget.</p></article></body></html>"
        val extracted = ArticleTextExtractor.extract(html, "https://example.com/a")
        assertNotNull(extracted)
        assertTrue(extracted!!.blocks.isNotEmpty())
    }
}
