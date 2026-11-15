package io.github.iamoutofmana.doomscroll.data.network

import io.github.iamoutofmana.doomscroll.domain.links.ArticleHeuristics
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Reader-mode extraction (Phase 2): pulls the article's readable paragraphs out of a
 * publisher page — junk filtered, repeated headlines removed, related-link lists dropped.
 * Rendered as plain text, so there is no HTML injection surface and no publisher ads.
 */
object ArticleTextExtractor {
    data class Extracted(
        val title: String?,
        val paragraphs: List<String>,
    )

    private const val MIN_PARAGRAPH = 40
    private const val MAX_PARAGRAPH = 2000
    private const val MAX_BLOCKS = 80
    private const val MAX_CHARS = 30_000
    private const val MIN_TOTAL_CHARS = 80
    private const val TITLE_MIN = 20

    // "... 2 hours ago" / "... 4 days ago": these are related-content link rows, not article text.
    private val RELATIVE_TIMESTAMP_END = Regex(
        "(?i)\\b\\d+\\s+(second|minute|hour|day|week|month|year)s?\\s+ago$",
    )
    private val NON_WORD = Regex("[^a-z0-9 ]")
    private val WHITESPACE = Regex("\\s+")

    fun extract(
        html: String,
        baseUri: String,
    ): Extracted? {
        val doc = Jsoup.parse(html, baseUri)
        val title = doc.selectFirst("h1")?.text()?.trim()?.ifBlank { null }
            ?: doc.title().trim().ifBlank { null }
        val titleKey = title?.takeIf { it.length >= TITLE_MIN }?.let(::normalizeForCompare)

        val containers = buildList {
            doc.selectFirst("article")?.let { add(it) }
            doc.selectFirst("main")?.let { add(it) }
            add(doc.body())
        }
        for (container in containers) {
            val blocks = collectBlocks(container, titleKey)
            if (blocks.isNotEmpty() && blocks.sumOf { it.length } >= MIN_TOTAL_CHARS) {
                return Extracted(title, blocks)
            }
        }
        return null
    }

    private fun collectBlocks(
        container: Element,
        titleKey: String?,
    ): List<String> {
        val result = mutableListOf<String>()
        var chars = 0
        for (element in container.select("p, blockquote")) {
            val text = element.text().trim()
            if (text.length < MIN_PARAGRAPH || text.length > MAX_PARAGRAPH) continue
            if (ArticleHeuristics.isJunk(text)) continue
            if (RELATIVE_TIMESTAMP_END.containsMatchIn(text)) continue
            val key = normalizeForCompare(text)
            // Skip the repeated headline (some sites render it again inside the body).
            if (titleKey != null && (key == titleKey || key.startsWith(titleKey) || titleKey.startsWith(key))) {
                continue
            }
            // Skip duplicate blocks (teasers, repeated intros).
            if (result.any { normalizeForCompare(it) == key }) continue
            result.add(text)
            chars += text.length
            if (result.size >= MAX_BLOCKS || chars >= MAX_CHARS) break
        }
        return result
    }

    private fun normalizeForCompare(text: String): String =
        WHITESPACE.replace(NON_WORD.replace(text.lowercase(), " "), " ").trim()
}
