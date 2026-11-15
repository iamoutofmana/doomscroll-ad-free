package io.github.iamoutofmana.doomscroll.domain

import io.github.iamoutofmana.doomscroll.domain.links.UrlSafety
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Security: feed content is attacker-influenced. Article links must never reach
 * local/private network resources or non-web schemes (lite-SSRF / intent abuse).
 */
class UrlSafetyTest {
    @Test
    fun allowsPublicHttps() {
        assertTrue(UrlSafety.isSafeArticleUrl("https://www.theverge.com/2026/11/13/article"))
        assertTrue(UrlSafety.isSafeArticleUrl("https://8.8.8.8/article"))
    }

    @Test
    fun rejectsCleartextHttpForArticles() {
        assertFalse(UrlSafety.isSafeArticleUrl("http://example.com/article"))
    }

    @Test
    fun rejectsNonWebSchemes() {
        val schemes = listOf(
            "javascript:alert(1)",
            "intent://scan/#Intent;scheme=zxing;end",
            "file:///etc/passwd",
            "content://com.example.provider/data",
            "data:text/html,<script>alert(1)</script>",
        )
        schemes.forEach { url ->
            assertFalse("scheme must be rejected: $url", UrlSafety.isSafeArticleUrl(url))
            assertFalse("scheme must be rejected: $url", UrlSafety.isOpenableWebUrl(url))
        }
    }

    @Test
    fun rejectsLoopbackAndPrivateNetworks() {
        val urls = listOf(
            "https://localhost/admin",
            "https://127.0.0.1/",
            "https://127.8.9.10/",
            "https://10.0.0.5/router",
            "https://172.16.4.2/",
            "https://172.31.255.1/",
            "https://192.168.1.1/admin",
            "https://169.254.169.254/latest/meta-data",
            "https://printer.local/status",
            "https://[::1]/",
        )
        urls.forEach { url ->
            assertFalse("must be blocked: $url", UrlSafety.isSafeArticleUrl(url))
        }
    }

    @Test
    fun publicAddressesInPrivateRangesStayAllowed() {
        assertTrue(UrlSafety.isSafeArticleUrl("https://172.15.0.1/article"))
        assertTrue(UrlSafety.isSafeArticleUrl("https://172.32.0.1/article"))
        assertTrue(UrlSafety.isSafeArticleUrl("https://192.169.0.1/article"))
    }

    @Test
    fun openableWebUrlCoversBothWebSchemes() {
        assertTrue(UrlSafety.isOpenableWebUrl("http://example.com/article"))
        assertTrue(UrlSafety.isOpenableWebUrl("https://example.com/article"))
        assertFalse(UrlSafety.isOpenableWebUrl("ftp://example.com/file"))
    }

    @Test
    fun imageUrlsMustBeWebSchemes() {
        assertTrue(UrlSafety.isSafeImageUrl("https://cdn.example.com/hero.jpg"))
        assertTrue(UrlSafety.isSafeImageUrl("http://cdn.example.com/hero.jpg"))
        assertFalse(UrlSafety.isSafeImageUrl("file:///sdcard/secret.jpg"))
        assertFalse(UrlSafety.isSafeImageUrl("javascript:alert(1)"))
    }
}
