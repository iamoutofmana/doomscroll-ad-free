package io.github.iamoutofmana.doomscroll.data.repo

import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.data.network.ArticleTextExtractor
import io.github.iamoutofmana.doomscroll.data.network.GnRedirectResolver
import io.github.iamoutofmana.doomscroll.data.network.HttpBodies
import io.github.iamoutofmana.doomscroll.data.network.ReaderBlock
import io.github.iamoutofmana.doomscroll.domain.AppMetadata
import io.github.iamoutofmana.doomscroll.domain.links.UrlSafety
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Reader mode (Phase 2.1): resolves the publisher URL, extracts structured article blocks
 * (headings, paragraphs, bullets, images), caches them as JSON in the local DB, and hands
 * them to the UI. No WebView, no ads.
 */
class ReaderService(
    private val client: OkHttpClient,
    private val resolver: GnRedirectResolver,
    private val articles: ArticleRepository,
) {
    sealed interface Result {
        data class Content(
            val article: ArticleEntity,
            val title: String?,
            val blocks: List<ReaderBlock>,
        ) : Result

        data class Failed(
            val article: ArticleEntity?,
            val message: String,
        ) : Result
    }

    suspend fun load(articleId: Long): Result {
        val article = articles.byId(articleId) ?: return Result.Failed(null, "Article not found")

        val cached = article.readerText
        if (!cached.isNullOrBlank()) {
            val blocks = decodeBlocks(cached)
            if (!blocks.isNullOrEmpty()) {
                return Result.Content(article, article.title, blocks)
            }
        }

        val target = resolveTarget(article)
            ?: return Result.Failed(article, "Couldn't resolve this article's link")
        if (!UrlSafety.isSafeArticleUrl(target)) {
            return Result.Failed(article, "Blocked an unsafe article link")
        }

        return try {
            val html = withContext(Dispatchers.IO) { fetchHtml(target) }
                ?: return Result.Failed(article, "Couldn't download the page")

            val extracted = ArticleTextExtractor.extract(html, target)
            if (extracted == null) {
                val paywalled = PAYWALL_MARKERS.any { marker -> html.contains(marker, ignoreCase = true) }
                return Result.Failed(
                    article,
                    if (paywalled) {
                        "This article looks like it's behind a paywall — open the original to read it there."
                    } else {
                        "Couldn't find readable text on this page"
                    },
                )
            }

            val blocks = extracted.blocks
            articles.saveReaderText(
                article.id,
                JSON.encodeToString(blocks),
            )
            Result.Content(article, extracted.title ?: article.title, blocks)
        } catch (e: Exception) {
            Result.Failed(article, e.message ?: "Extraction failed")
        }
    }

    private suspend fun resolveTarget(article: ArticleEntity): String? {
        if (!article.resolvedLink.isNullOrBlank()) return article.resolvedLink
        if (article.link.contains("news.google.com")) {
            val resolved = resolver.resolve(article.link) ?: return null
            articles.setResolvedLink(article.id, resolved)
            return resolved
        }
        return article.link
    }

    private fun fetchHtml(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", AppMetadata.BROWSER_UA)
            .header("Accept", "text/html,application/xhtml+xml")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            HttpBodies.readCapped(body, MAX_ARTICLE_BYTES)?.let { String(it) }
        }
    }

    private fun decodeBlocks(raw: String): List<ReaderBlock>? {
        if (!raw.trimStart().startsWith("[")) return null
        return runCatching {
            JSON.decodeFromString<List<ReaderBlock>>(raw)
        }.getOrNull()
    }

    private companion object {
        const val MAX_ARTICLE_BYTES = 2 * 1024 * 1024
        val JSON = Json { ignoreUnknownKeys = true }

        val PAYWALL_MARKERS = listOf(
            "subscribe to continue",
            "subscribe to read",
            "already a subscriber",
            "sign in to continue",
            "create a free account",
            "only available to subscribers",
            "for subscribers only",
        )
    }
}
