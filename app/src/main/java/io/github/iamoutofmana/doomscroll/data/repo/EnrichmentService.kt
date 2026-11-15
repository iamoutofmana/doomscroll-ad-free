package io.github.iamoutofmana.doomscroll.data.repo

import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.data.network.ArticleEnricher

/**
 * Lazily fills previews (summary + hero image) for articles whose feed carried neither —
 * notably every Google News item. Runs only when a card is expanded; failures back off
 * for a while so repeated taps don't hammer a dead publisher.
 */
class EnrichmentService(
    private val articles: ArticleRepository,
    private val enricher: ArticleEnricher,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val recentFailures = mutableMapOf<Long, Long>()

    suspend fun ensurePreview(article: ArticleEntity) {
        if (!article.summary.isNullOrBlank() && !article.imageUrl.isNullOrBlank()) return
        val failedAt = recentFailures[article.id]
        if (failedAt != null && clock() - failedAt < FAILURE_COOLDOWN_MS) return

        val enriched = enricher.fetch(article.link, article.resolvedLink)
        if (enriched == null) {
            recentFailures[article.id] = clock()
            return
        }
        val summary = article.summary?.takeIf { it.isNotBlank() } ?: enriched.description
        val imageUrl = article.imageUrl?.takeIf { it.isNotBlank() } ?: enriched.imageUrl
        val resolvedUrl = article.resolvedLink?.takeIf { it.isNotBlank() } ?: enriched.resolvedUrl
        if (summary == article.summary && imageUrl == article.imageUrl && resolvedUrl == article.resolvedLink) return
        articles.updatePreview(article.id, summary, imageUrl, resolvedUrl)
        recentFailures.remove(article.id)
    }

    private companion object {
        const val FAILURE_COOLDOWN_MS = 10L * 60L * 1000L
    }
}
