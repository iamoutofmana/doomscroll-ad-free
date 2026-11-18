package io.github.iamoutofmana.doomscroll.domain

import io.github.iamoutofmana.doomscroll.domain.model.SourceType
import io.github.iamoutofmana.doomscroll.domain.packs.TopicPacks
import io.github.iamoutofmana.doomscroll.domain.topics.SourceTopics
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceTopicsTest {
    private val aiPack = TopicPacks.ALL.first { it.id == "ai" }
    private val aiUrl = aiPack.sources.first { it.type == SourceType.RSS }.url
    private val customUrl = "https://example.com/custom-feed.xml"

    @Test
    fun mapsPackSourcesToTheirTopic() {
        assertEquals("ai", SourceTopics.packIdForUrl(aiUrl))
        assertEquals("construction", SourceTopics.topicOf(TopicPacks.ALL.first { it.id == "construction" }.sources.first().url))
    }

    @Test
    fun unknownSourcesFallIntoOther() {
        assertEquals(null, SourceTopics.packIdForUrl(customUrl))
        assertEquals(SourceTopics.OTHER_ID, SourceTopics.topicOf(customUrl))
    }

    @Test
    fun chipsOnlyIncludeTopicsWithSourcesAndOtherWhenNeeded() {
        val chips = SourceTopics.chipsFor(listOf(aiUrl, customUrl))

        assertEquals(listOf("ai", SourceTopics.OTHER_ID), chips.map { it.id })
        assertEquals("AI", chips.first().label)
        assertEquals("Other", chips.last().label)
    }

    @Test
    fun noOtherChipWhenEverySourceIsMapped() {
        val urls = aiPack.sources.map { it.url }
        val chips = SourceTopics.chipsFor(urls)

        assertEquals(listOf("ai"), chips.map { it.id })
    }
}
