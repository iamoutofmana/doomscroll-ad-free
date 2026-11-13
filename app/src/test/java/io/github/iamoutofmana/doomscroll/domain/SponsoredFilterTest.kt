package io.github.iamoutofmana.doomscroll.domain

import io.github.iamoutofmana.doomscroll.domain.filter.SponsoredFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SponsoredFilterTest {
    @Test
    fun flagsSponsored() {
        assertTrue(SponsoredFilter.isSponsored("The 10 best mattresses — Sponsored"))
    }

    @Test
    fun flagsPaidContent() {
        assertTrue(SponsoredFilter.isSponsored("Paid Content: Inside the new factory"))
    }

    @Test
    fun flagsPromoted() {
        assertTrue(SponsoredFilter.isSponsored("5 gadgets we love [promoted]"))
    }

    @Test
    fun flagsHashtagAd() {
        assertTrue(SponsoredFilter.isSponsored("Our favorite travel gear #ad"))
    }

    @Test
    fun doesNotFlagLegitNewsAboutAds() {
        // Conservative by design: "ad" as a word in real headlines must survive (plan §7).
        assertFalse(SponsoredFilter.isSponsored("Nike drops a bold new ad campaign"))
    }

    @Test
    fun doesNotFlagPresentedByYet() {
        // Deliberately NOT in the Phase 1 pattern set — too many false positives.
        assertFalse(SponsoredFilter.isSponsored("Presented by Verizon: CES roundup"))
    }

    @Test
    fun flagsSponsorCategory() {
        assertTrue(SponsoredFilter.isSponsored("Nice article", listOf("Sponsored")))
    }

    @Test
    fun normalCategoriesPass() {
        assertFalse(SponsoredFilter.isSponsored("Nice article", listOf("News", "Tech")))
    }
}
