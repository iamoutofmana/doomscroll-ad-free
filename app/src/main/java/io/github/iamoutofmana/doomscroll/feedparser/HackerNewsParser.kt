package io.github.iamoutofmana.doomscroll.feedparser

import io.github.iamoutofmana.doomscroll.domain.links.HtmlText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Hacker News front-page parser (Algolia public JSON API).
 * Returns items with `url` (the article) and `story_id` (stable per-story identity).
 * Self-posts (Ask HN, Show HN with no external link) are skipped.
 */
object HackerNewsParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(text: String): ParsedFeed {
        val response = json.decodeFromString<HnSearchResponse>(text)
        val items = response.hits
            .filter { !it.url.isNullOrBlank() && !it.title.isNullOrBlank() }
            .map { hit ->
                ParsedItem(
                    guid = "hn-${hit.objectID ?: hit.url!!}",
                    title = hit.title!!,
                    link = hit.url,
                    author = hit.author,
                    publishedAt = FeedDateParser.parse(hit.createdAt),
                    summaryText = HtmlText.strip(hit.storyText.orEmpty()).ifBlank { null },
                    // HN feed has no images
                    imageUrl = null,
                    audioUrl = null,
                    videoUrl = null,
                    publisher = null,
                    categories = emptyList(),
                )
            }
        return ParsedFeed(
            title = "Hacker News",
            siteUrl = "https://news.ycombinator.com/",
            description = null,
            items = items,
        )
    }
}

@Serializable
private data class HnSearchResponse(
    val hits: List<HnHit> = emptyList(),
)

@Serializable
private data class HnHit(
    @SerialName("objectID") val objectID: String? = null,
    val title: String? = null,
    val url: String? = null,
    val author: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("story_text") val storyText: String? = null,
)
