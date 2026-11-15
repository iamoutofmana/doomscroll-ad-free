package io.github.iamoutofmana.doomscroll.feedparser

import io.github.iamoutofmana.doomscroll.domain.links.HtmlText
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Streaming parser for RSS 2.0 / RDF / Atom. Namespace-unaware on purpose: tags are matched
 * by qualified name ("media:content", "content:encoded"), which keeps prefix handling trivial.
 *
 * Defensive against real-world feed weirdness: CDATA, entities, empty tags, unknown elements
 * (consumed and ignored), and malformed text elements (drained, not fatal).
 */
object XmlFeedParser {
    private val IMG_SRC = Regex("""<img[^>]+src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    fun parse(text: String): ParsedFeed {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        // Defense in depth: never process DTDs (XXE / entity-expansion bombs).
        runCatching { parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false) }
        parser.setInput(StringReader(text))
        var eventType = parser.eventType
        while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
            eventType = parser.next()
        }
        if (eventType == XmlPullParser.END_DOCUMENT) {
            return ParsedFeed(null, null, null, emptyList())
        }
        return when (parser.name.lowercase()) {
            "feed" -> parseAtomFeed(parser)
            else -> parseRssFeed(parser)
        }
    }

    // ---- RSS 2.0 / RDF ----

    private fun parseRssFeed(parser: XmlPullParser): ParsedFeed {
        var feedTitle: String? = null
        var feedLink: String? = null
        var feedDescription: String? = null
        val items = mutableListOf<ParsedItem>()
        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                    "channel" -> {} // descend; children handled by this loop
                    "item" -> items.add(parseRssItem(parser))
                    "title" -> if (feedTitle == null) feedTitle = safeText(parser).ifBlank { null }
                    "link" -> if (feedLink == null) feedLink = safeText(parser).ifBlank { null }
                    "description" -> if (feedDescription == null) feedDescription = safeText(parser)
                    else -> consumeSubtree(parser)
                }
                XmlPullParser.END_TAG -> if (parser.name.lowercase() == "rss" || parser.name.lowercase() == "rdf") break
                XmlPullParser.END_DOCUMENT -> break
                else -> {}
            }
        }
        return ParsedFeed(feedTitle, feedLink, feedDescription, items)
    }

    private fun parseRssItem(parser: XmlPullParser): ParsedItem {
        var title = ""
        var link: String? = null
        var guid: String? = null
        var pubDate: String? = null
        var description: String? = null
        var contentEncoded: String? = null
        var author: String? = null
        var publisher: String? = null
        val categories = mutableListOf<String>()
        var mediaUrl: String? = null
        var mediaType: String? = null
        var mediaMedium: String? = null
        var thumbUrl: String? = null
        var enclosureUrl: String? = null
        var enclosureType: String? = null

        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                    "title" -> title = safeText(parser)
                    "link" -> if (link == null) link = safeText(parser).ifBlank { null }
                    "guid" -> guid = safeText(parser).ifBlank { null }
                    "pubdate" -> pubDate = safeText(parser).ifBlank { null }
                    "dc:date" -> if (pubDate == null) pubDate = safeText(parser).ifBlank { null }
                    "description" -> description = safeText(parser)
                    "content:encoded" -> contentEncoded = safeText(parser)
                    "author", "dc:creator" -> author = safeText(parser).ifBlank { null }
                    "source" -> publisher = safeText(parser).ifBlank { null }
                    "category" -> {
                        val text = safeText(parser)
                        if (text.isNotBlank()) categories.add(text)
                    }
                    "media:content" -> {
                        attr(parser, "url")?.let { mediaUrl = it }
                        mediaType = attr(parser, "type")
                        mediaMedium = attr(parser, "medium")
                        consumeSubtree(parser)
                    }
                    "media:thumbnail" -> {
                        if (thumbUrl == null) attr(parser, "url")?.let { thumbUrl = it }
                        consumeSubtree(parser)
                    }
                    "media:group" -> {} // descend into grouped media children
                    "enclosure" -> {
                        attr(parser, "url")?.let { enclosureUrl = it }
                        enclosureType = attr(parser, "type")
                        consumeSubtree(parser)
                    }
                    else -> consumeSubtree(parser)
                }
                XmlPullParser.END_TAG -> if (parser.name.lowercase() == "item") break
                XmlPullParser.END_DOCUMENT -> break
                else -> {}
            }
        }

        return ParsedItem(
            guid = guid ?: link ?: title,
            title = title,
            link = link,
            author = author,
            publishedAt = FeedDateParser.parse(pubDate),
            summaryText = firstNonBlank(description, contentEncoded)?.let(HtmlText::strip)?.ifBlank { null },
            imageUrl = pickImage(mediaUrl, mediaType, mediaMedium, thumbUrl, enclosureUrl, enclosureType, contentEncoded, description),
            audioUrl = pickByType(enclosureUrl, enclosureType, mediaUrl, mediaType, "audio/"),
            videoUrl = pickByType(enclosureUrl, enclosureType, mediaUrl, mediaType, "video/"),
            publisher = publisher,
            categories = categories,
        )
    }

    // ---- Atom ----

    private fun parseAtomFeed(parser: XmlPullParser): ParsedFeed {
        var feedTitle: String? = null
        var feedLink: String? = null
        var feedDescription: String? = null
        val items = mutableListOf<ParsedItem>()
        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                    "entry" -> items.add(parseAtomEntry(parser))
                    "title" -> if (feedTitle == null) feedTitle = safeText(parser).ifBlank { null }
                    "subtitle" -> if (feedDescription == null) feedDescription = safeText(parser)
                    "link" -> {
                        val rel = attr(parser, "rel")
                        val href = attr(parser, "href")
                        if (feedLink == null && (rel == null || rel == "alternate")) feedLink = href
                        consumeSubtree(parser)
                    }
                    else -> consumeSubtree(parser)
                }
                XmlPullParser.END_TAG -> if (parser.name.lowercase() == "feed") break
                XmlPullParser.END_DOCUMENT -> break
                else -> {}
            }
        }
        return ParsedFeed(feedTitle, feedLink, feedDescription, items)
    }

    private fun parseAtomEntry(parser: XmlPullParser): ParsedItem {
        var title = ""
        var id: String? = null
        var link: String? = null
        var author: String? = null
        var published: String? = null
        var updated: String? = null
        var summary: String? = null
        var content: String? = null
        var thumbUrl: String? = null
        var mediaUrl: String? = null
        var mediaType: String? = null
        var mediaMedium: String? = null
        var enclosureUrl: String? = null
        var enclosureType: String? = null
        val categories = mutableListOf<String>()

        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                    "title" -> title = safeText(parser)
                    "id" -> id = safeText(parser).ifBlank { null }
                    "link" -> {
                        val rel = attr(parser, "rel")
                        val href = attr(parser, "href")
                        val type = attr(parser, "type")
                        if (rel == "enclosure" && href != null && enclosureUrl == null) {
                            enclosureUrl = href
                            enclosureType = type
                        } else if (link == null && (rel == null || rel == "alternate") && href != null) {
                            link = href
                        }
                        consumeSubtree(parser)
                    }
                    "author" -> author = parseAtomPerson(parser) ?: author
                    "published" -> published = safeText(parser).ifBlank { null }
                    "updated" -> updated = safeText(parser).ifBlank { null }
                    "issued" -> if (published == null) published = safeText(parser).ifBlank { null }
                    "summary" -> summary = safeText(parser)
                    "content" -> content = safeText(parser)
                    "category" -> {
                        val term = attr(parser, "term")
                        if (term != null) {
                            categories.add(term)
                            consumeSubtree(parser)
                        } else {
                            val text = safeText(parser)
                            if (text.isNotBlank()) categories.add(text)
                        }
                    }
                    "media:thumbnail" -> {
                        if (thumbUrl == null) attr(parser, "url")?.let { thumbUrl = it }
                        consumeSubtree(parser)
                    }
                    "media:content" -> {
                        attr(parser, "url")?.let { mediaUrl = it }
                        mediaType = attr(parser, "type")
                        mediaMedium = attr(parser, "medium")
                        consumeSubtree(parser)
                    }
                    "media:group" -> {} // descend
                    else -> consumeSubtree(parser)
                }
                XmlPullParser.END_TAG -> if (parser.name.lowercase() == "entry") break
                XmlPullParser.END_DOCUMENT -> break
                else -> {}
            }
        }

        return ParsedItem(
            guid = id ?: link ?: title,
            title = title,
            link = link,
            author = author,
            publishedAt = FeedDateParser.parse(firstNonBlank(published, updated)),
            summaryText = firstNonBlank(summary, content)?.let(HtmlText::strip)?.ifBlank { null },
            imageUrl = pickImage(mediaUrl, mediaType, mediaMedium, thumbUrl, enclosureUrl, enclosureType, content, summary),
            audioUrl = pickByType(enclosureUrl, enclosureType, mediaUrl, mediaType, "audio/"),
            videoUrl = pickByType(enclosureUrl, enclosureType, mediaUrl, mediaType, "video/"),
            publisher = null,
            categories = categories,
        )
    }

    private fun parseAtomPerson(parser: XmlPullParser): String? {
        var name: String? = null
        var email: String? = null
        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                    "name" -> name = safeText(parser).ifBlank { null }
                    "email" -> email = safeText(parser).ifBlank { null }
                    else -> consumeSubtree(parser)
                }
                XmlPullParser.END_TAG -> if (parser.name.lowercase() == "author") return name ?: email
                XmlPullParser.END_DOCUMENT -> return name ?: email
                else -> {}
            }
        }
    }

    // ---- helpers ----

    private fun attr(parser: XmlPullParser, name: String): String? =
        parser.getAttributeValue(null, name)?.trim()?.ifBlank { null }

    private fun safeText(parser: XmlPullParser): String =
        try {
            parser.nextText().trim()
        } catch (e: Exception) {
            // Malformed element (nested tags where text was expected): drain and move on.
            drainToElementEnd(parser)
            ""
        }

    private fun drainToElementEnd(parser: XmlPullParser) {
        val depth = parser.depth
        while (true) {
            val ev = parser.next()
            if (ev == XmlPullParser.END_DOCUMENT) return
            if (ev == XmlPullParser.END_TAG && parser.depth <= depth) return
        }
    }

    /** Parser must sit on a START_TAG; consumes through the matching END_TAG. */
    private fun consumeSubtree(parser: XmlPullParser) {
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return
                else -> {}
            }
        }
    }

    private fun pickImage(
        mediaUrl: String?,
        mediaType: String?,
        mediaMedium: String?,
        thumbUrl: String?,
        enclosureUrl: String?,
        enclosureType: String?,
        contentHtml: String?,
        descriptionHtml: String?,
    ): String? = when {
        mediaMedium == "image" && mediaUrl != null -> mediaUrl
        mediaType?.startsWith("image/") == true && mediaUrl != null -> mediaUrl
        thumbUrl != null -> thumbUrl
        enclosureType?.startsWith("image/") == true -> enclosureUrl
        else -> firstImageInHtml(contentHtml) ?: firstImageInHtml(descriptionHtml)
    }

    private fun pickByType(
        enclosureUrl: String?,
        enclosureType: String?,
        mediaUrl: String?,
        mediaType: String?,
        prefix: String,
    ): String? = when {
        enclosureType?.startsWith(prefix) == true -> enclosureUrl
        mediaType?.startsWith(prefix) == true -> mediaUrl
        else -> null
    }

    private fun firstImageInHtml(html: String?): String? =
        html?.let { IMG_SRC.find(it)?.groupValues?.get(1) }

    private fun firstNonBlank(vararg values: String?): String? = values.firstOrNull { !it.isNullOrBlank() }
}
