package io.github.iamoutofmana.doomscroll.domain

/**
 * App identity constants for non-UI contexts (widget labels, feed User-Agent, logs).
 * UI strings live in res/values/strings.xml — keep both sides in sync.
 */
object AppMetadata {
    const val PACKAGE = "io.github.iamoutofmana.doomscroll"
    const val APP_NAME = "Doomscroll Ad Free"

    /** Used for feed fetches from Phase 1 (e.g., Reddit requires a descriptive UA). */
    const val USER_AGENT = "DoomscrollAdFree/0.2 (+https://github.com/iamoutofmana/doomscroll-ad-free)"

    /** Browser-ish UA for publisher pages (previews / redirect resolution) — some sites serve nothing useful otherwise. */
    const val BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
}
