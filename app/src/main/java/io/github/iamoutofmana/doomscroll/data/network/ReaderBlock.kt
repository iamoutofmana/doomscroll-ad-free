package io.github.iamoutofmana.doomscroll.data.network

import kotlinx.serialization.Serializable

/**
 * A structured reader-mode block (Phase 2.1): headings, paragraphs, bullets and images,
 * serialized into the article's `readerText` cache so formatting survives re-opens.
 */
@Serializable
data class ReaderBlock(
    /** One of: heading, paragraph, bullet, image */
    val type: String,
    val text: String? = null,
    val imageUrl: String? = null,
) {
    companion object {
        const val HEADING = "heading"
        const val PARAGRAPH = "paragraph"
        const val BULLET = "bullet"
        const val IMAGE = "image"
    }
}
