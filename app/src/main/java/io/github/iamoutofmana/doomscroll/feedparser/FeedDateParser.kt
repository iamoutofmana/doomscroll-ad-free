package io.github.iamoutofmana.doomscroll.feedparser

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Lenient date parsing for the mess of formats real feeds emit:
 * RFC-822/1123 variants (RSS) and ISO-8601 variants (Atom/JSON Feed).
 * Returns null on failure — callers fall back to fetch-time ordering.
 */
object FeedDateParser {
    private val ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val RFC822 = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm[:ss] X", Locale.ENGLISH)
    private val DOW_PREFIX = Regex("^[A-Za-z]{3}\\s*,?\\s+")

    // Obsolete named zones -> numeric offsets so one "X" pattern covers everything.
    private val ZONE_FIXES = mapOf(
        "GMT" to "+0000",
        "UTC" to "+0000",
        "UT" to "+0000",
        "Z" to "+0000",
        "EST" to "-0500",
        "EDT" to "-0400",
        "CST" to "-0600",
        "CDT" to "-0500",
        "MST" to "-0700",
        "MDT" to "-0600",
        "PST" to "-0800",
        "PDT" to "-0700",
    )

    fun parse(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim()

        // ISO-8601 family (Atom, JSON Feed): ...Z, +02:00, fractional seconds, date-only.
        runCatching { return Instant.parse(s).toEpochMilli() }
        runCatching { return OffsetDateTime.parse(s, ISO_OFFSET).toInstant().toEpochMilli() }
        runCatching { return LocalDateTime.parse(s, ISO_LOCAL).toInstant(ZoneOffset.UTC).toEpochMilli() }
        runCatching { return LocalDate.parse(s).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }

        // RFC-822 family (RSS): "Fri, 13 Nov 2026 09:30:00 GMT", "13 Nov 2026 08:00 +0100", ...
        runCatching { return rfc822(s) }
        return null
    }

    private fun rfc822(input: String): Long {
        var s = input
        if (DOW_PREFIX.containsMatchIn(s)) {
            s = DOW_PREFIX.replaceFirst(s, "")
        }
        for ((name, offset) in ZONE_FIXES) {
            if (s.endsWith(" $name")) {
                s = s.substring(0, s.length - name.length) + offset
                break
            }
        }
        return OffsetDateTime.parse(s, RFC822).toInstant().toEpochMilli()
    }
}
