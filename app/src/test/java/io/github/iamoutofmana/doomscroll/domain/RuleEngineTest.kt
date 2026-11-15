package io.github.iamoutofmana.doomscroll.domain

import io.github.iamoutofmana.doomscroll.domain.filter.Rule
import io.github.iamoutofmana.doomscroll.domain.filter.RuleEngine
import io.github.iamoutofmana.doomscroll.domain.filter.RuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleEngineTest {
    private val keyword = Rule(id = 1, type = RuleType.KEYWORD, pattern = "bitcoin", enabled = true)
    private val domain = Rule(id = 2, type = RuleType.DOMAIN, pattern = "dailymail.co.uk", enabled = true)

    @Test
    fun keywordMatchesTitleCaseInsensitively() {
        val match = RuleEngine.isBlocked(
            title = "Bitcoin surges past new high",
            link = "https://example.com/a",
            publisher = "Example",
            rules = listOf(keyword),
        )
        assertEquals(keyword, match)
    }

    @Test
    fun keywordIgnoresUnrelatedStories() {
        assertNull(
            RuleEngine.isBlocked(
                title = "Local bakery wins award",
                link = "https://example.com/a",
                publisher = "Example",
                rules = listOf(keyword),
            ),
        )
    }

    @Test
    fun domainMatchesPublisherUrl() {
        val match = RuleEngine.isBlocked(
            title = "Some story",
            link = "https://www.dailymail.co.uk/news/article-123",
            publisher = null,
            rules = listOf(domain),
        )
        assertEquals(domain, match)
    }

    @Test
    fun domainMatchesPublisherNameForGoogleNewsItems() {
        val match = RuleEngine.isBlocked(
            title = "Some story",
            link = "https://news.google.com/rss/articles/abc",
            publisher = "Daily Mail",
            rules = listOf(Rule(id = 3, type = RuleType.DOMAIN, pattern = "daily mail", enabled = true)),
        )
        assertEquals(3L, match?.id)
    }

    @Test
    fun disabledRulesAreIgnored() {
        val disabled = keyword.copy(enabled = false)
        assertNull(
            RuleEngine.isBlocked(
                title = "Bitcoin surges",
                link = "https://example.com/a",
                publisher = null,
                rules = listOf(disabled),
            ),
        )
    }

    @Test
    fun emptyRulesMatchNothing() {
        assertNull(RuleEngine.isBlocked("Anything", "https://example.com", null, emptyList()))
    }
}
