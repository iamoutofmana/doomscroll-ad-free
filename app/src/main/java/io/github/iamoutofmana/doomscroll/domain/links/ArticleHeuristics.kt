package io.github.iamoutofmana.doomscroll.domain.links

/** Shared text heuristics for preview and reader extraction. */
object ArticleHeuristics {
    private val JUNK_PATTERNS = listOf(
        Regex("(?i)cookie"),
        Regex("(?i)subscribe"),
        Regex("(?i)newsletter"),
        Regex("(?i)sign\\s*(up|in)"),
        Regex("(?i)log\\s*in"),
        Regex("(?i)advertis"),
        Regex("(?i)all rights reserved"),
        Regex("(?i)privacy policy"),
        Regex("(?i)terms of (service|use)"),
        Regex("(?i)share (this|on)"),
        Regex("(?i)follow us"),
        Regex("(?i)enable javascript"),
        Regex("(?i)related (articles|stories|content)"),
        Regex("(?i)supported by"),
        Regex("(?i)continue reading"),
        Regex("(?i)read more"),
        Regex("(?i)download (the|our) app"),
    )

    fun isJunk(text: String): Boolean = JUNK_PATTERNS.any { it.containsMatchIn(text) }
}
