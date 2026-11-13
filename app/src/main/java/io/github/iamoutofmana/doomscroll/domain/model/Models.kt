package io.github.iamoutofmana.doomscroll.domain.model

/** How a source's feed URL is built and interpreted. */
enum class SourceType {
    /** Google News topic feed (World, Technology, ...). */
    GN_TOPIC,

    /** Google News keyword-search feed. */
    GN_SEARCH,

    /** Plain RSS/Atom/JSON-Feed URL added by the user. */
    RSS,

    /** YouTube channel (per-channel RSS via PopularSources.youtubeChannelUrl). */
    YOUTUBE,

    /** Reddit subreddit (subreddit RSS via PopularSources.redditSubredditUrl). */
    REDDIT,

    /** Lemmy community (community feed via PopularSources.lemmyCommunityUrl). */
    LEMMY,

    /** Hacker News front page (Algolia JSON — parsed by HackerNewsParser). */
    HACKER_NEWS,
}

/** Why an article was hidden. Always logged and reversible — never silently wrong (plan §7). */
enum class HiddenReason(val label: String) {
    SPONSORED_FILTER("Sponsored filter"),
    USER("Hidden by you"),
}
