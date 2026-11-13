package io.github.iamoutofmana.doomscroll.domain

import io.github.iamoutofmana.doomscroll.domain.links.HtmlText
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlTextTest {
    @Test
    fun stripsTagsAndUnescapes() {
        assertEquals("Hello world & more", HtmlText.strip("<p>Hello <b>world</b> &amp; more</p>"))
    }

    @Test
    fun decodesNumericEntities() {
        assertEquals("AB", HtmlText.unescapeEntities("&#65;&#x42;"))
    }

    @Test
    fun collapsesWhitespace() {
        assertEquals("a b", HtmlText.strip("a\n\n   b"))
    }

    @Test
    fun doubleEscapedAmpStaysLiteral() {
        assertEquals("&#38;", HtmlText.unescapeEntities("&amp;#38;"))
    }

    @Test
    fun extractsTextFromLinkSoup() {
        assertEquals(
            "Full coverage",
            HtmlText.strip("""<a href="https://news.google.com/x">Full coverage</a>"""),
        )
    }
}
