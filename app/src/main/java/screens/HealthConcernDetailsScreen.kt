package com.example.carelink.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.*

@Composable
fun HealthConcernDetailsScreen(
    concern: HealthConcern? = null, isLoading: Boolean = false, error: String? = null,
    appointments: List<Appointment> = emptyList(), appointmentsLoading: Boolean = false,
    appointmentsError: String? = null, isSaving: Boolean = false, saveError: String? = null,
    onStatusChange: (ConcernStatus) -> Unit = {}, onLinkAppointment: (String?) -> Unit = {},
    onRetry: () -> Unit = {}, onBack: () -> Unit = {}
) {
    var choosingAppointment by rememberSaveable(concern?.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Health concern details", style = MaterialTheme.typography.headlineLarge)
        when {
            isLoading -> Text("Loading health concern...")
            error != null -> { Text(error); Button(onClick = onRetry) { Text("Retry") } }
            concern == null -> Text("This health concern could not be found.")
            else -> {
                Text(concern.title, style = MaterialTheme.typography.titleLarge)
                Text("Description: ${concern.description}")
                Text("Severity: ${concern.severity.name.lowercase().replaceFirstChar(Char::uppercase)}")
                Text("Date: ${concern.recordedDate}")
                Text("Status: ${concern.status.name.lowercase().replaceFirstChar(Char::uppercase)}")
                val linked = appointments.find { it.id == concern.appointmentId }
                Text(when {
                    concern.appointmentId == null -> "Linked appointment: None"
                    appointmentsLoading -> "Loading linked appointment..."
                    appointmentsError != null -> "Linked appointment: ${concern.appointmentId} (unavailable)"
                    linked == null -> "Linked appointment: ${concern.appointmentId} (no longer available)"
                    else -> "Linked appointment: ${linked.title} — ${linked.date} at ${linked.time}"
                })
                if (appointmentsError != null) { Text(appointmentsError); TextButton(onClick = onRetry) { Text("Retry appointments") } }
                Button(enabled = !isSaving, onClick = {
                    onStatusChange(if (concern.status == ConcernStatus.ACTIVE) ConcernStatus.DISCUSSED else ConcernStatus.ACTIVE)
                }) { Text(if (concern.status == ConcernStatus.ACTIVE) "Mark as discussed" else "Mark as active") }
                OutlinedButton(enabled = !isSaving, onClick = { choosingAppointment = true }) { Text("Link to Appointment") }
                if (isSaving) Text("Saving changes...")
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        OutlinedButton(onClick = onBack) { Text("Back to health concerns") }
    }
    if (choosingAppointment && concern != null) AlertDialog(
        onDismissRequest = { choosingAppointment = false },
        title = { Text("Choose an appointment") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                when {
                    appointmentsLoading -> Text("Loading appointments...")
                    appointmentsError != null -> Text(appointmentsError)
                    appointments.isEmpty() -> Text("No appointments available. Create an appointment first.")
                    else -> appointments.forEach { appointment ->
                        TextButton(enabled = !isSaving, onClick = {
                            choosingAppointment = false; onLinkAppointment(appointment.id)
                        }) { Text("${appointment.title} — ${appointment.date} at ${appointment.time}") }
                    }
                }
                if (concern.appointmentId != null) TextButton(enabled = !isSaving, onClick = {
                    choosingAppointment = false; onLinkAppointment(null)
                }) { Text("Remove appointment link") }
            }
        },
        confirmButton = { TextButton(onClick = { choosingAppointment = false }) { Text("Cancel") } }
    )
}
