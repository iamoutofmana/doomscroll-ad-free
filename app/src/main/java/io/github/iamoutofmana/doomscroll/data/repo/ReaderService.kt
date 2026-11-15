package io.github.iamoutofmana.doomscroll.data.repo

import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.data.network.ArticleTextExtractor
import io.github.iamoutofmana.doomscroll.data.network.GnRedirectResolver
import io.github.iamoutofmana.doomscroll.data.network.HttpBodies
import io.github.iamoutofmana.doomscroll.domain.AppMetadata
import io.github.iamoutofmana.doomscroll.domain.links.UrlSafety
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Reader mode (Phase 2): resolves the publisher URL, extracts readable article text,
 * caches it in the local DB, and hands paragraphs to the UI. No WebView, no ads.
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
            val paragraphs: List<String>,
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
            return Result.Content(article, article.title, cached.split("\n\n").filter { it.isNotBlank() })
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
                ?: return Result.Failed(article, "Couldn't find readable text on this page")
            val text = extracted.paragraphs.joinToString("\n\n")
            articles.saveReaderText(article.id, text)
            Result.Content(article, extracted.title ?: article.title, extracted.paragraphs)
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

    private companion object {
        const val MAX_ARTICLE_BYTES = 2 * 1024 * 1024
    }
}
