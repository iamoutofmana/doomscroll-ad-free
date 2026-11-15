package io.github.iamoutofmana.doomscroll.data.repo

import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryClustererTest {
    private fun article(
        id: Long,
        title: String,
        publishedAt: Long,
    ) = ArticleEntity(
        id = id,
        sourceId = 1,
        guid = "guid-$id",
        title = title,
        link = "https://example.com/$id",
        fetchedAt = publishedAt,
        publishedAt = publishedAt,
    )

    private val now = 1_700_000_000_000L

    @Test
    fun groupsNearDuplicateHeadlines() {
        val primary = article(1, "FAA ground stop shuts down flights nationwide", now)
        val variant = article(2, "Live updates: FAA declares rare nationwide ground stop of flights", now - 60_000)
        val other = article(3, "Apple announces new MacBook Pro", now - 120_000)

        val groups = StoryClusterer.cluster(listOf(primary, variant, other))

        assertEquals(2, groups.size)
        val faaGroup = groups.first { it.primary.id == 1L }
        assertEquals(1, faaGroup.related.size)
        assertEquals(2L, faaGroup.related[0].id)
    }

    @Test
    fun keepsUnrelatedStoriesSeparate() {
        val a = article(1, "Google announces Pixel 10 with new camera", now)
        val b = article(2, "Liverpool wins dramatic derby in stoppage time", now)

        val groups = StoryClusterer.cluster(listOf(a, b))

        assertEquals(2, groups.size)
        assertTrue(groups.all { it.related.isEmpty() })
    }

    @Test
    fun sameHeadlineOutsideWindowStaysSeparate() {
        val title = "SpaceX launches Starship on successful test flight"
        val a = article(1, title, now)
        val b = article(2, title, now - 3L * 24L * 60L * 60L * 1000L)

        val groups = StoryClusterer.cluster(listOf(a, b))

        assertEquals(2, groups.size)
    }

    @Test
    fun shortGenericTitlesAreNotGrouped() {
        val a = article(1, "Big news today", now)
        val b = article(2, "Big news now", now - 1_000)

        val groups = StoryClusterer.cluster(listOf(a, b))

        // After stopword removal both titles have < 3 significant tokens: never grouped.
        assertEquals(2, groups.size)
    }

    @Test
    fun newestArticleBecomesPrimary() {
        val older = article(1, "Hurricane watch issued for the coast", now - 3_600_000)
        val newer = article(2, "Hurricane watch issued along the coast", now)

        val groups = StoryClusterer.cluster(listOf(older, newer))

        assertEquals(1, groups.size)
        assertEquals(2L, groups[0].primary.id)
        assertEquals(1, groups[0].related.size)
    }
}
