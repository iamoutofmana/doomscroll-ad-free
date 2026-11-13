@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.iamoutofmana.doomscroll.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.iamoutofmana.doomscroll.R
import io.github.iamoutofmana.doomscroll.domain.googlenews.GoogleNews

/**
 * Bottom sheet for adding sources. Two top-level tabs:
 * - **Google News**: topics, keyword searches, locale picker
 * - **More sources**: YouTube, Reddit, Lemmy, Hacker News, generic RSS
 */
@Composable
fun AddSourceSheet(
    onDismiss: () -> Unit,
    onAddGoogleNews: (List<GoogleNews.Topic>, GoogleNews.GnLocale) -> Unit,
    onAddSearch: (String, GoogleNews.GnLocale) -> Unit,
    onAddRss: (String) -> Unit,
    onAddYouTube: (String) -> Unit,
    onAddReddit: (String) -> Unit,
    onAddLemmy: (String, String) -> Unit,
    onAddHackerNews: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(stringResource(R.string.add_sheet_gn_tab)) },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(stringResource(R.string.add_sheet_more_tab)) },
                )
            }

            if (tab == 0) {
                GoogleNewsSection(onAddGoogleNews, onAddSearch)
            } else {
                MoreSourcesSection(onAddYouTube, onAddReddit, onAddLemmy, onAddHackerNews, onAddRss)
            }
        }
    }
}

@Composable
private fun GoogleNewsSection(
    onAddGoogleNews: (List<GoogleNews.Topic>, GoogleNews.GnLocale) -> Unit,
    onAddSearch: (String, GoogleNews.GnLocale) -> Unit,
) {
    var locale by remember { mutableStateOf(GoogleNews.DEFAULT_LOCALE) }
    val selectedTopics = remember { mutableStateListOf<GoogleNews.Topic>() }
    var query by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(0) } // 0 = topics, 1 = search

    LocaleRow(selected = locale, onSelect = { locale = it })
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
        FilterChip(
            selected = mode == 0,
            onClick = { mode = 0 },
            label = { Text(stringResource(R.string.add_sheet_topics_hint)) },
            modifier = Modifier.padding(end = 8.dp),
        )
        FilterChip(
            selected = mode == 1,
            onClick = { mode = 1 },
            label = { Text(stringResource(R.string.add_sheet_search_label)) },
        )
    }
    Spacer(Modifier.height(8.dp))

    if (mode == 0) {
        FlowRow(modifier = Modifier.padding(horizontal = 12.dp)) {
            GoogleNews.TOPICS.forEach { topic ->
                FilterChip(
                    selected = selectedTopics.contains(topic),
                    onClick = {
                        if (!selectedTopics.remove(topic)) selectedTopics.add(topic)
                    },
                    label = { Text(topic.display) },
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
        Button(
            onClick = { onAddGoogleNews(selectedTopics.toList(), locale) },
            enabled = selectedTopics.isNotEmpty(),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Text(stringResource(R.string.add_sheet_add_n, selectedTopics.size))
        }
    } else {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.add_sheet_search_label)) },
            placeholder = { Text(stringResource(R.string.add_sheet_search_hint)) },
            supportingText = { Text(stringResource(R.string.add_sheet_search_operators)) },
            singleLine = true,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        )
        Button(
            onClick = { onAddSearch(query.trim(), locale) },
            enabled = query.isNotBlank(),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Text(stringResource(R.string.add_sheet_add_search))
        }
    }
}

@Composable
private fun MoreSourcesSection(
    onAddYouTube: (String) -> Unit,
    onAddReddit: (String) -> Unit,
    onAddLemmy: (String, String) -> Unit,
    onAddHackerNews: () -> Unit,
    onAddRss: (String) -> Unit,
) {
    var yt by remember { mutableStateOf("") }
    var sub by remember { mutableStateOf("") }
    var lemmyInstance by remember { mutableStateOf("") }
    var lemmyCommunity by remember { mutableStateOf("") }
    var rss by remember { mutableStateOf("") }

    // YouTube
    SectionHeader(R.string.add_sheet_youtube_title)
    OutlinedTextField(
        value = yt,
        onValueChange = { yt = it },
        label = { Text(stringResource(R.string.add_sheet_youtube_label)) },
        placeholder = { Text(stringResource(R.string.add_sheet_youtube_hint)) },
        supportingText = { Text(stringResource(R.string.add_sheet_youtube_hint2)) },
        singleLine = true,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    )
    Button(
        onClick = { onAddYouTube(yt.trim()) },
        enabled = yt.isNotBlank(),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Text(stringResource(R.string.add_sheet_add_youtube))
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // Reddit
    SectionHeader(R.string.add_sheet_reddit_title)
    OutlinedTextField(
        value = sub,
        onValueChange = { sub = it },
        label = { Text(stringResource(R.string.add_sheet_reddit_label)) },
        placeholder = { Text(stringResource(R.string.add_sheet_reddit_hint)) },
        singleLine = true,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    )
    Button(
        onClick = { onAddReddit(sub.trim()) },
        enabled = sub.isNotBlank(),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Text(stringResource(R.string.add_sheet_add_reddit))
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // Lemmy
    SectionHeader(R.string.add_sheet_lemmy_title)
    OutlinedTextField(
        value = lemmyInstance,
        onValueChange = { lemmyInstance = it },
        label = { Text(stringResource(R.string.add_sheet_lemmy_instance_label)) },
        placeholder = { Text(stringResource(R.string.add_sheet_lemmy_instance_hint)) },
        singleLine = true,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    )
    OutlinedTextField(
        value = lemmyCommunity,
        onValueChange = { lemmyCommunity = it },
        label = { Text(stringResource(R.string.add_sheet_lemmy_community_label)) },
        placeholder = { Text(stringResource(R.string.add_sheet_lemmy_community_hint)) },
        singleLine = true,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth(),
    )
    Button(
        onClick = { onAddLemmy(lemmyInstance, lemmyCommunity) },
        enabled = lemmyInstance.isNotBlank() && lemmyCommunity.isNotBlank(),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Text(stringResource(R.string.add_sheet_add_lemmy))
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // Hacker News
    SectionHeader(R.string.add_sheet_hn_title)
    Text(
        text = stringResource(R.string.add_sheet_hn_body),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    Button(
        onClick = onAddHackerNews,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth(),
    ) {
        Text(stringResource(R.string.add_sheet_add_hn))
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // Generic RSS
    SectionHeader(R.string.add_sheet_rss_title)
    val trimmed = rss.trim()
    val isPlainHttp = trimmed.startsWith("http://", ignoreCase = true)
    val isValid = trimmed.startsWith("https://", ignoreCase = true)
    OutlinedTextField(
        value = rss,
        onValueChange = { rss = it },
        label = { Text(stringResource(R.string.add_sheet_rss_label)) },
        placeholder = { Text(stringResource(R.string.add_sheet_rss_hint)) },
        supportingText = {
            if (isPlainHttp) {
                Text(
                    stringResource(R.string.add_sheet_rss_http_warning),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        isError = isPlainHttp,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
    )
    Button(
        onClick = { onAddRss(trimmed) },
        enabled = isValid,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Text(stringResource(R.string.add_sheet_add_rss))
    }
}

@Composable
private fun SectionHeader(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun LocaleRow(
    selected: GoogleNews.GnLocale,
    onSelect: (GoogleNews.GnLocale) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = stringResource(R.string.add_sheet_locale),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
        ) {
            GoogleNews.LOCALES.forEach { loc ->
                FilterChip(
                    selected = loc == selected,
                    onClick = { onSelect(loc) },
                    label = { Text(loc.display) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}
