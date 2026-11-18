package io.github.iamoutofmana.doomscroll.data

import io.github.iamoutofmana.doomscroll.data.sync.SyncPacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPacingTest {
    @Test
    fun recognizesRateLimitFailures() {
        assertTrue(SyncPacing.isRateLimited("HTTP 429"))
        assertTrue(SyncPacing.isRateLimited("HTTP 429 Too Many Requests"))
        assertFalse(SyncPacing.isRateLimited("HTTP 500"))
        assertFalse(SyncPacing.isRateLimited(null))
    }

    @Test
    fun retryBackoffIncreasesAndThenHolds() {
        assertEquals(1_500L, SyncPacing.delayForRetry(0))
        assertEquals(4_000L, SyncPacing.delayForRetry(1))
        assertEquals(4_000L, SyncPacing.delayForRetry(9))
    }

    @Test
    fun spacingIsSmallButNonZero() {
        assertTrue(SyncPacing.GN_SPACING_MS in 1..2_000)
    }
}
