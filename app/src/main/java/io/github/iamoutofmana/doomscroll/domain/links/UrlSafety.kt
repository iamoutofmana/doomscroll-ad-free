package io.github.iamoutofmana.doomscroll.domain.links

import java.net.URI

/**
 * URL safety for links that originate from feed content (attacker-influenced).
 *
 * Feed entries must never be able to:
 * - open non-web schemes (javascript:, intent:, file:, content:, data:)
 * - make the app fetch local/private network resources (router admin pages, localhost
 *   services — the classic lite-SSRF vector on-device)
 *
 * User-typed feed URLs are explicit intent and are only required to be https
 * (see FeedFetcher); article links inside feeds get the stricter treatment here.
 */
object UrlSafety {
    fun isSafeArticleUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase()?.trim() ?: return false
        if (host.isEmpty()) return false
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) return false
        if (isPrivateIpv4(host) || isPrivateIpv6(host)) return false
        return true
    }

    /** For Custom Tabs / sharing: plain web links only. */
    fun isOpenableWebUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        return scheme == "http" || scheme == "https"
    }

    /** Images may be http(s); everything else is rejected. */
    fun isSafeImageUrl(url: String): Boolean = isOpenableWebUrl(url)

    private fun isPrivateIpv4(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4) return false
        val octets = parts.map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false
        val (a, b) = octets
        return a == 0 ||
            a == 10 ||
            a == 127 ||
            (a == 169 && b == 254) ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 168)
    }

    private fun isPrivateIpv6(host: String): Boolean {
        val normalized = host.trim('[', ']').lowercase()
        return normalized == "::1" ||
            normalized.startsWith("fc") ||
            normalized.startsWith("fd") ||
            normalized.startsWith("fe80")
    }
}
