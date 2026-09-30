package com.example.carelink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.CareActivityType
import com.example.carelink.model.CareHistoryEntry
import com.example.carelink.model.CareHistoryFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CareHistoryScreen(
    entries: List<CareHistoryEntry> = emptyList(),
    isLoading: Boolean = false,
    loadError: String? = null,
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var selectedTypes by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Draft values do not change the list until the patient taps Apply filters.
    var applied by remember { mutableStateOf(CareHistoryFilter()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    // Validation should catch bad ranges first, but this guard keeps rendering safe.
    val visible = runCatching { applied.apply(entries) }.getOrDefault(emptyList())
    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TextButton(onClick = onBack) {
                    Text("‹ Back to medications")
                }

                Text(
                    text = "Care history",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            item {
                Text(
                    "Optional filters use Unix timestamps in milliseconds."
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start timestamp") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End timestamp") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                CareActivityType.entries.forEach { type ->
                    FilterChip(
                        selected = type.name in selectedTypes,
                        onClick = {
                            selectedTypes =
                                if (type.name in selectedTypes) {
                                    selectedTypes - type.name
                                } else {
                                    selectedTypes + type.name
                                }
                        },
                        label = {
                            Text(
                                type.name.lowercase()
                                    .replace('_', ' ')
                                    .replaceFirstChar(Char::uppercase)
                            )
                        }
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val startMillis = start.trim().toLongOrNull()
                            val endMillis = end.trim().toLongOrNull()

                            if (
                                (start.isNotBlank() && startMillis == null) ||
                                (end.isNotBlank() && endMillis == null)
                            ) {
                                error =
                                    "Enter valid timestamps or leave them blank."
                            } else {
                                val candidate = CareHistoryFilter(
                                    startMillis = startMillis,
                                    endMillis = endMillis,
                                    types = CareActivityType.entries
                                        .filter { it.name in selectedTypes }
                                        .toSet()
                                )

                                error = candidate.validate()

                                if (error == null) {
                                    applied = candidate
                                }
                            }
                        }
                    ) {
                        Text("Apply filters")
                    }

                    OutlinedButton(
                        onClick = {
                            start = ""
                            end = ""
                            selectedTypes = emptySet()
                            applied = CareHistoryFilter()
                            error = null
                        }
                    ) {
                        Text("Clear")
                    }
                }
            }

            error?.let { message ->
                item {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            when {
                isLoading -> {
                    item {
                        CircularProgressIndicator()
                        Text("Loading care history...")
                    }
                }

                loadError != null -> {
                    item {
                        Text(
                            loadError,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }

                visible.isEmpty() -> {
                    item {
                        Text(
                            if (entries.isEmpty()) {
                                "No recorded doses yet."
                            } else {
                                "No history matches these filters."
                            }
                        )
                    }
                }

                else -> {
                    items(visible, key = { it.id }) { entry ->
                        CareHistoryCard(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun CareHistoryCard(entry: CareHistoryEntry) {
    val formattedDate = SimpleDateFormat(
        "MMM d, yyyy • h:mm a",
        Locale.getDefault()
    ).format(Date(entry.occurredAtMillis))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = entry.summary,
                style = MaterialTheme.typography.titleMedium
            )

            Text("Scheduled dose: $formattedDate")
        }
    }
}