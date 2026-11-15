package io.github.iamoutofmana.doomscroll.data.network

import io.github.iamoutofmana.doomscroll.domain.AppMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches a publisher page and extracts preview metadata (summary + hero image).
 * Google News links are resolved first so we scrape the publisher, not Google's JS shell.
 * Best-effort: null on any failure.
 */
class ArticleEnricher(
    private val client: OkHttpClient,
    private val resolver: GnRedirectResolver,
) {
    data class Enriched(
        val description: String?,
        val imageUrl: String?,
        val resolvedUrl: String?,
    )

    suspend fun fetch(
        articleLink: String,
        knownResolved: String?,
    ): Enriched? = withContext(Dispatchers.IO) {
        try {
            val target = resolveTarget(articleLink, knownResolved) ?: return@withContext null
            val request = Request.Builder()
                .url(target)
                .header("User-Agent", AppMetadata.BROWSER_UA)
                .header("Accept", "text/html,application/xhtml+xml")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val finalUrl = response.request.url.toString()
                val preview = PreviewParser.parse(body, finalUrl)
                Enriched(
                    description = preview.description,
                    imageUrl = preview.imageUrl,
                    resolvedUrl = knownResolved ?: finalUrl,
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun resolveTarget(
        articleLink: String,
        knownResolved: String?,
    ): String? {
        if (!knownResolved.isNullOrBlank()) return knownResolved
        if (articleLink.contains("news.google.com")) return resolver.resolve(articleLink)
        return articleLink
    }
}
