package io.github.iamoutofmana.doomscroll.domain.time

/** Relative timestamps for cards ("3h ago"). Pure so the formatting is testable. */
object TimeText {
    private const val MINUTE = 60_000L
    private const val HOUR = 3_600_000L
    private const val DAY = 86_400_000L

    fun relative(
        timestamp: Long?,
        now: Long = System.currentTimeMillis(),
    ): String {
        if (timestamp == null) return ""
        val diff = now - timestamp
        if (diff < MINUTE) return "now"
        if (diff < HOUR) return "${diff / MINUTE}m ago"
        if (diff < DAY) return "${diff / HOUR}h ago"
        val days = diff / DAY
        if (days < 7) return "${days}d ago"
        if (days < 30) return "${days / 7}w ago"
        if (days < 365) return "${days / 30}mo ago"
        return "${days / 365}y ago"
    }
}
