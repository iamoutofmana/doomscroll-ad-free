@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.iamoutofmana.doomscroll.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.iamoutofmana.doomscroll.R
import io.github.iamoutofmana.doomscroll.data.db.ArticleEntity
import io.github.iamoutofmana.doomscroll.data.network.ReaderBlock
import io.github.iamoutofmana.doomscroll.data.repo.ReaderService
import io.github.iamoutofmana.doomscroll.domain.links.LinkSanitizer
import io.github.iamoutofmana.doomscroll.domain.time.TimeText
import io.github.iamoutofmana.doomscroll.ui.LocalAppContainer
import io.github.iamoutofmana.doomscroll.ui.openInCustomTab

/**
 * In-app reader (Phase 2): extracted article text, no ads, adjustable type.
 * The publisher's page is one tap away via "Original".
 */
@Composable
fun ReaderScreen(
    articleId: Long,
    onClose: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    var state by remember { mutableStateOf<ReaderService.Result?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var fontScale by rememberSaveable { mutableFloatStateOf(1f) }

    LaunchedEffect(articleId, attempt) {
        state = container.readerService.load(articleId)
    }

    val article: ArticleEntity? = when (val s = state) {
        is ReaderService.Result.Content -> s.article
        is ReaderService.Result.Failed -> s.article
        null -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
                title = {
                    Text(
                        text = article?.publisher.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    TextButton(
                        onClick = { fontScale = (fontScale - 0.1f).coerceAtLeast(0.8f) },
                    ) { Text(stringResource(R.string.reader_font_smaller)) }
                    TextButton(
                        onClick = { fontScale = (fontScale + 0.1f).coerceAtMost(1.8f) },
                    ) { Text(stringResource(R.string.reader_font_larger)) }
                    TextButton(
                        onClick = {
                            article?.let { a ->
                                val url = LinkSanitizer.sanitize(a.resolvedLink ?: a.link)
                                if (url.isNotBlank()) context.openInCustomTab(url)
                            }
                        },
                    ) { Text(stringResource(R.string.reader_original)) }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.reader_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is ReaderService.Result.Failed -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.reader_failed),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row {
                            Button(onClick = { attempt++ }) {
                                Text(stringResource(R.string.reader_retry))
                            }
                            TextButton(
                                onClick = {
                                    s.article?.let { a ->
                                        val url = LinkSanitizer.sanitize(a.resolvedLink ?: a.link)
                                        if (url.isNotBlank()) context.openInCustomTab(url)
                                    }
                                },
                            ) {
                                Text(stringResource(R.string.reader_original))
                            }
                        }
                    }
                }
            }

            is ReaderService.Result.Content -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 32.dp,
                    ),
                ) {
                    item {
                        Text(
                            text = s.title ?: article?.title.orEmpty(),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        val meta = listOfNotNull(
                            article?.publisher,
                            TimeText.relative(article?.publishedAt).ifBlank { null },
                        ).joinToString("  ·  ")
                        if (meta.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    items(s.blocks) { block ->
                        ReaderBlockView(block = block, fontScale = fontScale)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderBlockView(
    block: ReaderBlock,
    fontScale: Float,
) {
    when (block.type) {
        ReaderBlock.IMAGE -> {
            val url = block.imageUrl
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        ReaderBlock.HEADING -> {
            Text(
                text = block.text.orEmpty(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp * fontScale,
                    lineHeight = 27.sp * fontScale,
                ),
                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
            )
        }
        ReaderBlock.BULLET -> {
            Text(
                text = "•  ${block.text.orEmpty()}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp * fontScale,
                    lineHeight = 26.sp * fontScale,
                ),
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            )
        }
        else -> {
            Text(
                text = block.text.orEmpty(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp * fontScale,
                    lineHeight = 26.sp * fontScale,
                ),
                modifier = Modifier.padding(vertical = 7.dp),
            )
        }
    }
}
