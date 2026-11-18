@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.iamoutofmana.doomscroll.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.iamoutofmana.doomscroll.R
import io.github.iamoutofmana.doomscroll.domain.model.NewSource
import io.github.iamoutofmana.doomscroll.domain.packs.TopicPack
import io.github.iamoutofmana.doomscroll.ui.components.sourceTypeLabel

/**
 * Two-level topic-pack browser inside one sheet:
 * packs list -> pack detail with per-source checkboxes -> add selected.
 */
@Composable
fun PackSheet(
    packs: List<TopicPack>,
    onDismiss: () -> Unit,
    onAdd: (TopicPack, List<NewSource>) -> Unit,
) {
    var openPack by remember { mutableStateOf<TopicPack?>(null) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            val pack = openPack
            if (pack == null) {
                Text(
                    text = stringResource(R.string.packs_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.packs_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                packs.forEach { topicPack ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .clickable {
                                openPack = topicPack
                                selected = topicPack.sources.map { it.url }.toSet()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = topicPack.emoji, fontSize = 26.sp)
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(
                                        text = topicPack.title,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = topicPack.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = topicPack.description,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.packs_sources_count, topicPack.sources.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { openPack = null }) {
                        Text(stringResource(R.string.packs_all))
                    }
                }
                Text(
                    text = "${pack.emoji}  ${pack.title}",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                Text(
                    text = pack.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
                pack.sources.forEach { source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (selected.contains(source.url)) {
                                    selected - source.url
                                } else {
                                    selected + source.url
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selected.contains(source.url),
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + source.url else selected - source.url
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = source.title,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = sourceTypeLabel(source.type),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        onAdd(pack, pack.sources.filter { selected.contains(it.url) })
                    },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.packs_add_n, selected.size))
                }
            }
        }
    }
}
