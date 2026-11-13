package io.github.iamoutofmana.doomscroll.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.iamoutofmana.doomscroll.domain.model.SourceType

// Phase 1 simplification (documented in docs/PLAN.md §5): Room entities double as UI models.
// When the project modularizes, split domain models + mappers out of the data layer.

@Entity(tableName = "sources", indices = [Index(value = ["url"], unique = true)])
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** SourceType.name — stored as string for schema stability. */
    val type: String,
    val url: String,
    val title: String,
    val enabled: Boolean = true,
    val createdAt: Long,
    val lastSyncAt: Long? = null,
    val etag: String? = null,
    val lastModified: String? = null,
    val lastError: String? = null,
) {
    val sourceType: SourceType
        get() = SourceType.valueOf(type)
}

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sourceId", "guid"], unique = true),
        Index(value = ["publishedAt"]),
        Index(value = ["savedAt"]),
    ],
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    /** Stable per-source identity; duplicate inserts are ignored. */
    val guid: String,
    val title: String,
    /** Raw link from the feed (Google News items: the news.google.com redirect). */
    val link: String,
    /** Publisher URL after redirect resolution; null until resolved (best-effort). */
    val resolvedLink: String? = null,
    val publisher: String? = null,
    val author: String? = null,
    val publishedAt: Long? = null,
    val fetchedAt: Long,
    /** Plain-text summary (HTML stripped at ingest). */
    val summary: String? = null,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val videoUrl: String? = null,
    val readAt: Long? = null,
    val savedAt: Long? = null,
    val hiddenAt: Long? = null,
    /** HiddenReason.name — always logged; hiding is reversible (plan §7). */
    val hiddenReason: String? = null,
)
