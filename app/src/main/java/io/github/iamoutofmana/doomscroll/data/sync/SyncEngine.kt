package io.github.iamoutofmana.doomscroll.data.sync

import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.data.db.SourceEntity
import io.github.iamoutofmana.doomscroll.data.network.FeedFetcher
import io.github.iamoutofmana.doomscroll.data.network.GnRedirectResolver
import io.github.iamoutofmana.doomscroll.data.repo.ArticleRepository
import io.github.iamoutofmana.doomscroll.data.repo.SourceRepository
import io.github.iamoutofmana.doomscroll.domain.filter.SponsoredFilter
import io.github.iamoutofmana.doomscroll.domain.googlenews.GoogleNews
import io.github.iamoutofmana.doomscroll.domain.links.TitleNormalizer
import io.github.iamoutofmana.doomscroll.domain.model.HiddenReason
import io.github.iamoutofmana.doomscroll.domain.model.SourceType
import io.github.iamoutofmana.doomscroll.feedparser.FeedParser
import io.github.iamoutofmana.doomscroll.feedparser.ParsedFeed
import io.github.iamoutofmana.doomscroll.feedparser.ParsedItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The whole pipeline in one place (plan §5):
 * fetch (conditional GET) -> parse -> normalize -> sponsored filter -> dedupe -> store
 * -> resolve GN links.
 *
 * Sequential over sources on purpose: polite to servers, simple to reason about, and fast
 * enough for a personal number of feeds. A Mutex keeps UI-triggered refreshes and the
 * periodic WorkManager sync from overlapping.
 */
class SyncEngine(
    private val sources: SourceRepository,
    private val articles: ArticleRepository,
    private val fetcher: FeedFetcher,
    private val resolver: GnRedirectResolver,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    data class Stats(
        val sourcesSynced: Int,
        val newArticles: Int,
        val failures: Int,
    )

    private val mutex = Mutex()

    suspend fun syncAll(): Stats = mutex.withLock {
        val enabled = sources.enabled()
        var newTotal = 0
        var failures = 0

        // Cross-source dedupe window: 7 days. Same headline across multiple GN topics
        // (Top stories, Technology, ...) or across GN + a direct RSS republish shows once.
        val cutoffMs = clock() - DEDUPE_WINDOW_MS
        val seenTitles: MutableSet<String> =
            articles.recentTitles(cutoffMs).mapTo(mutableSetOf()) { TitleNormalizer.normalize(it) }

        for (src in enabled) {
            val result = runCatching { syncSource(src, seenTitles) }
            result
                .onSuccess { newTotal += it }
                .onFailure {
                    failures++
                    sources.markError(src.id, it.message ?: "sync failed", clock())
                }
        }
        articles.pruneOlderThanDays(PRUNE_DAYS)
        Stats(enabled.size, newTotal, failures)
    }

    suspend fun syncSource(
        src: SourceEntity,
        seenTitles: MutableSet<String>? = null,
    ): Int = when (val res = fetcher.fetch(src.url, src.etag, src.lastModified)) {
        FeedFetcher.Result.NotModified -> {
            sources.markSynced(src.id, clock(), src.etag, src.lastModified)
            0
        }
        is FeedFetcher.Result.Failure -> {
            sources.markError(src.id, res.message, clock())
            0
        }
        is FeedFetcher.Result.Content -> ingest(src, res, seenTitles)
    }

    private suspend fun ingest(
        src: SourceEntity,
        res: FeedFetcher.Result.Content,
        seenTitles: MutableSet<String>?,
    ): Int {
        val feed = FeedParser.parse(res.body, sourceUrl = src.url)
        val now = clock()
        val isGn = src.sourceType == SourceType.GN_TOPIC || src.sourceType == SourceType.GN_SEARCH

        val entities = mutableListOf<ArticleEntity>()
        for (item in feed.items) {
            val entity = mapItem(item, src, feed, isGn, now) ?: continue
            val normalizedTitle = TitleNormalizer.normalize(entity.title)
            // Cross-source dedupe: skip if the same headline was already seen (either in a
            // previously-synced source or earlier in this batch).
            if (seenTitles != null) {
                if (normalizedTitle in seenTitles) continue
                seenTitles.add(normalizedTitle)
            }
            entities.add(entity)
        }

        val rowIds = articles.insertAll(entities)
        val newCount = rowIds.count { it != -1L }

        // RSS sources added by URL adopt the feed's own title after the first successful fetch.
        if (src.sourceType == SourceType.RSS && !feed.title.isNullOrBlank()) {
            sources.rename(src.id, feed.title)
        }
        sources.markSynced(src.id, now, res.etag, res.lastModified)

        if (isGn) resolveNewRedirects(entities, rowIds)
        return newCount
    }

    private fun mapItem(
        item: ParsedItem,
        src: SourceEntity,
        feed: ParsedFeed,
        isGn: Boolean,
        now: Long,
    ): ArticleEntity? {
        val link = item.link?.takeIf { it.isNotBlank() } ?: return null
        val headline = if (isGn) GoogleNews.splitTitle(item.title).first else item.title
        val titlePublisher = if (isGn) GoogleNews.splitTitle(item.title).second else null
        val sponsored = SponsoredFilter.isSponsored(item.title, item.categories)
        val publisher = item.publisher?.takeIf { it.isNotBlank() }
            ?: titlePublisher
            ?: feed.title?.takeIf { !isGn && it.isNotBlank() }
        val summary = if (isGn) {
            // Google News descriptions are link soup — never useful as a card summary.
            null
        } else {
            item.summaryText?.takeIf { it.isNotBlank() }?.let {
                if (it.length > SUMMARY_MAX) it.take(SUMMARY_MAX) + "…" else it
            }
        }
        return ArticleEntity(
            sourceId = src.id,
            guid = item.guid.ifBlank { link },
            title = headline.ifBlank { src.title },
            link = link,
            publisher = publisher,
            author = item.author?.takeIf { it.isNotBlank() },
            publishedAt = item.publishedAt,
            fetchedAt = now,
            summary = summary,
            imageUrl = item.imageUrl?.takeIf { it.isNotBlank() },
            audioUrl = item.audioUrl?.takeIf { it.isNotBlank() },
            videoUrl = item.videoUrl?.takeIf { it.isNotBlank() },
            hiddenAt = if (sponsored) now else null,
            hiddenReason = if (sponsored) HiddenReason.SPONSORED_FILTER.name else null,
        )
    }

    /** Best-effort publisher-URL resolution for freshly inserted Google News items. */
    private suspend fun resolveNewRedirects(
        entities: List<ArticleEntity>,
        rowIds: List<Long>,
    ) {
        val targets = entities
            .zip(rowIds)
            .filter { (entity, id) -> id != -1L && entity.link.contains("news.google.com") }
            .take(RESOLVE_CAP)
        var consecutiveFailures = 0
        for ((entity, id) in targets) {
            val resolved = runCatching { resolver.resolve(entity.link) }.getOrNull()
            if (resolved != null) {
                articles.setResolvedLink(id, resolved)
                consecutiveFailures = 0
            } else {
                consecutiveFailures++
                if (consecutiveFailures >= RESOLVE_FAILURE_LIMIT) break
            }
        }
    }

    private companion object {
        const val SUMMARY_MAX = 300
        const val PRUNE_DAYS = 30
        const val RESOLVE_CAP = 15
        const val RESOLVE_FAILURE_LIMIT = 3
        const val DEDUPE_WINDOW_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
