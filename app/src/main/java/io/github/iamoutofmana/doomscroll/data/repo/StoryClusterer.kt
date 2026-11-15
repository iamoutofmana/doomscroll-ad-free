package io.github.iamoutofmana.doomscroll.data.repo

import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.domain.links.TitleNormalizer
import kotlin.math.abs

/**
 * Groups near-duplicate headlines ("same story, different outlet") into one display unit
 * (plan §7 layer 4, user feedback #4). The newest article becomes the group's primary;
 * the rest are listed as "Also covered by" under it.
 *
 * Exact-title duplicates are already dropped at ingest (SyncEngine); this catches the
 * reworded variants — e.g. "FAA ground stop shuts down flights nationwide" vs
 * "Live updates: FAA declares rare nationwide ground stop of flights".
 */
object StoryClusterer {
    data class StoryGroup(
        val primary: ArticleEntity,
        val related: List<ArticleEntity>,
    )

    private const val SIMILARITY_THRESHOLD = 0.5
    private const val MIN_TOKENS = 3
    private const val WINDOW_MS = 48L * 60L * 60L * 1000L // 48 hours

    private val STOPWORDS = setOf(
        "the", "a", "an", "and", "or", "but", "of", "to", "in", "on", "for", "with", "at",
        "by", "from", "as", "is", "are", "was", "were", "be", "been", "has", "have", "had",
        "will", "would", "could", "should", "this", "that", "these", "those", "it", "its",
        "his", "her", "their", "your", "our", "my", "after", "before", "over", "under",
        "about", "into", "than", "then", "so", "up", "down", "out", "off", "more", "most",
        "new", "news", "live", "updates", "update", "report", "reports", "says", "said",
        "amid", "via", "how", "why", "what", "when", "where", "who", "just", "now", "day",
    )

    fun tokens(title: String): Set<String> = TitleNormalizer.normalize(title)
        .split(' ')
        .filter { it.length > 2 && it !in STOPWORDS }
        .toSet()

    fun similarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.count { it in b }
        val union = a.size + b.size - intersection
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    fun cluster(articles: List<ArticleEntity>): List<StoryGroup> {
        val buckets = mutableListOf<Pair<ArticleEntity, MutableList<ArticleEntity>>>()
        val tokenCache = HashMap<Long, Set<String>>()

        fun tokensOf(article: ArticleEntity): Set<String> =
            tokenCache.getOrPut(article.id) { tokens(article.title) }

        val sorted = articles.sortedByDescending { it.publishedAt ?: it.fetchedAt }
        for (article in sorted) {
            val articleTokens = tokensOf(article)
            val match = buckets.firstOrNull { (primary, _) ->
                val primaryTokens = tokensOf(primary)
                primaryTokens.size >= MIN_TOKENS &&
                    articleTokens.size >= MIN_TOKENS &&
                    similarity(primaryTokens, articleTokens) >= SIMILARITY_THRESHOLD &&
                    abs(
                        (primary.publishedAt ?: primary.fetchedAt) - (article.publishedAt ?: article.fetchedAt),
                    ) <= WINDOW_MS
            }
            if (match != null) {
                match.second.add(article)
            } else {
                buckets.add(article to mutableListOf())
            }
        }
        return buckets.map { (primary, related) -> StoryGroup(primary, related) }
    }
}
