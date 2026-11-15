package io.github.iamoutofmana.doomscroll.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: SourceEntity): Long

    @Query("SELECT * FROM sources ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE enabled = 1")
    suspend fun enabled(): List<SourceEntity>

    @Query("UPDATE sources SET lastSyncAt = :at, etag = :etag, lastModified = :lastModified, lastError = NULL WHERE id = :id")
    suspend fun markSynced(id: Long, at: Long, etag: String?, lastModified: String?)

    @Query("UPDATE sources SET lastError = :error, lastSyncAt = :at WHERE id = :id")
    suspend fun markError(id: Long, error: String, at: Long)

    @Query("UPDATE sources SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE sources SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<ArticleEntity>): List<Long>

    @Query("SELECT * FROM articles WHERE hiddenAt IS NULL ORDER BY COALESCE(publishedAt, fetchedAt) DESC LIMIT 500")
    fun observeFeed(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE savedAt IS NOT NULL ORDER BY savedAt DESC")
    fun observeSaved(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE hiddenAt IS NOT NULL ORDER BY hiddenAt DESC")
    fun observeHidden(): Flow<List<ArticleEntity>>

    @Query("UPDATE articles SET readAt = :at WHERE id = :id AND readAt IS NULL")
    suspend fun markRead(id: Long, at: Long)

    @Query("UPDATE articles SET readAt = :at WHERE readAt IS NULL AND hiddenAt IS NULL")
    suspend fun markAllRead(at: Long)

    @Query("UPDATE articles SET savedAt = :at WHERE id = :id")
    suspend fun setSaved(id: Long, at: Long?)

    @Query("UPDATE articles SET hiddenAt = :at, hiddenReason = :reason WHERE id = :id")
    suspend fun setHidden(id: Long, at: Long?, reason: String?)

    @Query("UPDATE articles SET hiddenAt = NULL, hiddenReason = NULL WHERE hiddenAt IS NOT NULL")
    suspend fun unhideAll()

    @Query("UPDATE articles SET resolvedLink = :url WHERE id = :id")
    suspend fun setResolvedLink(id: Long, url: String)

    @Query("DELETE FROM articles WHERE savedAt IS NULL AND fetchedAt < :cutoff")
    suspend fun prune(cutoff: Long)

    /** Titles of recent articles, across all sources — used by cross-source dedupe. */
    @Query("SELECT title FROM articles WHERE COALESCE(publishedAt, fetchedAt) >= :cutoff")
    suspend fun recentTitles(cutoff: Long): List<String>

    /** Titles the user (or the filter) hid since `cutoff` — never re-shown (plan §7). */
    @Query("SELECT title FROM articles WHERE hiddenAt IS NOT NULL AND hiddenAt >= :cutoff")
    suspend fun hiddenTitles(cutoff: Long): List<String>

    /** Fills in lazily-fetched preview fields without clobbering existing values. */
    @Query(
        "UPDATE articles SET summary = COALESCE(:summary, summary), " +
            "imageUrl = COALESCE(:imageUrl, imageUrl), " +
            "resolvedLink = COALESCE(:resolvedLink, resolvedLink) WHERE id = :id",
    )
    suspend fun updatePreview(id: Long, summary: String?, imageUrl: String?, resolvedLink: String?)

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun byId(id: Long): ArticleEntity?

    @Query("SELECT * FROM articles WHERE hiddenAt IS NULL ORDER BY COALESCE(publishedAt, fetchedAt) DESC LIMIT :limit")
    suspend fun activeArticles(limit: Int): List<ArticleEntity>

    @Query("UPDATE articles SET readerText = :text, readerFetchedAt = :at WHERE id = :id")
    suspend fun saveReaderText(id: Long, text: String, at: Long)

    @Query("UPDATE articles SET readerText = NULL, readerFetchedAt = NULL WHERE readerText IS NOT NULL")
    suspend fun clearReaderCache()
}

@Dao
interface RuleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rule: RuleEntity): Long

    @Query("SELECT * FROM rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE enabled = 1")
    suspend fun enabled(): List<RuleEntity>

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun delete(id: Long)
}
