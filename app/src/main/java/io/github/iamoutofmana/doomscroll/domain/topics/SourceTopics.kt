package io.github.iamoutofmana.doomscroll.domain.topics

import io.github.iamoutofmana.doomscroll.domain.packs.TopicPacks

/**
 * Maps sources to feed topics for the "view by topic" chips (v1.3).
 *
 * A source belongs to a topic when its URL is one of the curated pack sources; everything
 * else (custom feeds, Hacker News, extra Google News topics) falls into "Other". This keeps
 * the mapping stateless — no DB migration, and adding packs automatically extends the chips.
 */
object SourceTopics {
    const val OTHER_ID = "other"

    data class Chip(
        val id: String,
        val label: String,
    )

    fun packIdForUrl(url: String): String? =
        TopicPacks.ALL.firstOrNull { pack -> pack.sources.any { it.url == url } }?.id

    fun topicOf(url: String): String = packIdForUrl(url) ?: OTHER_ID

    /** Chips for the topics that actually have at least one source, plus Other if needed. */
    fun chipsFor(sourceUrls: List<String>): List<Chip> {
        val present = sourceUrls.mapNotNull(::packIdForUrl).toSet()
        val packChips = TopicPacks.ALL
            .filter { it.id in present }
            .map { Chip(it.id, it.chipLabel) }
        val hasOther = sourceUrls.any { packIdForUrl(it) == null }
        return if (hasOther) packChips + Chip(OTHER_ID, "Other") else packChips
    }
}
