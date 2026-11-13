package io.github.iamoutofmana.doomscroll.feedparser

import io.github.iamoutofmana.doomscroll.domain.popularsources.PopularSources

/**
 * Format-sniffing dispatcher:
 * - Hacker News Algolia JSON (detected by URL)
 * - JSON Feed 1.x (detected by leading '{')
 * - XML (RSS 2.0 / RDF / Atom — everything else)
 *
 * URL is required only so HN's non-standard JSON shape can be routed; for XML and
 * JSON Feed the body itself is enough.
 */
object FeedParser {
    fun parse(
        text: String,
        sourceUrl: String? = null,
    ): ParsedFeed {
        if (sourceUrl != null && PopularSources.isHackerNewsUrl(sourceUrl)) {
            return HackerNewsParser.parse(text)
        }
        val trimmed = text.trim()
        return if (trimmed.startsWith("{")) {
            JsonFeedParser.parse(trimmed)
        } else {
            XmlFeedParser.parse(trimmed)
        }
    }
}
