package io.github.iamoutofmana.doomscroll.data.network

import io.github.iamoutofmana.doomscroll.domain.AppMetadata
import io.github.iamoutofmana.doomscroll.domain.links.UrlSafety
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI

/**
 * "Paste a link, get a feed" (v1.2): reads a site or article page, looks for
 * `<link rel="alternate">` RSS/Atom/JSON-Feed, then probes the usual feed paths.
 * Best-effort by design — no crawling beyond a handful of candidates.
 */
class FeedDiscovery(private val client: OkHttpClient) {
    sealed interface Result {
        data class Found(
            val feedUrl: String,
            val title: String?,
        ) : Result

        data class Failed(val reason: Reason) : Result
    }

    enum class Reason {
        INVALID_URL,
        UNSAFE,
        X_UNSUPPORTED,
        FETCH_FAILED,
        NO_FEED,
    }

    data class Discovered(
        val feedUrl: String,
        val pageTitle: String?,
    )

    suspend fun discover(input: String): Result = withContext(Dispatchers.IO) {
        val pageUrl = normalizeUrl(input) ?: return@withContext Result.Failed(Reason.INVALID_URL)
        if (!UrlSafety.isSafeArticleUrl(pageUrl)) return@withContext Result.Failed(Reason.UNSAFE)
        if (isXUrl(pageUrl)) return@withContext Result.Failed(Reason.X_UNSUPPORTED)

        val html = fetch(pageUrl, MAX_PAGE_BYTES, AppMetadata.BROWSER_UA) ?: return@withContext Result.Failed(Reason.FETCH_FAILED)
        val linked = fromHtml(html, pageUrl)
        if (linked != null) {
            validate(linked.feedUrl)?.let { feedTitle ->
                return@withContext Result.Found(linked.feedUrl, feedTitle ?: linked.pageTitle)
            }
        }

        for (candidate in candidatesFor(pageUrl)) {
            if (!UrlSafety.isSafeArticleUrl(candidate)) continue
            validate(candidate)?.let { feedTitle ->
                return@withContext Result.Found(candidate, feedTitle)
            }
        }
        Result.Failed(Reason.NO_FEED)
    }

    /** Returns the feed's own title when the URL is a valid feed, else null. */
    private fun validate(feedUrl: String): String? {
        val body = fetch(feedUrl, MAX_FEED_BYTES, AppMetadata.USER_AGENT)
            ?: fetch(feedUrl, MAX_FEED_BYTES, AppMetadata.BROWSER_UA)
            ?: return null
        return if (looksLikeFeed(body)) feedTitle(body) else null
    }

    private fun fetch(
        url: String,
        maxBytes: Int,
        userAgent: String,
    ): String? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header(
                "Accept",
                "application/rss+xml,application/atom+xml,application/feed+json,application/xml,text/xml,text/html,*/*",
            )
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            val finalUrl = response.request.url.toString()
            if (!UrlSafety.isSafeArticleUrl(finalUrl)) return@runCatching null
            val body = response.body ?: return@runCatching null
            HttpBodies.readCapped(body, maxBytes)?.let { String(it) }
        }
    }.getOrNull()

    companion object {
        private const val MAX_PAGE_BYTES = 1 * 1024 * 1024
        private const val MAX_FEED_BYTES = 2 * 1024 * 1024
        private const val MAX_TITLE = 120

        private val FEED_TYPES = listOf("rss", "atom", "feed+json", "json")

        /** Accepts bare hosts, upgrades http to https, rejects anything else. */
        fun normalizeUrl(input: String): String? {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return null
            val hasUnsupportedScheme = trimmed.contains("://") &&
                !trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true)
            if (hasUnsupportedScheme) return null
            val withScheme = when {
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
                trimmed.startsWith("http://", ignoreCase = true) ->
                    "https://" + trimmed.substring("http://".length)
                else -> "https://$trimmed"
            }
            val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
            if (uri.host.isNullOrBlank()) return null
            if (uri.scheme != "https") return null
            return withScheme
        }

        fun isXUrl(url: String): Boolean {
            val host = runCatching { URI(url).host }.getOrNull()?.lowercase() ?: return false
            return host == "x.com" || host.endsWith(".x.com") ||
                host == "twitter.com" || host.endsWith(".twitter.com")
        }

        /** Pure HTML inspection; tested without network. */
        fun fromHtml(
            html: String,
            baseUri: String,
        ): Discovered? {
            val doc = Jsoup.parse(html, baseUri)
            for (link in doc.select("link[href]")) {
                val rel = link.attr("rel").lowercase()
                if (!rel.contains("alternate")) continue
                val type = link.attr("type").lowercase()
                if (FEED_TYPES.none { type.contains(it) }) continue
                val absolute = link.absUrl("href")
                if (absolute.isBlank() || !absolute.startsWith("https://")) continue
                return Discovered(absolute, doc.title().trim().ifBlank { null }?.take(MAX_TITLE))
            }
            return null
        }

        /** Common feed locations, origin first, then the page's own directory. */
        fun candidatesFor(pageUrl: String): List<String> {
            val uri = runCatching { URI(pageUrl) }.getOrNull() ?: return emptyList()
            val origin = "${uri.scheme}://${uri.authority}"
            val result = mutableListOf(
                "$origin/feed",
                "$origin/rss",
                "$origin/rss.xml",
                "$origin/feed.xml",
                "$origin/atom.xml",
                "$origin/index.xml",
            )
            val path = uri.path.orEmpty().trimEnd('/')
            if (path.isNotEmpty() && path != "/") {
                result += "$origin$path/feed"
                result += "$origin$path/rss.xml"
            }
            return result.distinct()
        }

        fun looksLikeFeed(body: String): Boolean {
            val text = body.trimStart('\uFEFF', ' ', '\n', '\r', '\t').lowercase()
            val core = if (text.startsWith("<?xml")) text.substringAfter("?>", text).trimStart() else text
            if (core.startsWith("<rss") || core.startsWith("<feed") || core.startsWith("<rdf")) return true
            return core.startsWith("{") && core.contains("\"items\"") && core.contains("\"version\"")
        }

        fun feedTitle(body: String): String? {
            val match = Regex("(?is)<title[^>]*>(.*?)</title>").find(body)
            val raw = match?.groupValues?.get(1)?.trim()
            val title = if (raw != null) {
                Jsoup.parse(raw).text().trim()
            } else {
                Regex("\"title\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            }
            return title?.takeIf { it.isNotBlank() }?.take(MAX_TITLE)
        }
    }
}
