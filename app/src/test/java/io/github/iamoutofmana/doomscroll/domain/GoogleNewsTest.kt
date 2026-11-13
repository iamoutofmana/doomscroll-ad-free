package io.github.iamoutofmana.doomscroll.domain

import io.github.iamoutofmana.doomscroll.domain.googlenews.GoogleNews
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleNewsTest {
    @Test
    fun topStoriesUrl() {
        assertEquals(
            "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en",
            GoogleNews.topicUrl(GoogleNews.TOPICS.first()),
        )
    }

    @Test
    fun topicUrlWithLocale() {
        val uk = GoogleNews.LOCALES.first { it.gl == "GB" }
        val tech = GoogleNews.TOPICS.first { it.code == "TECHNOLOGY" }
        assertEquals(
            "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-GB&gl=GB&ceid=GB:en",
            GoogleNews.topicUrl(tech, uk),
        )
    }

    @Test
    fun searchUrlEncodesQuery() {
        assertEquals(
            "https://news.google.com/rss/search?q=formula+1&hl=en-US&gl=US&ceid=US:en",
            GoogleNews.searchUrl("formula 1"),
        )
    }

    @Test
    fun searchUrlEncodesOperators() {
        val url = GoogleNews.searchUrl("intitle:pixel OR when:7d")
        assertTrue(url.contains("q=intitle%3Apixel+OR+when%3A7d"))
    }

    @Test
    fun geoUrlEncodesCity() {
        assertEquals(
            "https://news.google.com/rss/headlines/section/geo/New+York?hl=en-US&gl=US&ceid=US:en",
            GoogleNews.geoUrl("New York"),
        )
    }

    @Test
    fun splitTitleAtLastSeparator() {
        assertEquals("Headline - Early" to "Publisher", GoogleNews.splitTitle("Headline - Early - Publisher"))
        assertEquals("Plain title" to null, GoogleNews.splitTitle("Plain title"))
    }

    @Test
    fun splitTitleRejectsEmptySides() {
        assertEquals("A - " to null, GoogleNews.splitTitle("A - "))
        assertEquals(" - B" to null, GoogleNews.splitTitle(" - B"))
    }

    @Test
    fun detectsGoogleNewsUrls() {
        assertTrue(GoogleNews.isGoogleNewsUrl("https://news.google.com/rss/articles/ABC"))
        assertFalse(GoogleNews.isGoogleNewsUrl("https://example.com/feed"))
    }
}
