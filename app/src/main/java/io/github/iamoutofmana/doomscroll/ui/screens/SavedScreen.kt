package io.github.iamoutofmana.doomscroll.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.iamoutofmana.doomscroll.R
import io.github.iamoutofmana.doomscroll.ui.LocalAppContainer
import io.github.iamoutofmana.doomscroll.ui.components.ArticleCard
import io.github.iamoutofmana.doomscroll.ui.components.rememberOpenArticle
import kotlinx.coroutines.launch

@Composable
fun SavedScreen(
    contentPadding: PaddingValues,
    onRead: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val saved by container.articleRepository.saved.collectAsStateWithLifecycle(initialValue = emptyList())
    val openArticle = rememberOpenArticle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .statusBarsPadding(),
    ) {
        Text(
            text = stringResource(R.string.saved_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )
        if (saved.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.saved_empty), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.saved_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(saved, key = { it.id }) { article ->
                    ArticleCard(
                        article = article,
                        onTap = { onRead(article.id) },
                        onOpenInBrowser = { openArticle(article) },
                        onToggleSave = {
                            scope.launch { container.articleRepository.setSaved(article.id, false) }
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
