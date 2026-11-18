package io.github.iamoutofmana.doomscroll.domain.packs

import io.github.iamoutofmana.doomscroll.domain.googlenews.GoogleNews
import io.github.iamoutofmana.doomscroll.domain.model.NewSource
import io.github.iamoutofmana.doomscroll.domain.model.SourceType
import io.github.iamoutofmana.doomscroll.domain.popularsources.PopularSources

/**
 * Curated topic packs (v1.2): one-tap bundles of feeds that are known to work today.
 *
 * Prefer Google News searches for anything that can rot — publisher RSS endpoints change,
 * but `site:` and keyword searches keep working. Packs use the US edition, matching the
 * local (Salt Lake City) pack.
 */
data class TopicPack(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val description: String,
    /** Compact label for feed filter chips. */
    val chipLabel: String,
    val sources: List<NewSource>,
)

object TopicPacks {
    private val locale = GoogleNews.DEFAULT_LOCALE

    private fun rss(
        title: String,
        url: String,
    ) = NewSource(SourceType.RSS, url, title)

    private fun search(
        query: String,
        title: String,
    ) = NewSource(SourceType.GN_SEARCH, GoogleNews.searchUrl(query, locale), title)

    private fun topic(
        code: String,
        display: String,
    ) = NewSource(SourceType.GN_TOPIC, GoogleNews.topicUrl(GoogleNews.Topic(code, display), locale), display)

    private fun geo(city: String) = NewSource(SourceType.GN_TOPIC, GoogleNews.geoUrl(city, locale), "$city (local)")

    private fun reddit(subreddit: String) =
        NewSource(SourceType.REDDIT, PopularSources.redditSubredditUrl(subreddit), "r/$subreddit")

    val ALL: List<TopicPack> = listOf(
        TopicPack(
            id = "ai",
            emoji = "🤖",
            title = "AI & machine learning",
            subtitle = "Labs, research, products",
            description = "Frontier labs, research feeds and tech-press coverage of artificial intelligence.",
            chipLabel = "AI",
            sources = listOf(
                rss("The Verge — AI", "https://www.theverge.com/rss/ai-artificial-intelligence/index.xml"),
                rss("MIT Technology Review", "https://www.technologyreview.com/feed/"),
                rss("OpenAI", "https://openai.com/news/rss.xml"),
                rss("Google DeepMind", "https://deepmind.google/blog/rss.xml"),
                rss("Hugging Face", "https://huggingface.co/blog/feed.xml"),
                rss("arXiv cs.AI", "https://export.arxiv.org/rss/cs.AI"),
                search("\"artificial intelligence\"", "AI — Google News"),
            ),
        ),
        TopicPack(
            id = "construction",
            emoji = "🏗️",
            title = "Construction & built environment",
            subtitle = "Projects, trades, policy",
            description = "Trade press, architecture and engineering news, plus practitioner discussion.",
            chipLabel = "Construction",
            sources = listOf(
                rss("Construction Dive", "https://www.constructiondive.com/feeds/news/"),
                rss("ArchDaily", "https://www.archdaily.com/rss"),
                search("\"construction industry\"", "Construction industry — Google News"),
                search("site:enr.com", "ENR — Google News"),
                reddit("Construction"),
            ),
        ),
        TopicPack(
            id = "salt-lake-city",
            emoji = "🏔️",
            title = "Salt Lake City metro",
            subtitle = "SLC, Davis County, Utah",
            description = "Local coverage from the Salt Lake Valley, Davis County and across Utah.",
            chipLabel = "Salt Lake City",
            sources = listOf(
                geo("Salt Lake City"),
                rss("Deseret News", "https://www.deseret.com/arc/outboundfeeds/rss/"),
                search("site:ksl.com", "KSL — Google News"),
                search("site:sltrib.com", "Salt Lake Tribune — Google News"),
                search("site:standard.net", "Standard-Examiner — Google News"),
            ),
        ),
        TopicPack(
            id = "food",
            emoji = "🍜",
            title = "Food & dining",
            subtitle = "Cooking, restaurants, industry",
            description = "Food media, restaurant business reporting and cooking coverage.",
            chipLabel = "Food",
            sources = listOf(
                rss("Eater", "https://www.eater.com/rss/index.xml"),
                rss("Bon Appétit", "https://www.bonappetit.com/feed/rss"),
                search("\"restaurant industry\"", "Restaurant industry — Google News"),
                search("food", "Food — Google News"),
            ),
        ),
        TopicPack(
            id = "business",
            emoji = "📈",
            title = "Business & markets",
            subtitle = "Companies, markets, small business",
            description = "Market coverage, company news and small-business reporting.",
            chipLabel = "Business",
            sources = listOf(
                topic("BUSINESS", "Business — Google News"),
                rss("CNBC", "https://www.cnbc.com/id/100003114/device/rss/rss.html"),
                rss("Fortune", "https://fortune.com/feed/"),
                rss("Fast Company", "https://www.fastcompany.com/rss"),
                search("\"small business\"", "Small business — Google News"),
            ),
        ),
    )
}
