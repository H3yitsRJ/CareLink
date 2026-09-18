// Appointment list with loading, error, empty, and populated states. MainActivity supplies records and
// handles row selection, creation, and bottom navigation.

package com.example.carelink.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Appointment
import navigation.BottomNavBar
import navigation.BottomNavDestination

@Composable
fun AppointmentsScreen(
    appointments: List<Appointment> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    onAdd: () -> Unit = {},
    onSelect: (Appointment) -> Unit = {},
    onNavigate: (BottomNavDestination) -> Unit = {}
) {
    Scaffold(bottomBar = { BottomNavBar(BottomNavDestination.Appointments, onNavigate) }) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Appointments", style = MaterialTheme.typography.headlineLarge)
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add appointment") }
            when {
                isLoading -> StateMessage("Loading appointments")
                error != null -> StateMessage(error, true)
                appointments.isEmpty() -> StateMessage("No appointments yet.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(appointments, key = { it.id }) { appointment ->
                        CareCard(appointment.title) {
                            Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { onSelect(appointment) }) {
                                Text("${appointment.date} at ${appointment.time}")
                                if (appointment.provider.isNotBlank()) Text(appointment.provider)
                                Text(appointment.status.name.lowercase().replaceFirstChar(Char::uppercase))
                            }
                        }
                    }
                }
            }
        }
    }
}
