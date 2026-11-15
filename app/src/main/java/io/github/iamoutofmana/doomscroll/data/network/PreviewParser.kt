package io.github.iamoutofmana.doomscroll.data.network

import io.github.iamoutofmana.doomscroll.domain.links.ArticleHeuristics
import io.github.iamoutofmana.doomscroll.domain.links.HtmlText
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

/**
 * Extracts an article preview (summary + hero image) from a publisher page.
 *
 * Feeds frequently carry nothing useful — Google News always does — so when a card is
 * expanded we scrape the publisher page: og:image for the hero, and the article's own
 * opening paragraphs for a summary that actually says something beyond the headline
 * (og:description is usually just a subtitle echo — user feedback #1).
 */
object PreviewParser {
    data class Preview(
        val description: String?,
        val imageUrl: String?,
    )

    private const val DESCRIPTION_MAX = 600
    private const val PARAGRAPH_MIN = 60
    private const val PARAGRAPH_MAX = 800
    private const val MAX_PARAGRAPHS = 3

    fun parse(
        html: String,
        baseUri: String,
    ): Preview {
        val doc = Jsoup.parse(html, baseUri)

        fun meta(
            selector: String,
            resolveUrl: Boolean,
        ): String? {
            val element = doc.selectFirst(selector) ?: return null
            val raw = element.attr("content").trim()
            if (raw.isEmpty()) return null
            return if (resolveUrl) element.absUrl("content").ifBlank { raw } else raw
        }

        val imageUrl = meta("meta[property=og:image]", resolveUrl = true)
            ?: meta("meta[name=og:image]", resolveUrl = true)
            ?: meta("meta[name=twitter:image]", resolveUrl = true)

        val ogDescription = clean(meta("meta[property=og:description]", resolveUrl = false))
        val metaDescription = clean(meta("meta[name=description]", resolveUrl = false))
        val bodyExcerpt = extractBodyExcerpt(doc)

        val description = listOfNotNull(ogDescription, metaDescription, bodyExcerpt)
            .maxByOrNull { it.length }
            ?.let(::cap)

        return Preview(description, imageUrl)
    }

    private fun extractBodyExcerpt(doc: Document): String? {
        val containers = mutableListOf<Elements>()
        doc.selectFirst("article")?.let { containers.add(it.select("p")) }
        containers.add(doc.select("main p"))
        containers.add(doc.select("p"))

        for (elements in containers) {
            val paragraphs = elements
                .map { it.text().trim() }
                .filter { it.length in PARAGRAPH_MIN..PARAGRAPH_MAX }
                .filterNot(ArticleHeuristics::isJunk)
            if (paragraphs.isNotEmpty()) {
                return paragraphs.take(MAX_PARAGRAPHS).joinToString("\n\n")
            }
        }
        return null
    }

    private fun clean(text: String?): String? =
        text?.let(HtmlText::strip)?.ifBlank { null }

    private fun cap(text: String): String =
        if (text.length > DESCRIPTION_MAX) {
            text.take(DESCRIPTION_MAX).trimEnd() + "…"
        } else {
            text
        }
}
