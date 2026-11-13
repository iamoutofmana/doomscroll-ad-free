package io.github.iamoutofmana.doomscroll.domain

import io.github.iamoutofmana.doomscroll.domain.popularsources.PopularSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PopularSourcesTest {
    @Test
    fun youtubeChannelUrlFromId() {
        assertEquals(
            "https://www.youtube.com/feeds/videos.xml?channel_id=UCBcRF18j7Qf58Fujz2nuQhw",
            PopularSources.youtubeChannelUrl("UCBcRF18j7Qf58Fujz2nuQhw"),
        )
    }

    @Test
    fun youtubeChannelUrlExtractsFromChannelUrl() {
        assertEquals(
            "https://www.youtube.com/feeds/videos.xml?channel_id=UCBcRF18j7Qf58Fujz2nuQhw",
            PopularSources.youtubeChannelUrl("https://www.youtube.com/channel/UCBcRF18j7Qf58Fujz2nuQhw"),
        )
    }

    @Test
    fun redditSubredditUrl() {
        assertEquals("https://www.reddit.com/r/android/.rss", PopularSources.redditSubredditUrl("android"))
        assertEquals("https://www.reddit.com/r/android/.rss", PopularSources.redditSubredditUrl("r/android"))
        assertEquals("https://www.reddit.com/r/android/.rss", PopularSources.redditSubredditUrl("/r/android"))
    }

    @Test
    fun lemmyCommunityUrl() {
        assertEquals(
            "https://lemmy.world/feeds/c/android.xml?sort=New",
            PopularSources.lemmyCommunityUrl("lemmy.world", "android"),
        )
        assertEquals(
            "https://lemmy.world/feeds/c/android.xml?sort=New",
            PopularSources.lemmyCommunityUrl("https://lemmy.world", "android"),
        )
    }

    @Test
    fun urlDetectionHelpers() {
        assertTrue(PopularSources.isYouTubeFeedUrl("https://www.youtube.com/feeds/videos.xml?channel_id=X"))
        assertTrue(PopularSources.isRedditFeedUrl("https://www.reddit.com/r/android/.rss"))
        assertTrue(PopularSources.isLemmyFeedUrl("https://lemmy.world/feeds/c/android.xml"))
        assertTrue(PopularSources.isHackerNewsUrl("https://hn.algolia.com/api/v1/search?tags=front_page"))
        assertFalse(PopularSources.isYouTubeFeedUrl("https://youtube.com/watch?v=x"))
        assertFalse(PopularSources.isRedditFeedUrl("https://www.reddit.com/r/android"))
    }
}
