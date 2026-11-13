package io.github.iamoutofmana.doomscroll.domain.filter

/**
 * Conservative sponsor/advertorial heuristics (plan §7).
 *
 * Deliberately narrow: false positives hide real news silently, so we only flag
 * unambiguous markers. Looser phrases ("presented by") join in Phase 2 with a
 * confidence-tiered engine and a review surface.
 */
object SponsoredFilter {
    private val PATTERNS = listOf(
        Regex("(?i)\\bsponsored\\b"),
        Regex("(?i)\\badvertorial\\b"),
        Regex("(?i)\\bpaid\\s+(post|content|promotion|article)\\b"),
        Regex("(?i)\\bpromoted\\b"),
        Regex("(?i)\\bpartner\\s+content\\b"),
        Regex("(?i)\\bnative\\s+ad(verti[sz]ement|vertising)?\\b"),
        Regex("(?i)#(ad|sponsored|advertorial)\\b"),
        Regex("(?i)\\bpublicit[eé]\\b"),
    )

    private val CATEGORY_MARKERS = setOf("sponsor", "sponsored", "advertorial", "paid post", "native advertising")

    fun isSponsored(
        title: String,
        categories: List<String> = emptyList(),
    ): Boolean = PATTERNS.any { it.containsMatchIn(title) } ||
        categories.any { it.trim().lowercase() in CATEGORY_MARKERS }
}
