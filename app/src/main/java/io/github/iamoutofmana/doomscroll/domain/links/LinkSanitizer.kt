package io.github.iamoutofmana.doomscroll.domain.links

/**
 * Strips tracking parameters from outgoing links (plan §7, layer 2) and unwraps
 * AMP paths to their canonical article. Pure string surgery — JVM-testable.
 */
object LinkSanitizer {
    private val UTM_PREFIX = "utm_"

    // Explicit (non-utm_) tracking params seen in the wild.
    private val TRACKING_PARAMS = setOf(
        "fbclid", "gclid", "msclkid", "mc_cid", "mc_eid", "igshid", "_ga", "_gl", "cmpid",
    )

    fun sanitize(url: String): String {
        val base0 = unwrapAmp(url)
        val qIdx = base0.indexOf('?')
        if (qIdx == -1) return base0
        val base = base0.substring(0, qIdx)
        val rest = base0.substring(qIdx + 1)
        val fragIdx = rest.indexOf('#')
        val query = if (fragIdx >= 0) rest.substring(0, fragIdx) else rest
        val fragment = if (fragIdx >= 0) rest.substring(fragIdx) else ""
        val kept = query.split('&')
            .filter { it.isNotBlank() && !isTracking(it.substringBefore('=')) }
        if (kept.isEmpty()) return base + fragment
        return base + "?" + kept.joinToString("&") + fragment
    }

    private fun isTracking(key: String): Boolean {
        val k = key.lowercase()
        return k.startsWith(UTM_PREFIX) || k in TRACKING_PARAMS
    }

    /** domain.com/amp/story -> domain.com/story, domain.com/amp -> domain.com/ (leading-only, deliberately conservative). */
    fun unwrapAmp(url: String): String {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd == -1) return url
        val after = url.substring(schemeEnd + 3)
        val pathStart = after.indexOf('/')
        if (pathStart == -1) return url
        val host = after.substring(0, pathStart)
        val pathAndMore = after.substring(pathStart)
        val cleaned = when {
            pathAndMore.startsWith("/amp/") -> pathAndMore.removePrefix("/amp")
            pathAndMore == "/amp" -> "/"
            else -> pathAndMore
        }
        return url.substring(0, schemeEnd + 3) + host + cleaned
    }
}
