package io.github.iamoutofmana.doomscroll.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.iamoutofmana.doomscroll.R
import io.github.iamoutofmana.doomscroll.data.db.SourceEntity
import io.github.iamoutofmana.doomscroll.data.network.FeedDiscovery
import io.github.iamoutofmana.doomscroll.domain.googlenews.GoogleNews
import io.github.iamoutofmana.doomscroll.domain.model.SourceType
import io.github.iamoutofmana.doomscroll.domain.packs.TopicPacks
import io.github.iamoutofmana.doomscroll.domain.popularsources.PopularSources
import io.github.iamoutofmana.doomscroll.domain.time.TimeText
import io.github.iamoutofmana.doomscroll.ui.LocalAppContainer
import io.github.iamoutofmana.doomscroll.ui.components.FilterRulesDialog
import io.github.iamoutofmana.doomscroll.ui.components.sourceTypeLabel
import kotlinx.coroutines.launch

@Composable
fun SourcesScreen(contentPadding: PaddingValues) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sources by container.sourceRepository.sources.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var showPacks by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    suspend fun doSync() {
        syncing = true
        val stats = container.syncEngine.syncAll()
        container.lastAutoSyncAt = System.currentTimeMillis()
        syncing = false
        val msg = when {
            stats.failures > 0 -> context.getString(R.string.synced_errors, stats.failures)
            stats.newArticles > 0 -> context.getString(R.string.synced_new, stats.newArticles)
            else -> context.getString(R.string.synced_up_to_date)
        }
        snackbarHostState.showSnackbar(msg)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sources_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showFilters = true }) {
                    Text(stringResource(R.string.rules_button))
                }
                IconButton(
                    onClick = { scope.launch { doSync() } },
                    enabled = !syncing,
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                    )
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.packs_discover_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.packs_discover_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showPacks = true }) {
                            Text(stringResource(R.string.packs_browse))
                        }
                    }
                }
            }
            if (sources.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(R.string.sources_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.sources_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sources, key = { it.id }) { source ->
                        SourceRow(
                            source = source,
                            onToggle = { enabled ->
                                scope.launch { container.sourceRepository.setEnabled(source.id, enabled) }
                            },
                            onDelete = {
                                scope.launch { container.sourceRepository.delete(source.id) }
                            },
                        )
                        HorizontalDivider()
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.sources_add)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
        )
    }

    if (showAdd) {
        AddSourceSheet(
            onDismiss = { showAdd = false },
            onAddGoogleNews = { topics, locale ->
                showAdd = false
                scope.launch {
                    topics.forEach { topic ->
                        container.sourceRepository.add(
                            SourceType.GN_TOPIC,
                            GoogleNews.topicUrl(topic, locale),
                            context.getString(R.string.source_title_gn_topic, topic.display, locale.display),
                        )
                    }
                    doSync()
                }
            },
            onAddSearch = { query, locale ->
                showAdd = false
                scope.launch {
                    container.sourceRepository.add(
                        SourceType.GN_SEARCH,
                        GoogleNews.searchUrl(query, locale),
                        context.getString(R.string.source_title_gn_search, query, locale.display),
                    )
                    doSync()
                }
            },
            onAddRss = { url ->
                showAdd = false
                scope.launch {
                    val host = Uri.parse(url).host ?: url
                    container.sourceRepository.add(SourceType.RSS, url, host)
                    doSync()
                }
            },
            onAddFromLink = { raw ->
                showAdd = false
                scope.launch {
                    when (val result = container.feedDiscovery.discover(raw)) {
                        is FeedDiscovery.Result.Found -> {
                            val title = result.title?.takeIf { it.isNotBlank() }
                                ?: Uri.parse(result.feedUrl).host
                                ?: result.feedUrl
                            val inserted = container.sourceRepository.add(
                                SourceType.RSS,
                                result.feedUrl,
                                title,
                            )
                            if (inserted > 0L) {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.packs_added_one, title),
                                )
                                doSync()
                            } else {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.packs_already_one, title),
                                )
                            }
                        }

                        is FeedDiscovery.Result.Failed ->
                            snackbarHostState.showSnackbar(discoveryMessage(context, result.reason))
                    }
                }
            },
            onAddYouTube = { channelId ->
                showAdd = false
                scope.launch {
                    val url = PopularSources.youtubeChannelUrl(channelId)
                    container.sourceRepository.add(
                        SourceType.YOUTUBE,
                        url,
                        context.getString(R.string.source_title_youtube, channelId),
                    )
                    doSync()
                }
            },
            onAddReddit = { subreddit ->
                showAdd = false
                scope.launch {
                    val url = PopularSources.redditSubredditUrl(subreddit)
                    if (url.isBlank()) return@launch
                    val name = subreddit.trim().removePrefix("r/").removePrefix("/r/")
                    container.sourceRepository.add(
                        SourceType.REDDIT,
                        url,
                        context.getString(R.string.source_title_reddit, name),
                    )
                    doSync()
                }
            },
            onAddLemmy = { instance, community ->
                showAdd = false
                scope.launch {
                    val url = PopularSources.lemmyCommunityUrl(instance, community)
                    if (url.isBlank()) return@launch
                    container.sourceRepository.add(
                        SourceType.LEMMY,
                        url,
                        context.getString(R.string.source_title_lemmy, community, instance.trim()),
                    )
                    doSync()
                }
            },
            onAddHackerNews = {
                showAdd = false
                scope.launch {
                    container.sourceRepository.add(
                        SourceType.HACKER_NEWS,
                        PopularSources.HACKER_NEWS_URL,
                        context.getString(R.string.source_title_hackernews),
                    )
                    doSync()
                }
            },
        )
    }

    if (showPacks) {
        PackSheet(
            packs = TopicPacks.ALL,
            onDismiss = { showPacks = false },
            onAdd = { _, chosen ->
                showPacks = false
                scope.launch {
                    val added = container.sourceRepository.addAll(chosen)
                    val message = when {
                        added == 0 -> context.getString(R.string.packs_added_none)
                        added == chosen.size -> context.getString(R.string.packs_added_n, added)
                        else -> context.getString(
                            R.string.packs_added_some,
                            added,
                            chosen.size,
                            chosen.size - added,
                        )
                    }
                    snackbarHostState.showSnackbar(message)
                    if (added > 0) doSync()
                }
            },
        )
    }

    if (showFilters) {
        FilterRulesDialog(onDismiss = { showFilters = false })
    }
}

@Composable
private fun SourceRow(
    source: SourceEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val typeLabel = sourceTypeLabel(source.sourceType)
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val status = when {
                source.lastError != null -> source.lastError
                source.lastSyncAt != null ->
                    stringResource(R.string.source_synced_at, TimeText.relative(source.lastSyncAt))
                else -> stringResource(R.string.source_never_synced)
            }
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = if (source.lastError != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(checked = source.enabled, onCheckedChange = onToggle)
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun discoveryMessage(
    context: Context,
    reason: FeedDiscovery.Reason,
): String = context.getString(
    when (reason) {
        FeedDiscovery.Reason.INVALID_URL -> R.string.feed_discovery_invalid
        FeedDiscovery.Reason.UNSAFE -> R.string.feed_discovery_unsafe
        FeedDiscovery.Reason.X_UNSUPPORTED -> R.string.feed_discovery_x
        FeedDiscovery.Reason.FETCH_FAILED -> R.string.feed_discovery_failed
        FeedDiscovery.Reason.NO_FEED -> R.string.feed_discovery_none
    },
)
