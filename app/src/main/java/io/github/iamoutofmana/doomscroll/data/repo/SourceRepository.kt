package io.github.iamoutofmana.doomscroll.data.repo

import io.github.iamoutofmana.doomscroll.data.db.SourceDao
import io.github.iamoutofmana.doomscroll.data.db.SourceEntity
import io.github.iamoutofmana.doomscroll.domain.model.SourceType
import kotlinx.coroutines.flow.Flow

class SourceRepository(private val dao: SourceDao) {
    val sources: Flow<List<SourceEntity>> = dao.observeAll()

    suspend fun enabled(): List<SourceEntity> = dao.enabled()

    suspend fun add(
        type: SourceType,
        url: String,
        title: String,
    ): Long = dao.insert(
        SourceEntity(
            type = type.name,
            url = url,
            title = title,
            createdAt = System.currentTimeMillis(),
        ),
    )

    suspend fun setEnabled(
        id: Long,
        enabled: Boolean,
    ) = dao.setEnabled(id, enabled)

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun rename(
        id: Long,
        title: String,
    ) = dao.rename(id, title)

    suspend fun markSynced(
        id: Long,
        at: Long,
        etag: String?,
        lastModified: String?,
    ) = dao.markSynced(id, at, etag, lastModified)

    suspend fun markError(
        id: Long,
        error: String,
        at: Long,
    ) = dao.markError(id, error, at)
}
