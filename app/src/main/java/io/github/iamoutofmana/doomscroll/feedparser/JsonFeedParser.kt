package io.github.iamoutofmana.doomscroll.feedparser

import io.github.iamoutofmana.doomscroll.domain.links.HtmlText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** JSON Feed 1.x support (jsonfeed.org) — some modern blogs publish it instead of XML. */
object JsonFeedParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(text: String): ParsedFeed {
        val feed = json.decodeFromString<JsonFeedDto>(text)
        return ParsedFeed(
            title = feed.title,
            siteUrl = feed.homePageUrl,
            description = feed.description,
            items = feed.items.map { it.toParsed() },
        )
    }

    private fun JsonItemDto.toParsed(): ParsedItem {
        val link = url ?: externalUrl
        val summaryRaw = contentText ?: summary ?: contentHtml?.let(HtmlText::strip)
        return ParsedItem(
            guid = id ?: link ?: title ?: "",
            title = title ?: "",
            link = link,
            author = authors.firstOrNull { !it.name.isNullOrBlank() }?.name,
            publishedAt = FeedDateParser.parse(datePublished ?: dateModified),
            summaryText = summaryRaw?.trim()?.ifBlank { null },
            imageUrl = image ?: bannerImage,
            audioUrl = null,
            videoUrl = null,
            publisher = null,
            categories = tags,
        )
    }
}

@Serializable
private data class JsonFeedDto(
    val title: String? = null,
    @SerialName("home_page_url") val homePageUrl: String? = null,
    val description: String? = null,
    val items: List<JsonItemDto> = emptyList(),
)

@Serializable
private data class JsonItemDto(
    val id: String? = null,
    val url: String? = null,
    @SerialName("external_url") val externalUrl: String? = null,
    val title: String? = null,
    @SerialName("content_html") val contentHtml: String? = null,
    @SerialName("content_text") val contentText: String? = null,
    val summary: String? = null,
    @SerialName("date_published") val datePublished: String? = null,
    @SerialName("date_modified") val dateModified: String? = null,
    val image: String? = null,
    @SerialName("banner_image") val bannerImage: String? = null,
    val authors: List<JsonAuthorDto> = emptyList(),
    val tags: List<String> = emptyList(),
)

@Serializable
private data class JsonAuthorDto(
    val name: String? = null,
)
