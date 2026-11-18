package io.github.iamoutofmana.doomscroll.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.iamoutofmana.doomscroll.R
import io.github.iamoutofmana.doomscroll.domain.model.SourceType

/** Shared human-readable label for a source type (source rows, topic packs). */
@Composable
fun sourceTypeLabel(type: SourceType): String = stringResource(
    when (type) {
        SourceType.GN_TOPIC -> R.string.source_type_gn_topic
        SourceType.GN_SEARCH -> R.string.source_type_gn_search
        SourceType.RSS -> R.string.source_type_rss
        SourceType.YOUTUBE -> R.string.source_type_youtube
        SourceType.REDDIT -> R.string.source_type_reddit
        SourceType.LEMMY -> R.string.source_type_lemmy
        SourceType.HACKER_NEWS -> R.string.source_type_hackernews
    },
)
