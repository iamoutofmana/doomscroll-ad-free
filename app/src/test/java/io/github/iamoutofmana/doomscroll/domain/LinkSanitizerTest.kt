package io.github.iamoutofmana.doomscroll.domain

import io.github.iamoutofmana.doomscroll.domain.links.LinkSanitizer
import org.junit.Assert.assertEquals
import org.junit.Test

class LinkSanitizerTest {
    @Test
    fun stripsUtmAndKnownTrackingParams() {
        assertEquals(
            "https://example.com/a?id=7",
            LinkSanitizer.sanitize("https://example.com/a?id=7&utm_source=rss&utm_medium=feed&fbclid=abc&gclid=x"),
        )
    }

    @Test
    fun keepsCleanUrlsUntouched() {
        assertEquals(
            "https://example.com/a?id=7",
            LinkSanitizer.sanitize("https://example.com/a?id=7"),
        )
    }

    @Test
    fun preservesFragmentWhenAllParamsStripped() {
        assertEquals(
            "https://example.com/a#sec",
            LinkSanitizer.sanitize("https://example.com/a?utm_source=x#sec"),
        )
    }

    @Test
    fun unwrapsLeadingAmpPath() {
        assertEquals("https://example.com/story.html", LinkSanitizer.sanitize("https://example.com/amp/story.html"))
        assertEquals("https://example.com/", LinkSanitizer.sanitize("https://example.com/amp"))
        assertEquals("https://example.com/story", LinkSanitizer.sanitize("https://example.com/story"))
    }

    @Test
    fun ampUnwrapCombinesWithParamStripping() {
        assertEquals(
            "https://example.com/s",
            LinkSanitizer.sanitize("https://example.com/amp/s?utm_campaign=k"),
        )
    }

    @Test
    fun stripsTrackingFromAnyScheme() {
        // Sanitizer is scheme-agnostic: tracking params are noise on any URL.
        assertEquals("ftp://example.com/x", LinkSanitizer.sanitize("ftp://example.com/x?utm_source=a"))
    }
}
