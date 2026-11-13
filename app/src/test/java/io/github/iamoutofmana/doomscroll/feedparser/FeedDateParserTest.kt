package io.github.iamoutofmana.doomscroll.feedparser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class FeedDateParserTest {
    @Test
    fun parsesRfc822WithDayName() {
        assertEquals(
            Instant.parse("2026-11-13T09:30:00Z").toEpochMilli(),
            FeedDateParser.parse("Fri, 13 Nov 2026 09:30:00 GMT"),
        )
    }

    @Test
    fun parsesRfc822WithoutDayName() {
        assertEquals(
            Instant.parse("2026-11-13T09:30:00Z").toEpochMilli(),
            FeedDateParser.parse("13 Nov 2026 09:30:00 GMT"),
        )
    }

    @Test
    fun parsesRfc822WithNumericOffset() {
        assertEquals(
            Instant.parse("2026-11-12T09:00:00Z").toEpochMilli(),
            FeedDateParser.parse("Thu, 12 Nov 2026 10:00:00 +0100"),
        )
    }

    @Test
    fun parsesRfc822WithNamedZone() {
        assertEquals(
            Instant.parse("2026-11-12T15:00:00Z").toEpochMilli(),
            FeedDateParser.parse("Thu, 12 Nov 2026 10:00:00 EST"),
        )
    }

    @Test
    fun parsesRfc822WithoutSeconds() {
        assertEquals(
            Instant.parse("2026-11-12T10:00:00Z").toEpochMilli(),
            FeedDateParser.parse("12 Nov 2026 10:00 +0000"),
        )
    }

    @Test
    fun parsesIsoWithZ() {
        assertEquals(
            Instant.parse("2026-11-12T10:00:00Z").toEpochMilli(),
            FeedDateParser.parse("2026-11-12T10:00:00Z"),
        )
    }

    @Test
    fun parsesIsoWithOffset() {
        assertEquals(
            Instant.parse("2026-11-12T08:00:00Z").toEpochMilli(),
            FeedDateParser.parse("2026-11-12T10:00:00+02:00"),
        )
    }

    @Test
    fun parsesIsoWithFractionalSeconds() {
        assertEquals(
            Instant.parse("2026-11-12T10:00:00.123Z").toEpochMilli(),
            FeedDateParser.parse("2026-11-12T10:00:00.123Z"),
        )
    }

    @Test
    fun parsesDateOnly() {
        assertEquals(
            Instant.parse("2026-11-12T00:00:00Z").toEpochMilli(),
            FeedDateParser.parse("2026-11-12"),
        )
    }

    @Test
    fun garbageReturnsNull() {
        assertNull(FeedDateParser.parse("not a date"))
        assertNull(FeedDateParser.parse(null))
        assertNull(FeedDateParser.parse("   "))
    }
}
