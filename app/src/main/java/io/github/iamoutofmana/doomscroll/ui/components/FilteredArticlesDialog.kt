package io.github.iamoutofmana.doomscroll.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.iamoutofmana.doomscroll.R
import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.domain.model.HiddenReason
import io.github.iamoutofmana.doomscroll.domain.time.TimeText

/**
 * Review surface for everything the sponsored filter (or you) hid.
 * Hiding is always logged with a reason and reversible — plan §7 promise.
 */
@Composable
fun FilteredArticlesDialog(
    articles: List<ArticleEntity>,
    onDismiss: () -> Unit,
    onUnhide: (Long) -> Unit,
    onUnhideAll: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filtered_title)) },
        text = {
            if (articles.isEmpty()) {
                Text(stringResource(R.string.filtered_empty))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(articles, key = { it.id }) { article ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = article.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val reason = article.hiddenReason?.let { raw ->
                                    runCatching { HiddenReason.valueOf(raw).label }.getOrDefault(raw)
                                }.orEmpty()
                                Text(
                                    text = listOf(reason, TimeText.relative(article.publishedAt))
                                        .filter { it.isNotBlank() }
                                        .joinToString("  ·  "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { onUnhide(article.id) }) {
                                Text(stringResource(R.string.filtered_unhide))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (articles.isNotEmpty()) {
                TextButton(
                    onClick = {
                        onUnhideAll()
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.filtered_unhide_all))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}
