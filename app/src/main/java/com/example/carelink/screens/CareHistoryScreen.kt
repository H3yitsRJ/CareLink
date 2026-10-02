package com.example.carelink.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedTypes by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var appliedStart by rememberSaveable { mutableStateOf<Long?>(null) }
    var appliedEnd by rememberSaveable { mutableStateOf<Long?>(null) }
    var appliedTypes by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var dateField by remember { mutableStateOf<String?>(null) }
    val applied = remember(appliedStart, appliedEnd, appliedTypes) {
        CareHistoryFilter(appliedStart, appliedEnd, CareActivityType.entries.filter { it.name in appliedTypes }.toSet())
    }
    val visible = remember(entries, applied) { applied.apply(entries) }
    if (dateField != null) {
        val picker = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { dateField = null },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        // Material date picker dates are UTC calendar dates, not local instants.
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(Date(millis))
                        if (dateField == "start") start = date else end = date
                    }
                    dateField = null
                }, enabled = picker.selectedDateMillis != null) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { dateField = null }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }
    Scaffold { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TextButton(onClick = onBack) { Text("‹ Back to medications") }
                Text("Care history", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        start, { start = it }, label = { Text("Start date") },
                        placeholder = { Text("YYYY-MM-DD") }, singleLine = true,
                        trailingIcon = { TextButton(onClick = { dateField = "start" }) { Text("Choose") } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        end, { end = it }, label = { Text("End date") },
                        placeholder = { Text("YYYY-MM-DD") }, singleLine = true,
                        trailingIcon = { TextButton(onClick = { dateField = "end" }) { Text("Choose") } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Column {
                    CareActivityType.entries.forEach { type ->
                        FilterChip(
                            selected = type.name in selectedTypes,
                            onClick = {
                                selectedTypes = if (type.name in selectedTypes) selectedTypes - type.name
                                    else selectedTypes + type.name
                            }, label = { Text(type.label) }
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val first = historyDateBoundary(start.trim())
                        val last = historyDateBoundary(end.trim(), endOfDay = true)
                        error = if ((start.isNotBlank() && first == null) || (end.isNotBlank() && last == null)) {
                            "Enter dates as YYYY-MM-DD or leave them blank."
                        } else CareHistoryFilter(first, last).validate()
                        if (error == null) {
                            appliedStart = first
                            appliedEnd = last
                            appliedTypes = selectedTypes.toList()
                        }
                    }) { Text("Apply filters") }
                    OutlinedButton(onClick = {
                        start = ""; end = ""; selectedTypes = emptyList()
                        appliedStart = null; appliedEnd = null; appliedTypes = emptyList(); error = null
                    }) { Text("Clear") }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (applied.isActive) {
                item {
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    Text("Active filters: " + listOfNotNull(
                        appliedStart?.let { "From ${dateFormat.format(Date(it))}" },
                        appliedEnd?.let { "Through ${dateFormat.format(Date(it))}" },
                        applied.types.takeIf { it.isNotEmpty() }?.joinToString { it.label }
                    ).joinToString(" • "), style = MaterialTheme.typography.bodyMedium)
                }
            }
            when {
                isLoading -> item { CircularProgressIndicator(); Text("Loading care history...") }
                loadError != null -> item {
                    Text(loadError, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry) { Text("Retry") }
                }
                visible.isEmpty() -> item {
                    Text(if (applied.isActive) "No history matches these filters. Clear filters or choose other dates or activity types."
                        else "No care history yet.")
                }
                else -> items(visible, key = { it.id }) { entry -> CareHistoryCard(entry) }
            }
        }
    }
}

@Composable
private fun CareHistoryCard(entry: CareHistoryEntry) {
    val date = remember(entry.occurredAtMillis) {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(entry.occurredAtMillis))
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(entry.summary, style = MaterialTheme.typography.titleMedium)
            Text(if (entry.type == CareActivityType.MEDICATION) "Scheduled dose: $date" else date)
        }
    }
}
