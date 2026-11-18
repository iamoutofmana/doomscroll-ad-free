package io.github.iamoutofmana.doomscroll.data.sync

/**
 * Politeness settings for the sync loop (v1.3): Google News rate-limits bursts of RSS
 * requests, so space them out and retry a 429 a couple of times with backoff before giving
 * up for this sync. Other sources are unaffected.
 */
internal object SyncPacing {
    const val GN_SPACING_MS = 350L
    val RETRY_DELAYS_MS = longArrayOf(1_500L, 4_000L)

    fun isRateLimited(message: String?): Boolean = message?.contains("429") == true

    fun delayForRetry(attempt: Int): Long =
        RETRY_DELAYS_MS[attempt.coerceIn(0, RETRY_DELAYS_MS.lastIndex)]
}
