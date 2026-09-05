package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Medication
import navigation.BottomNavBar
import navigation.BottomNavDestination

@Composable
fun MedicationsScreen(
    medications: List<Medication> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    onAdd: () -> Unit = {},
    onSelect: (Medication) -> Unit = {},
    onNavigate: (BottomNavDestination) -> Unit = {}
) {
    // Scaffold keeps the team's shared navigation outside the scrolling list.
    Scaffold(bottomBar = { BottomNavBar(BottomNavDestination.Medications, onNavigate) }) { innerPadding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(innerPadding).padding(16.dp)
        ) {
            Text("Medications", style = MaterialTheme.typography.headlineLarge)
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) { Text("Add medication") }
            when {
                isLoading -> StateMessage("Loading medications")
                error != null -> StateMessage(error, true)
                medications.isEmpty() -> StateMessage("No medications yet. Add a medication to create its schedule.")
                // LazyColumn only composes visible medication rows as the list grows.
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(medications, key = { it.id }) { medication ->
                        Card(Modifier.fillMaxWidth().clickable { onSelect(medication) }) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(medication.name, style = MaterialTheme.typography.titleMedium)
                                Text("${medication.strength} · ${medication.dose}")
                                Text("Next: ${medication.reminderTimes.firstOrNull() ?: "No reminder"}")
                            }
                        }
                    }
                }
            }
        }
    }
}
