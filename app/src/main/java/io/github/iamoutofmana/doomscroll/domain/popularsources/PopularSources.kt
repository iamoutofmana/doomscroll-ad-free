package io.github.iamoutofmana.doomscroll.domain.popularsources

/**
 * URL builders for non-Google-News sources (plan §5 Appendix A).
 *
 * Each feed type has a canonical URL shape; these helpers keep that knowledge in one place
 * so SyncEngine and the UI share exactly the same URLs.
 */
object PopularSources {
    private val REDDIT_PREFIX = Regex("^/?r/")

    /** YouTube per-channel RSS: requires the channel ID (UCxxxxxxxxxxxxxxxxxxxxxx). */
    fun youtubeChannelUrl(channelId: String): String {
        val trimmed = channelId.trim()
        // Accept the full URL too — extract /channel/UC... when present.
        val id = Regex("""/channel/(UC[A-Za-z0-9_-]+)""").find(trimmed)?.groupValues?.get(1) ?: trimmed
        return "https://www.youtube.com/feeds/videos.xml?channel_id=$id"
    }

    fun redditSubredditUrl(subreddit: String): String {
        val name = REDDIT_PREFIX.replaceFirst(subreddit.trim(), "").trim().ifBlank { return "" }
        return "https://www.reddit.com/r/$name/.rss"
    }

    fun lemmyCommunityUrl(
        instance: String,
        community: String,
    ): String {
        val host = instance
            .trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
            .ifBlank { return "" }
        val name = community.trim().ifBlank { return "" }
        return "https://$host/feeds/c/$name.xml?sort=New"
    }

    const val HACKER_NEWS_URL = "https://hn.algolia.com/api/v1/search?tags=front_page"

    /** True iff the URL is a YouTube channel feed we can recognize. */
    fun isYouTubeFeedUrl(url: String): Boolean =
        url.contains("youtube.com/feeds/videos.xml")

    fun isRedditFeedUrl(url: String): Boolean =
        url.contains("reddit.com/r/") && url.endsWith(".rss")

    fun isLemmyFeedUrl(url: String): Boolean =
        url.contains("/feeds/c/")

    fun isHackerNewsUrl(url: String): Boolean =
        url.contains("hn.algolia.com")
}
