package io.github.iamoutofmana.doomscroll.domain

/**
 * App identity constants for non-UI contexts (widget labels, feed User-Agent, logs).
 * UI strings live in res/values/strings.xml — keep both sides in sync.
 */
object AppMetadata {
    const val PACKAGE = "io.github.iamoutofmana.doomscroll"
    const val APP_NAME = "Doomscroll Ad Free"

    /** Used for feed fetches from Phase 1 (e.g., Reddit requires a descriptive UA). */
    const val USER_AGENT = "DoomscrollAdFree/0.1 (+https://github.com/iamoutofmana/doomscroll-ad-free)"
}
