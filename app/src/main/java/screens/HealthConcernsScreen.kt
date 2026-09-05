package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.carelink.model.ConcernSeverity
import com.example.carelink.model.ConcernStatus
import com.example.carelink.model.HealthConcern

@Composable
fun HealthConcernsScreen(
    concerns: List<HealthConcern> = emptyList(), isLoading: Boolean = false, error: String? = null,
    onAdd: () -> Unit = {}, onSelect: (HealthConcern) -> Unit = {}
) {
    var status by rememberSaveable { mutableStateOf<ConcernStatus?>(null) }
    var severity by rememberSaveable { mutableStateOf<ConcernSeverity?>(null) }
    // Filters are combined so patients can narrow by status and severity at the same time.
    val visible = concerns.filter { (status == null || it.status == status) && (severity == null || it.severity == severity) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Health concerns", style = MaterialTheme.typography.headlineLarge)
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add concern") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = status == null, onClick = { status = null }, label = { Text("All") })
            FilterChip(selected = status == ConcernStatus.ACTIVE, onClick = { status = ConcernStatus.ACTIVE }, label = { Text("Active") })
            FilterChip(selected = status == ConcernStatus.DISCUSSED, onClick = { status = ConcernStatus.DISCUSSED }, label = { Text("Discussed") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConcernSeverity.entries.forEach { item ->
                FilterChip(selected = severity == item, onClick = { severity = if (severity == item) null else item }, label = { Text(item.name.lowercase().replaceFirstChar(Char::uppercase)) })
            }
        }
        when {
            isLoading -> StateMessage("Loading health concerns")
            error != null -> StateMessage(error, true)
            concerns.isEmpty() -> StateMessage("No health concerns recorded.")
            visible.isEmpty() -> StateMessage("No health concerns match these filters.")
            // Higher-severity concerns come first without hiding the text severity label.
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visible.sortedByDescending { it.severity.ordinal }, key = { it.id }) { concern ->
                    CareCard(concern.title) {
                        Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { onSelect(concern) }) {
                            Text("${concern.severity.name.lowercase().replaceFirstChar(Char::uppercase)} severity")
                            Text("${concern.recordedDate} · ${concern.status.name.lowercase().replaceFirstChar(Char::uppercase)}")
                        }
                    }
                }
            }
        }
    }
}
