package io.github.iamoutofmana.doomscroll.data.repo

import io.github.iamoutofmana.doomscroll.data.db.ArticleDao
import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.domain.model.HiddenReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArticleRepository(private val dao: ArticleDao) {
    /** Feed is grouped so near-duplicate stories show once (user feedback #4). */
    val feed: Flow<List<StoryClusterer.StoryGroup>> = dao.observeFeed().map { StoryClusterer.cluster(it) }
    val saved: Flow<List<ArticleEntity>> = dao.observeSaved()
    val hidden: Flow<List<ArticleEntity>> = dao.observeHidden()

    suspend fun insertAll(articles: List<ArticleEntity>): List<Long> = dao.insertAll(articles)

    suspend fun markRead(id: Long) = dao.markRead(id, System.currentTimeMillis())

    suspend fun markAllRead() = dao.markAllRead(System.currentTimeMillis())

    suspend fun setSaved(
        id: Long,
        saved: Boolean,
    ) = dao.setSaved(id, if (saved) System.currentTimeMillis() else null)

    suspend fun hide(id: Long) = dao.setHidden(id, System.currentTimeMillis(), HiddenReason.USER.name)

    suspend fun unhide(id: Long) = dao.setHidden(id, null, null)

    suspend fun unhideAll() = dao.unhideAll()

    suspend fun setResolvedLink(
        id: Long,
        url: String,
    ) = dao.setResolvedLink(id, url)

    suspend fun updatePreview(
        id: Long,
        summary: String?,
        imageUrl: String?,
        resolvedLink: String?,
    ) = dao.updatePreview(id, summary, imageUrl, resolvedLink)

    suspend fun pruneOlderThanDays(days: Int) {
        val cutoff = System.currentTimeMillis() - days * DAY_MS
        dao.prune(cutoff)
    }

    suspend fun recentTitles(sinceMs: Long): List<String> = dao.recentTitles(sinceMs)

    suspend fun hiddenTitles(sinceMs: Long): List<String> = dao.hiddenTitles(sinceMs)

    suspend fun byId(id: Long): ArticleEntity? = dao.byId(id)

    suspend fun latestForWidget(limit: Int): List<ArticleEntity> = dao.activeArticles(limit)

    suspend fun saveReaderText(
        id: Long,
        text: String,
    ) = dao.saveReaderText(id, text, System.currentTimeMillis())

    suspend fun clearReaderCache() = dao.clearReaderCache()

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}
