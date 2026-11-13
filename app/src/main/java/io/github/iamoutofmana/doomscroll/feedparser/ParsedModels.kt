package io.github.iamoutofmana.doomscroll.feedparser

/** Normalized output of any supported feed format. */
data class ParsedFeed(
    val title: String?,
    val siteUrl: String?,
    val description: String?,
    val items: List<ParsedItem>,
)

data class ParsedItem(
    val guid: String,
    val title: String,
    val link: String?,
    val author: String?,
    val publishedAt: Long?,
    val summaryText: String?,
    val imageUrl: String?,
    val audioUrl: String?,
    val videoUrl: String?,
    /** RSS `<source>` element — Google News sets it to the publisher name. */
    val publisher: String?,
    val categories: List<String>,
)
