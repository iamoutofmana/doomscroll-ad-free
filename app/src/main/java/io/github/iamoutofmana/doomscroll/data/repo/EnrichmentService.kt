package io.github.iamoutofmana.doomscroll.data.repo

import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.data.network.ArticleEnricher

/**
 * Lazily fills previews (summary + hero image) for articles whose feed carried neither —
 * notably every Google News item. Runs only when a card is expanded.
 *
 * Summaries that are merely headline echoes (short og:descriptions) get upgraded to the
 * publisher's opening paragraphs on a later expand (user feedback #1). Failures and
 * no-op fetches back off via an in-memory cooldown.
 */
class EnrichmentService(
    private val articles: ArticleRepository,
    private val enricher: ArticleEnricher,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val lastAttempt = mutableMapOf<Long, Long>()

    suspend fun ensurePreview(article: ArticleEntity) {
        val currentSummaryLength = article.summary?.length ?: 0
        val hasGoodSummary = currentSummaryLength >= GOOD_SUMMARY_LENGTH
        if (hasGoodSummary && !article.imageUrl.isNullOrBlank()) return

        val attemptedAt = lastAttempt[article.id]
        if (attemptedAt != null && clock() - attemptedAt < ATTEMPT_COOLDOWN_MS) return
        lastAttempt[article.id] = clock()

        val enriched = enricher.fetch(article.link, article.resolvedLink) ?: return

        // Only replace an existing summary when the new one is meaningfully longer —
        // otherwise keep what the feed (or earlier enrichment) gave us.
        val candidateSummary = enriched.description?.takeIf { it.isNotBlank() }
        val summary = when {
            candidateSummary == null -> null
            currentSummaryLength == 0 -> candidateSummary
            candidateSummary.length > currentSummaryLength + SUMMARY_UPGRADE_MARGIN -> candidateSummary
            else -> null
        }
        val imageUrl = article.imageUrl?.takeIf { it.isNotBlank() } ?: enriched.imageUrl
        val resolvedUrl = article.resolvedLink?.takeIf { it.isNotBlank() } ?: enriched.resolvedUrl

        if (summary == null && imageUrl == article.imageUrl && resolvedUrl == article.resolvedLink) return
        articles.updatePreview(article.id, summary, imageUrl, resolvedUrl)
    }

    private companion object {
        const val GOOD_SUMMARY_LENGTH = 200
        const val SUMMARY_UPGRADE_MARGIN = 20
        const val ATTEMPT_COOLDOWN_MS = 10L * 60L * 1000L
    }
}

/**
 * Prefetches previews for the top of the feed after a sync so the visible cards gain
 * hero images and summaries without a tap (user feedback: "I want to see pictures").
 * Sequential and capped — a background nicety, never a sync blocker.
 */
suspend fun EnrichmentService.prefetch(
    groups: List<io.github.iamoutofmana.doomscroll.data.repo.StoryClusterer.StoryGroup>,
    limit: Int = 10,
) {
    groups
        .asSequence()
        .flatMap { sequenceOf(it.primary) + it.related.asSequence() }
        .filter { it.summary.isNullOrBlank() || it.imageUrl.isNullOrBlank() }
        .take(limit)
        .forEach { ensurePreview(it) }
}
