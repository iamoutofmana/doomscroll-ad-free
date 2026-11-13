@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.iamoutofmana.doomscroll.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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

/** Bottom sheet for adding sources: Google News topics, Google News searches, raw feed URLs. */
@Composable
fun AddSourceSheet(
    onDismiss: () -> Unit,
    onAddGoogleNews: (List<GoogleNews.Topic>, GoogleNews.GnLocale) -> Unit,
    onAddSearch: (String, GoogleNews.GnLocale) -> Unit,
    onAddRss: (String) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var locale by remember { mutableStateOf(GoogleNews.DEFAULT_LOCALE) }
    val selectedTopics = remember { mutableStateListOf<GoogleNews.Topic>() }
    var query by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

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
                    text = { Text(stringResource(R.string.add_sheet_search_tab)) },
                )
                Tab(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    text = { Text(stringResource(R.string.add_sheet_rss_tab)) },
                )
            }

            when (tab) {
                0 -> {
                    LocaleRow(selected = locale, onSelect = { locale = it })
                    Text(
                        text = stringResource(R.string.add_sheet_topics_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
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
                }

                1 -> {
                    LocaleRow(selected = locale, onSelect = { locale = it })
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

                else -> {
                    val trimmed = url.trim()
                    val isPlainHttp = trimmed.startsWith("http://", ignoreCase = true)
                    val isValid = trimmed.startsWith("https://", ignoreCase = true)
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
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
            }
        }
    }
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
