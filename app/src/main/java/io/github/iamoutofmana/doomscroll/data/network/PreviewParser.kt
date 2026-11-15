package io.github.iamoutofmana.doomscroll.data.network

import io.github.iamoutofmana.doomscroll.domain.links.HtmlText
import org.jsoup.Jsoup

/**
 * Extracts an article preview (og:description, og:image) from a publisher page.
 * Feeds frequently omit these (Google News always does) — this fills the card in
 * when the user expands it.
 */
object PreviewParser {
    data class Preview(
        val description: String?,
        val imageUrl: String?,
    )

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

        val firstParagraph = doc.select("p").firstOrNull { it.text().length > 80 }?.text()
        val description = (
            meta("meta[property=og:description]", resolveUrl = false)
                ?: meta("meta[name=description]", resolveUrl = false)
                ?: firstParagraph
        )
            ?.let(HtmlText::strip)
            ?.let { text -> if (text.length > DESCRIPTION_MAX) text.take(DESCRIPTION_MAX).trimEnd() + "…" else text }

        return Preview(description, imageUrl)
    }

    private const val DESCRIPTION_MAX = 400
}
