// Collects a title, date, and severity, then submits a HealthConcern. MainActivity appends it to the
// current in-memory list.

package com.example.carelink.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.ConcernSeverity
import com.example.carelink.model.HealthConcern

@Composable
fun AddHealthConcernScreen(patientId: String = "", onSave: (HealthConcern) -> Unit = {}, onCancel: () -> Unit = {}) {
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var severity by rememberSaveable { mutableStateOf(ConcernSeverity.MEDIUM) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Add health concern", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(title, { title = it }, label = { Text("Concern") }, modifier = Modifier.fillMaxWidth(), isError = attempted && title.isBlank())
        OutlinedTextField(date, { date = it }, label = { Text("Date") }, placeholder = { Text("YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
        Text("Severity")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConcernSeverity.entries.forEach { option ->
                FilterChip(selected = severity == option, onClick = { severity = option }, label = { Text(option.name.lowercase().replaceFirstChar(Char::uppercase)) })
            }
        }
        if (attempted && (title.isBlank() || date.isBlank())) StateMessage("Enter the concern and date.", true)
        Button(onClick = {
            attempted = true
            if (title.isNotBlank() && date.isNotBlank()) onSave(HealthConcern("concern-${System.currentTimeMillis()}", patientId, title.trim(), severity, date.trim()))
        }, modifier = Modifier.fillMaxWidth()) { Text("Save concern") }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
