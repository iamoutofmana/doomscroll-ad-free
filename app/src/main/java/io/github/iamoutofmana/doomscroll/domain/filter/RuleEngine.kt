package io.github.iamoutofmana.doomscroll.domain.filter

import java.net.URI

enum class RuleType {
    /** Hide stories whose title (or publisher name) contains the pattern. */
    KEYWORD,

    /** Hide stories whose link host (or publisher name) contains the pattern. */
    DOMAIN,
}

/** A user-defined hide rule (Phase 2 filter engine). */
data class Rule(
    val id: Long,
    val type: RuleType,
    val pattern: String,
    val enabled: Boolean,
)

/**
 * Pure matching logic for user rules. Deliberately simple substring matching —
 * predictable beats clever when the user is hand-tuning their feed.
 *
 * DOMAIN matches the link host (publisher URLs) and the publisher name, because
 * Google News items often only carry the publisher name until their link resolves.
 */
object RuleEngine {
    fun isBlocked(
        title: String,
        link: String,
        publisher: String?,
        rules: List<Rule>,
    ): Rule? {
        if (rules.isEmpty()) return null
        val host = hostOf(link)
        return rules.firstOrNull { rule ->
            rule.enabled && when (rule.type) {
                RuleType.KEYWORD ->
                    title.contains(rule.pattern, ignoreCase = true) ||
                        publisher?.contains(rule.pattern, ignoreCase = true) == true
                RuleType.DOMAIN ->
                    host.contains(rule.pattern, ignoreCase = true) ||
                        publisher?.contains(rule.pattern, ignoreCase = true) == true
            }
        }
    }

    fun hostOf(link: String): String =
        runCatching { URI(link).host.orEmpty().lowercase() }.getOrDefault("")
}
