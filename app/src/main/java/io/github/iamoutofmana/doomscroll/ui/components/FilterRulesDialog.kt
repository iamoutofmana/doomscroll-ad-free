package io.github.iamoutofmana.doomscroll.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.iamoutofmana.doomscroll.R
import io.github.iamoutofmana.doomscroll.domain.filter.RuleType
import io.github.iamoutofmana.doomscroll.ui.LocalAppContainer
import kotlinx.coroutines.launch

/** Manage user filter rules: hide by keyword or domain (Phase 2 filter engine). */
@Composable
fun FilterRulesDialog(onDismiss: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val rules by container.ruleRepository.rules.collectAsStateWithLifecycle(initialValue = emptyList())
    var type by remember { mutableStateOf(RuleType.KEYWORD) }
    var pattern by remember { mutableStateOf("") }

    fun submit() {
        val value = pattern.trim()
        if (value.isEmpty()) return
        scope.launch {
            container.ruleRepository.add(type, value)
            pattern = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rules_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.rules_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    FilterChip(
                        selected = type == RuleType.KEYWORD,
                        onClick = { type = RuleType.KEYWORD },
                        label = { Text(stringResource(R.string.rules_keyword)) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = type == RuleType.DOMAIN,
                        onClick = { type = RuleType.DOMAIN },
                        label = { Text(stringResource(R.string.rules_domain)) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        placeholder = { Text(stringResource(R.string.rules_pattern_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { submit() },
                        enabled = pattern.isNotBlank(),
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(stringResource(R.string.rules_add))
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (rules.isEmpty()) {
                    Text(
                        text = stringResource(R.string.rules_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(rules, key = { it.id }) { rule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rule.pattern, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = if (rule.type == RuleType.KEYWORD.name) {
                                            stringResource(R.string.rules_keyword)
                                        } else {
                                            stringResource(R.string.rules_domain)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { scope.launch { container.ruleRepository.delete(rule.id) } }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.rules_delete),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}
