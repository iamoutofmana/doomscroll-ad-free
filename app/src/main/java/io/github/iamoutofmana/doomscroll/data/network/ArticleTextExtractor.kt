package io.github.iamoutofmana.doomscroll.data.network

import io.github.iamoutofmana.doomscroll.domain.links.ArticleHeuristics
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI

/**
 * Reader-mode extraction (Phase 2.1): pulls the article's readable structure out of a
 * publisher page — styled headings, paragraphs, list items and images — with junk filtered.
 * Rendered as native text/image blocks, so there is no HTML injection surface and no
 * publisher ads.
 */
object ArticleTextExtractor {
    data class Extracted(
        val title: String?,
        val blocks: List<ReaderBlock>,
    )

    private const val MIN_PARAGRAPH = 40
    private const val MAX_PARAGRAPH = 2000
    private const val MIN_BULLET = 20
    private const val MAX_HEADING = 160
    private const val MAX_BLOCKS = 140
    private const val MAX_CHARS = 40_000
    private const val MIN_TOTAL_CHARS = 80
    private const val TITLE_MIN = 20
    private const val MAX_IMAGES = 4

    // "... 2 hours ago": these are related-content link rows, not article text.
    private val RELATIVE_TIMESTAMP_END = Regex(
        "(?i)\\b\\d+\\s+(second|minute|hour|day|week|month|year)s?\\s+ago$",
    )
    private val NON_WORD = Regex("[^a-z0-9 ]")
    private val WHITESPACE = Regex("\\s+")
    private val HEADING_TAGS = setOf("h1", "h2", "h3", "h4")

    fun extract(
        html: String,
        baseUri: String,
    ): Extracted? {
        val doc = Jsoup.parse(html, baseUri)
        val title = doc.selectFirst("h1")?.text()?.trim()?.ifBlank { null }
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()?.ifBlank { null }
            ?: doc.title().trim().ifBlank { null }
        val titleKey = title?.takeIf { it.length >= TITLE_MIN }?.let(::normalizeForCompare)

        val containers = buildList {
            doc.selectFirst("article")?.let { add(it) }
            doc.selectFirst("main")?.let { add(it) }
            add(doc.body())
        }
        for (container in containers) {
            val blocks = collectBlocks(container, titleKey)
            val textChars = blocks.filter { it.type != ReaderBlock.IMAGE }.sumOf { it.text?.length ?: 0 }
            if (textChars >= MIN_TOTAL_CHARS) {
                return Extracted(title, addHeroImage(doc, baseUri, blocks))
            }
        }
        return null
    }

    private fun addHeroImage(
        doc: Document,
        baseUri: String,
        blocks: List<ReaderBlock>,
    ): List<ReaderBlock> {
        if (blocks.any { it.type == ReaderBlock.IMAGE }) return blocks
        val ogImage = doc.selectFirst("meta[property=og:image]")?.attr("content")?.trim()?.ifBlank { null }
            ?: doc.selectFirst("meta[name=twitter:image]")?.attr("content")?.trim()?.ifBlank { null }
            ?: return blocks
        val absolute = absoluteUrl(ogImage, baseUri) ?: return blocks
        return listOf(ReaderBlock(type = ReaderBlock.IMAGE, imageUrl = absolute)) + blocks
    }

    private fun collectBlocks(
        container: Element,
        titleKey: String?,
    ): List<ReaderBlock> {
        val result = mutableListOf<ReaderBlock>()
        var chars = 0
        var images = 0
        for (element in container.select("h1, h2, h3, h4, p, blockquote, li, img")) {
            val tag = element.tagName().lowercase()
            if (tag == "img") {
                if (images < MAX_IMAGES) {
                    val src = element.attr("src").ifBlank { element.attr("data-src") }
                    val absolute = absoluteUrl(src, container.baseUri())
                    if (absolute != null) {
                        result.add(ReaderBlock(type = ReaderBlock.IMAGE, imageUrl = absolute))
                        images++
                    }
                }
                continue
            }
            val text = element.text().trim()
            if (text.isEmpty()) continue

            when {
                tag in HEADING_TAGS -> {
                    if (text.length !in 3..MAX_HEADING) continue
                    if (ArticleHeuristics.isJunk(text)) continue
                    val key = normalizeForCompare(text)
                    if (titleKey != null && (key == titleKey || key.startsWith(titleKey) || titleKey.startsWith(key))) {
                        continue
                    }
                    if (result.any { it.type == ReaderBlock.HEADING && normalizeForCompare(it.text.orEmpty()) == key }) continue
                    result.add(ReaderBlock(type = ReaderBlock.HEADING, text = text))
                    chars += text.length
                }

                tag == "li" -> {
                    if (text.length < MIN_BULLET) continue
                    if (ArticleHeuristics.isJunk(text)) continue
                    result.add(ReaderBlock(type = ReaderBlock.BULLET, text = text))
                    chars += text.length
                }

                else -> {
                    if (text.length < MIN_PARAGRAPH || text.length > MAX_PARAGRAPH) continue
                    if (ArticleHeuristics.isJunk(text)) continue
                    if (RELATIVE_TIMESTAMP_END.containsMatchIn(text)) continue
                    val key = normalizeForCompare(text)
                    if (titleKey != null && (key == titleKey || key.startsWith(titleKey) || titleKey.startsWith(key))) {
                        continue
                    }
                    if (result.any { it.type == ReaderBlock.PARAGRAPH && normalizeForCompare(it.text.orEmpty()) == key }) continue
                    result.add(ReaderBlock(type = ReaderBlock.PARAGRAPH, text = text))
                    chars += text.length
                }
            }
            if (result.size >= MAX_BLOCKS || chars >= MAX_CHARS) break
        }
        return result
    }

    private fun absoluteUrl(
        raw: String,
        baseUri: String,
    ): String? = runCatching {
        val url = URI(baseUri).resolve(raw.trim()).toString()
        if (url.startsWith("http://") || url.startsWith("https://")) url else null
    }.getOrNull()

    private fun normalizeForCompare(text: String): String =
        WHITESPACE.replace(NON_WORD.replace(text.lowercase(), " "), " ").trim()
}
