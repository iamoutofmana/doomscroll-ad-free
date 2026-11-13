@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.iamoutofmana.doomscroll.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.iamoutofmana.doomscroll.R
import io.github.iamoutofmana.doomscroll.ui.LocalAppContainer
import io.github.iamoutofmana.doomscroll.ui.components.ArticleCard
import io.github.iamoutofmana.doomscroll.ui.components.FilteredArticlesDialog
import io.github.iamoutofmana.doomscroll.ui.components.rememberOpenArticle
import kotlinx.coroutines.launch

private const val AUTO_SYNC_STALE_MS = 15 * 60_000L

@Composable
fun FeedScreen(
    contentPadding: PaddingValues,
    onOpenSources: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val articles by container.articleRepository.feed.collectAsStateWithLifecycle(initialValue = emptyList())
    val sources by container.sourceRepository.sources.collectAsStateWithLifecycle(initialValue = emptyList())
    val hidden by container.articleRepository.hidden.collectAsStateWithLifecycle(initialValue = emptyList())
    var refreshing by remember { mutableStateOf(false) }
    var showFiltered by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val openArticle = rememberOpenArticle()

    fun syncNow() {
        scope.launch {
            refreshing = true
            val stats = container.syncEngine.syncAll()
            container.lastAutoSyncAt = System.currentTimeMillis()
            refreshing = false
            val msg = when {
                stats.failures > 0 -> context.getString(R.string.synced_errors, stats.failures)
                stats.newArticles > 0 -> context.getString(R.string.synced_new, stats.newArticles)
                else -> context.getString(R.string.synced_up_to_date)
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Silent auto-sync when the app is opened with stale data (plan Appendix B).
    LaunchedEffect(Unit) {
        val stale = System.currentTimeMillis() - container.lastAutoSyncAt > AUTO_SYNC_STALE_MS
        if (sources.isNotEmpty() && stale) syncNow()
    }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (hidden.isNotEmpty()) {
                        TextButton(onClick = { showFiltered = true }) {
                            Text(stringResource(R.string.action_filtered, hidden.size))
                        }
                    }
                    IconButton(onClick = { scope.launch { container.articleRepository.markAllRead() } }) {
                        Icon(
                            Icons.Default.Done,
                            contentDescription = stringResource(R.string.action_mark_all_read),
                        )
                    }
                    IconButton(onClick = { syncNow() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (sources.isEmpty()) {
            EmptyFeedPlaceholder(innerPadding, onOpenSources)
        } else {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { syncNow() },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (articles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.feed_no_articles_body),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(32.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = innerPadding,
                    ) {
                        items(articles, key = { it.id }) { article ->
                            ArticleCard(
                                article = article,
                                onOpen = { openArticle(article) },
                                onToggleSave = {
                                    scope.launch {
                                        container.articleRepository.setSaved(article.id, article.savedAt == null)
                                    }
                                },
                                onHide = {
                                    scope.launch { container.articleRepository.hide(article.id) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFiltered) {
        FilteredArticlesDialog(
            articles = hidden,
            onDismiss = { showFiltered = false },
            onUnhide = { id -> scope.launch { container.articleRepository.unhide(id) } },
            onUnhideAll = { scope.launch { container.articleRepository.unhideAll() } },
        )
    }
}

@Composable
private fun EmptyFeedPlaceholder(
    contentPadding: PaddingValues,
    onOpenSources: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.feed_empty_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.feed_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onOpenSources) {
                Text(stringResource(R.string.feed_empty_cta))
            }
        }
    }
}
