package io.github.iamoutofmana.doomscroll.domain

import io.github.iamoutofmana.doomscroll.domain.links.TitleNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TitleNormalizerTest {
    @Test
    fun lowercasesAndStripsPunctuation() {
        assertEquals("breaking news today", TitleNormalizer.normalize("Breaking: News! Today."))
    }

    @Test
    fun collapsesWhitespace() {
        assertEquals("a b c", TitleNormalizer.normalize("  a   b\n\tc  "))
    }

    @Test
    fun stripsCurlyQuotesAndEllipsis() {
        assertEquals(
            "anthropic s dario amodei not super fussed about market",
            TitleNormalizer.normalize("Anthropic's Dario Amodei 'not super fussed' about market…"),
        )
    }

    @Test
    fun identicalHeadlinesNormalizeToSameKey() {
        val a = "Why Apple's HomeView Relies So Heavily On Your iPhone"
        val b = "Why Apple's HomeView relies so heavily on your iPhone"
        assertEquals(TitleNormalizer.normalize(a), TitleNormalizer.normalize(b))
    }

    @Test
    fun similarButDifferentHeadlinesStayDistinct() {
        val a = "Gmail gets Material 3 redesign"
        val b = "Gmail gets Material YOU redesign"
        assertNotEquals(TitleNormalizer.normalize(a), TitleNormalizer.normalize(b))
    }

    @Test
    fun preservesNumbers() {
        assertEquals("spacex starship flight 15", TitleNormalizer.normalize("SpaceX starship flight 15"))
    }
}
