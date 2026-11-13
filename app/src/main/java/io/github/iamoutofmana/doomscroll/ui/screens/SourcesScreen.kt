package io.github.iamoutofmana.doomscroll.ui.screens

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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import io.github.iamoutofmana.doomscroll.domain.googlenews.GoogleNews
import io.github.iamoutofmana.doomscroll.domain.model.SourceType
import io.github.iamoutofmana.doomscroll.domain.popularsources.PopularSources
import io.github.iamoutofmana.doomscroll.domain.time.TimeText
import io.github.iamoutofmana.doomscroll.ui.LocalAppContainer
import kotlinx.coroutines.launch

@Composable
fun SourcesScreen(contentPadding: PaddingValues) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sources by container.sourceRepository.sources.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAdd by remember { mutableStateOf(false) }
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
            val typeLabel = when (source.sourceType) {
                SourceType.GN_TOPIC -> stringResource(R.string.source_type_gn_topic)
                SourceType.GN_SEARCH -> stringResource(R.string.source_type_gn_search)
                SourceType.RSS -> stringResource(R.string.source_type_rss)
                SourceType.YOUTUBE -> stringResource(R.string.source_type_youtube)
                SourceType.REDDIT -> stringResource(R.string.source_type_reddit)
                SourceType.LEMMY -> stringResource(R.string.source_type_lemmy)
                SourceType.HACKER_NEWS -> stringResource(R.string.source_type_hackernews)
            }
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
