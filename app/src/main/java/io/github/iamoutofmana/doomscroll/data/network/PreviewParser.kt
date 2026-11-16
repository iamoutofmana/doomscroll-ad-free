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
    private const val MIN_IMAGE_WIDTH = 200
    private const val MIN_IMAGE_HEIGHT = 120

    // Site chrome rather than article imagery.
    private val IMAGE_JUNK = Regex(
        "(?i)(logo|sprite|icon|avatar|placeholder|pixel|spacer|badge|masthead|1x1" +
            "|feedburner|doubleclick|advert|/ads?/)",
    )
    private val IMAGE_JUNK_HINTS = listOf("logo", "icon", "brand", "masthead", "badge", "avatar", "sprite")
    private val IMAGE_SOURCE_ATTRS = listOf("src", "data-src", "data-lazy-src", "data-original")

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
            ?: firstContentImage(doc)

        val ogDescription = clean(meta("meta[property=og:description]", resolveUrl = false))
        val metaDescription = clean(meta("meta[name=description]", resolveUrl = false))
        val bodyExcerpt = extractBodyExcerpt(doc)

        val description = listOfNotNull(ogDescription, metaDescription, bodyExcerpt)
            .maxByOrNull { it.length }
            ?.let(::cap)

        return Preview(description, imageUrl)
    }

    private fun firstContentImage(doc: Document): String? {
        val scopes = listOfNotNull(doc.selectFirst("article"), doc.selectFirst("main"), doc.body())
        for (scope in scopes) {
            for (img in scope.select("img")) {
                val attr = IMAGE_SOURCE_ATTRS.firstOrNull { img.attr(it).isNotBlank() } ?: continue
                val raw = img.attr(attr).trim()
                if (IMAGE_JUNK.containsMatchIn(raw)) continue
                val hint = (img.className() + " " + img.id()).lowercase()
                if (IMAGE_JUNK_HINTS.any { hint.contains(it) }) continue
                val width = img.attr("width").toIntOrNull()
                val height = img.attr("height").toIntOrNull()
                if (width != null && width < MIN_IMAGE_WIDTH) continue
                if (height != null && height < MIN_IMAGE_HEIGHT) continue
                val absolute = img.absUrl(attr).ifBlank { raw }
                if (absolute.startsWith("https://") || absolute.startsWith("http://")) return absolute
            }
        }
        return null
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
