package io.github.iamoutofmana.doomscroll.domain.links

/**
 * Title normalization for cross-source dedupe (plan §7 layer 4).
 *
 * Same story across different sources (Google News "Top stories" vs "Technology", or a
 * generic RSS that republishes) should appear once in the feed, not twice. We keep the
 * first seen and drop later duplicates within a time window — see SyncEngine.
 *
 * Conservative: lowercases, strips punctuation/quotes/dashes, collapses whitespace. We
 * deliberately do NOT strip articles (a/the/and) — that would cause false positives on
 * short headlines.
 */
object TitleNormalizer {
    private val PUNCT = Regex("[^a-z0-9\\s]+")
    private val WHITESPACE = Regex("\\s+")

    fun normalize(title: String): String {
        val lowered = title.lowercase()
        val noPunct = PUNCT.replace(lowered, " ")
        return WHITESPACE.replace(noPunct, " ").trim()
    }
}
