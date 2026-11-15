package io.github.iamoutofmana.doomscroll.data.repo

import io.github.iamoutofmana.doomscroll.data.db.ArticleDao
import io.github.iamoutofmana.doomscroll.data.db.RuleDao
import io.github.iamoutofmana.doomscroll.data.db.RuleEntity
import io.github.iamoutofmana.doomscroll.domain.filter.Rule
import io.github.iamoutofmana.doomscroll.domain.filter.RuleEngine
import io.github.iamoutofmana.doomscroll.domain.filter.RuleType
import kotlinx.coroutines.flow.Flow

fun RuleEntity.toDomain(): Rule = Rule(id, RuleType.valueOf(type), pattern, enabled)

/**
 * User filter rules (Phase 2 filter engine): hide by keyword or domain.
 * Rules apply at ingest (SyncEngine) and retroactively when created — with the
 * hidden reason logged so everything stays reviewable and reversible (plan §7).
 */
class RuleRepository(
    private val dao: RuleDao,
    private val articleDao: ArticleDao,
) {
    val rules: Flow<List<RuleEntity>> = dao.observeAll()

    suspend fun enabledRules(): List<Rule> = dao.enabled().map { it.toDomain() }

    suspend fun add(
        type: RuleType,
        rawPattern: String,
    ) {
        val pattern = normalize(type, rawPattern) ?: return
        val id = dao.insert(
            RuleEntity(
                type = type.name,
                pattern = pattern,
                createdAt = System.currentTimeMillis(),
            ),
        )
        if (id == -1L) return // duplicate rule

        // Retroactively hide currently visible matches for immediate feedback.
        val rule = Rule(id, type, pattern, true)
        val now = System.currentTimeMillis()
        articleDao.activeArticles(600)
            .filter { RuleEngine.isBlocked(it.title, it.link, it.publisher, listOf(rule)) != null }
            .forEach { articleDao.setHidden(it.id, now, "Rule: $pattern") }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    private fun normalize(
        type: RuleType,
        raw: String,
    ): String? {
        val trimmed = raw.trim().lowercase()
        if (trimmed.isBlank()) return null
        return when (type) {
            RuleType.KEYWORD -> trimmed
            RuleType.DOMAIN ->
                trimmed
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .substringBefore('/')
                    .removePrefix("www.")
                    .ifBlank { null }
        }
    }
}
