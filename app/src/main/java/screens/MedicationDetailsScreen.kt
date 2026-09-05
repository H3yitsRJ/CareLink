package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Medication

@Composable
fun MedicationDetailsScreen(medication: Medication? = null, isLoading: Boolean = false, onEdit: () -> Unit = {}, onBack: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Medication details", style = MaterialTheme.typography.headlineLarge)
        // Loading and missing-record states are separate so the screen never flashes false data.
        when {
            isLoading -> Text("Loading medication")
            medication == null -> Text("This medication could not be found.")
            else -> {
                Text(medication.name, style = MaterialTheme.typography.titleLarge)
                Text("${medication.strength} · ${medication.dose}")
                Text("${medication.frequency} at ${medication.reminderTimes.joinToString()}")
                if (medication.instructions.isNotBlank()) Text(medication.instructions)
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit medication") }
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to medications") }
    }
}
