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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.CareActivityType
import com.example.carelink.model.CareHistoryEntry
import com.example.carelink.model.CareHistoryFilter

@Composable
fun CareHistoryScreen(entries: List<CareHistoryEntry> = emptyList()) {
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var selectedTypes by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Draft values do not change the list until the patient taps Apply filters.
    var applied by rememberSaveable { mutableStateOf(CareHistoryFilter()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    // Validation should catch bad ranges first, but this guard keeps rendering safe.
    val visible = runCatching { applied.apply(entries) }.getOrDefault(emptyList())
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Care history")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(start, { start = it }, label = { Text("Start timestamp") }, modifier = Modifier.weight(1f))
            OutlinedTextField(end, { end = it }, label = { Text("End timestamp") }, modifier = Modifier.weight(1f))
        }
        CareActivityType.entries.forEach { type ->
            FilterChip(
                selected = type.name in selectedTypes,
                onClick = { selectedTypes = if (type.name in selectedTypes) selectedTypes - type.name else selectedTypes + type.name },
                label = { Text(type.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)) }
            )
        }
        Button(onClick = {
            val filter = CareHistoryFilter(start.toLongOrNull(), end.toLongOrNull(), selectedTypes.mapNotNull { name -> CareActivityType.entries.firstOrNull { it.name == name } }.toSet())
            error = filter.validate()
            if (error == null) applied = filter
        }, modifier = Modifier.fillMaxWidth()) { Text("Apply filters") }
        OutlinedButton(onClick = { start = ""; end = ""; selectedTypes = emptySet(); applied = CareHistoryFilter(); error = null }, modifier = Modifier.fillMaxWidth()) { Text("Clear filters") }
        if (error != null) StateMessage(error!!, true)
        if (visible.isEmpty()) StateMessage(if (entries.isEmpty()) "No care history yet." else "No care history matches these filters.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visible, key = { it.id }) { entry -> CareCard(entry.type.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)) { Text(entry.summary) } }
        }
    }
}
