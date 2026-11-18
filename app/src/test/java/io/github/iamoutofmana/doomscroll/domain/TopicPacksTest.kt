package io.github.iamoutofmana.doomscroll.domain

import io.github.iamoutofmana.doomscroll.domain.model.SourceType
import io.github.iamoutofmana.doomscroll.domain.packs.TopicPacks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicPacksTest {
    @Test
    fun packsHaveUniqueIdsAndHttpsSources() {
        val packs = TopicPacks.ALL
        assertEquals(packs.size, packs.map { it.id }.toSet().size)
        assertTrue(packs.size >= 5)
        packs.forEach { pack ->
            assertTrue(pack.id.isNotBlank())
            assertTrue(pack.title.isNotBlank())
            assertTrue(pack.sources.isNotEmpty())
            pack.sources.forEach { source ->
                assertTrue("${pack.id}: ${source.title}", source.title.isNotBlank())
                assertTrue("${pack.id}: ${source.url}", source.url.startsWith("https://"))
            }
        }
    }

    @Test
    fun requestedPacksExist() {
        val ids = TopicPacks.ALL.map { it.id }.toSet()
        assertTrue(ids.containsAll(setOf("ai", "construction", "salt-lake-city", "food", "business")))
    }

    @Test
    fun googleNewsSearchesUseTheUsEdition() {
        val searches = TopicPacks.ALL.flatMap { pack ->
            pack.sources.filter { it.type == SourceType.GN_SEARCH }
        }
        assertTrue(searches.isNotEmpty())
        searches.forEach { source ->
            assertTrue(source.url.startsWith("https://news.google.com/rss/search?q="))
            assertTrue(source.url.contains("hl=en-US"))
            assertTrue(source.url.contains("gl=US"))
            assertTrue(source.url.contains("ceid=US:en"))
        }
    }

    @Test
    fun saltLakeCityPackCoversLocalPublishers() {
        val pack = TopicPacks.ALL.first { it.id == "salt-lake-city" }
        val urls = pack.sources.joinToString(" ") { it.url }
        assertTrue(urls.contains("news.google.com/rss/headlines/section/geo/Salt"))
        assertTrue(urls.contains("deseret.com"))
        assertTrue(urls.contains("site%3Aksl.com"))
        assertTrue(urls.contains("site%3Asltrib.com"))
    }

    @Test
    fun constructionPackIncludesRedditAndTradePress() {
        val pack = TopicPacks.ALL.first { it.id == "construction" }
        assertTrue(
            pack.sources.any {
                it.type == SourceType.REDDIT && it.url == "https://www.reddit.com/r/Construction/.rss"
            },
        )
        assertTrue(pack.sources.any { it.url.contains("constructiondive.com") })
        assertTrue(pack.sources.any { it.url.contains("archdaily.com") })
    }

    @Test
    fun businessPackUsesTheBusinessTopic() {
        val pack = TopicPacks.ALL.first { it.id == "business" }
        assertTrue(
            pack.sources.any {
                it.type == SourceType.GN_TOPIC && it.url.contains("/topic/BUSINESS")
            },
        )
    }
}
