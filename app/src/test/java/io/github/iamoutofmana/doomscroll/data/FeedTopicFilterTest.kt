package io.github.iamoutofmana.doomscroll.data

import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.data.repo.StoryClusterer
import io.github.iamoutofmana.doomscroll.data.repo.filterByTopic
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedTopicFilterTest {
    private fun article(
        id: Long,
        sourceId: Long,
    ) = ArticleEntity(
        id = id,
        sourceId = sourceId,
        guid = "guid-$id",
        title = "title $id",
        link = "https://example.com/$id",
        fetchedAt = 0L,
    )

    private val aiGroup = StoryClusterer.StoryGroup(article(1, 10), listOf(article(2, 11)))
    private val foodGroup = StoryClusterer.StoryGroup(article(3, 20), emptyList())
    private val groups = listOf(aiGroup, foodGroup)
    private val topics = mapOf(10L to "ai", 11L to "ai", 20L to "food")

    @Test
    fun nullTopicKeepsEverything() {
        assertEquals(groups, groups.filterByTopic(topics, null))
    }

    @Test
    fun keepsGroupsWhosePrimaryMatches() {
        assertEquals(listOf(foodGroup), groups.filterByTopic(topics, "food"))
    }

    @Test
    fun keepsGroupsWhoseRelatedCoverageMatches() {
        val relatedOnly = StoryClusterer.StoryGroup(primary = article(4, 99), related = listOf(article(5, 10)))
        assertEquals(
            listOf(relatedOnly),
            listOf(relatedOnly).filterByTopic(topics, "ai"),
        )
    }

    @Test
    fun excludesGroupsOutsideTheTopic() {
        assertEquals(emptyList<StoryClusterer.StoryGroup>(), groups.filterByTopic(topics, "business"))
    }
}
