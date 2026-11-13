package io.github.iamoutofmana.doomscroll.domain.links

/** Minimal HTML-to-text for card summaries. Full reader-mode extraction is Phase 2 (jsoup). */
object HtmlText {
    private val TAG = Regex("<[^>]*>")
    private val NAMED_ENTITIES = listOf("nbsp", "lt", "gt", "quot", "apos")
    private val NUMERIC_ENTITY = Regex("&#(\\d+);")
    private val HEX_ENTITY = Regex("&#x([0-9a-fA-F]+);")
    private val WHITESPACE = Regex("\\s+")

    fun strip(html: String): String {
        val noTags = html.replace(TAG, " ")
        return collapse(unescapeEntities(noTags))
    }

    fun unescapeEntities(s: String): String {
        var out = s
        // Numeric first (guarded against double-unescaping &amp;#38;).
        out = NUMERIC_ENTITY.replace(out) { m ->
            m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: m.value
        }
        out = HEX_ENTITY.replace(out) { m ->
            m.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: m.value
        }
        // Named entities, &amp; last so it can't re-open the others.
        for (name in NAMED_ENTITIES) {
            val replacement = when (name) {
                "nbsp" -> " "
                "lt" -> "<"
                "gt" -> ">"
                "quot" -> "\""
                "apos" -> "'"
                else -> name
            }
            out = out.replace("&$name;", replacement)
        }
        return out.replace("&amp;", "&")
    }

    private fun collapse(s: String): String = WHITESPACE.replace(s, " ").trim()
}
