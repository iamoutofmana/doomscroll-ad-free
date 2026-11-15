package io.github.iamoutofmana.doomscroll.data.network

import io.github.iamoutofmana.doomscroll.domain.links.ArticleHeuristics
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Reader-mode extraction (Phase 2): pulls the article's readable blocks out of a
 * publisher page — headings, paragraphs, quotes, list items — with junk filtered.
 * Rendered as plain text, so there is no HTML injection surface and no publisher ads.
 */
object ArticleTextExtractor {
    data class Extracted(
        val title: String?,
        val paragraphs: List<String>,
    )

    private const val MIN_PARAGRAPH = 30
    private const val MAX_PARAGRAPH = 2000
    private const val MAX_BLOCKS = 80
    private const val MAX_CHARS = 30_000
    private const val MIN_PARAGRAPHS = 1
    private const val MIN_TOTAL_CHARS = 80

    fun extract(
        html: String,
        baseUri: String,
    ): Extracted? {
        val doc = Jsoup.parse(html, baseUri)
        val containers = buildList {
            doc.selectFirst("article")?.let { add(it) }
            doc.selectFirst("main")?.let { add(it) }
            add(doc.body())
        }
        for (container in containers) {
            val blocks = collectBlocks(container)
            if (blocks.size >= MIN_PARAGRAPHS && blocks.sumOf { it.length } >= MIN_TOTAL_CHARS) {
                val title = doc.selectFirst("h1")?.text()?.trim()?.ifBlank { null }
                return Extracted(title, blocks)
            }
        }
        return null
    }

    private fun collectBlocks(container: Element): List<String> {
        val result = mutableListOf<String>()
        var chars = 0
        for (element in container.select("h1, h2, h3, p, blockquote, li")) {
            val text = element.text().trim()
            if (text.length > MAX_PARAGRAPH) continue
            val isHeading = element.tagName() in HEADING_TAGS
            val minLength = if (isHeading) 3 else MIN_PARAGRAPH
            if (text.length < minLength) continue
            if (ArticleHeuristics.isJunk(text)) continue
            result.add(text)
            chars += text.length
            if (result.size >= MAX_BLOCKS || chars >= MAX_CHARS) break
        }
        return result
    }

    private val HEADING_TAGS = setOf("h1", "h2", "h3")
}
