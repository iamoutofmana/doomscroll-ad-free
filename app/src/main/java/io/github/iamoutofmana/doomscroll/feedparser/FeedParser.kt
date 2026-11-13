package io.github.iamoutofmana.doomscroll.feedparser

/**
 * Format-sniffing dispatcher: JSON Feed vs XML (RSS 2.0 / RDF / Atom).
 * Malformed input throws — SyncEngine catches per source and records lastError.
 */
object FeedParser {
    fun parse(text: String): ParsedFeed {
        val trimmed = text.trim()
        return if (trimmed.startsWith("{")) {
            JsonFeedParser.parse(trimmed)
        } else {
            XmlFeedParser.parse(trimmed)
        }
    }
}
