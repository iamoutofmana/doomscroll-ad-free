package io.github.iamoutofmana.doomscroll.domain.googlenews

import java.net.URLEncoder

/**
 * Builds public Google News RSS feed URLs — the sanctioned Discover-style content source
 * (Google Discover has no API; see docs/PLAN.md §2).
 *
 * Locale params must agree: `hl` (language), `gl` (country), `ceid` (country:language edition).
 */
object GoogleNews {
    data class GnLocale(
        val hl: String,
        val gl: String,
        val ceid: String,
        val display: String,
    )

    data class Topic(val code: String, val display: String)

    val LOCALES = listOf(
        GnLocale("en-US", "US", "US:en", "US"),
        GnLocale("en-GB", "GB", "GB:en", "UK"),
        GnLocale("en-CA", "CA", "CA:en", "Canada"),
        GnLocale("en-AU", "AU", "AU:en", "Australia"),
        GnLocale("en-IN", "IN", "IN:en", "India"),
        GnLocale("de-DE", "DE", "DE:de", "Deutschland"),
        GnLocale("fr-FR", "FR", "FR:fr", "France"),
        GnLocale("es-419", "MX", "MX:es-419", "México"),
    )

    val DEFAULT_LOCALE = LOCALES.first()

    val TOPICS = listOf(
        Topic("", "Top stories"),
        Topic("WORLD", "World"),
        Topic("NATION", "Nation"),
        Topic("BUSINESS", "Business"),
        Topic("TECHNOLOGY", "Technology"),
        Topic("ENTERTAINMENT", "Entertainment"),
        Topic("SPORTS", "Sports"),
        Topic("SCIENCE", "Science"),
        Topic("HEALTH", "Health"),
    )

    fun topicUrl(topic: Topic, locale: GnLocale = DEFAULT_LOCALE): String =
        if (topic.code.isEmpty()) {
            "https://news.google.com/rss?${params(locale)}"
        } else {
            "https://news.google.com/rss/headlines/section/topic/${topic.code}?${params(locale)}"
        }

    /** Supports operators: intitle:, site:, OR, when:1d|7d|1y, quoted phrases. */
    fun searchUrl(query: String, locale: GnLocale = DEFAULT_LOCALE): String =
        "https://news.google.com/rss/search?q=${encode(query)}&${params(locale)}"

    fun geoUrl(city: String, locale: GnLocale = DEFAULT_LOCALE): String =
        "https://news.google.com/rss/headlines/section/geo/${encode(city)}?${params(locale)}"

    fun isGoogleNewsUrl(url: String): Boolean = url.contains("news.google.com")

    /**
     * Google News item titles look like "Headline - Publisher".
     * Splits at the last " - " separator; returns the raw title unchanged when absent.
     */
    fun splitTitle(rawTitle: String): Pair<String, String?> {
        val idx = rawTitle.lastIndexOf(" - ")
        if (idx <= 0) return rawTitle to null
        val headline = rawTitle.substring(0, idx).trim()
        val publisher = rawTitle.substring(idx + 3).trim()
        if (headline.isEmpty() || publisher.isEmpty()) return rawTitle to null
        return headline to publisher
    }

    private fun params(locale: GnLocale): String = "hl=${locale.hl}&gl=${locale.gl}&ceid=${locale.ceid}"

    private fun encode(s: String): String = URLEncoder.encode(s, "UTF-8")
}
