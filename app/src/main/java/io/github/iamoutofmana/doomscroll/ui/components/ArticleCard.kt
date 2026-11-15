@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.iamoutofmana.doomscroll.ui.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.iamoutofmana.doomscroll.R
import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.domain.links.LinkSanitizer
import io.github.iamoutofmana.doomscroll.domain.time.TimeText

/**
 * Feed card with the two-stage interaction from user feedback:
 * tap once = expand (summary, related coverage), tap again = open in Custom Tabs.
 * The overflow menu is pinned to the right edge so it never moves with title length.
 */
@Composable
fun ArticleCard(
    article: ArticleEntity,
    onTap: () -> Unit,
    onToggleSave: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
    related: List<ArticleEntity> = emptyList(),
    expanded: Boolean = false,
    previewLoading: Boolean = false,
    onOpenRelated: (ArticleEntity) -> Unit = {},
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick = onTap,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column {
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                ) {
                    val metaParts = listOfNotNull(
                        article.publisher,
                        TimeText.relative(article.publishedAt).ifBlank { null },
                        if (related.isNotEmpty()) {
                            stringResource(R.string.related_count, related.size)
                        } else {
                            null
                        },
                    )
                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = metaParts.joinToString("  ·  "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (article.readAt == null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (article.readAt == null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (expanded) {
                        Spacer(Modifier.height(6.dp))
                        when {
                            !article.summary.isNullOrBlank() -> {
                                Text(
                                    text = article.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            previewLoading -> {
                                Text(
                                    text = stringResource(R.string.preview_loading),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            else -> {
                                Text(
                                    text = stringResource(R.string.tap_to_open),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (related.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.related_header),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            related.forEach { other ->
                                RelatedRow(other = other, onOpen = { onOpenRelated(other) })
                            }
                        }
                    }
                }
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.padding(top = 4.dp, end = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (article.savedAt == null) R.string.article_save else R.string.article_unsave,
                                    ),
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onToggleSave()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.article_hide)) },
                            onClick = {
                                menuOpen = false
                                onHide()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.article_share)) },
                            onClick = {
                                menuOpen = false
                                val url = LinkSanitizer.sanitize(article.resolvedLink ?: article.link)
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, url)
                                }
                                context.startActivity(Intent.createChooser(send, null))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedRow(
    other: ArticleEntity,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onOpen)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val meta = listOfNotNull(
            other.publisher,
            TimeText.relative(other.publishedAt).ifBlank { null },
        ).joinToString("  ·  ")
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = other.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
