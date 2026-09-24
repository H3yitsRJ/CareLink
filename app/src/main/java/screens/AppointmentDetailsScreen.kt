// Displays the selected appointment and emits edit, cancellation, and follow-up callbacks. MainActivity
// changes cancellation status locally; appointment reminder cleanup is not implemented.

package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus
import kotlinx.coroutines.delay

@Composable
fun AppointmentDetailsScreen(
    appointment: Appointment? = null,
    isLoading: Boolean = false,
    successMessage: String? = null,
    onEdit: () -> Unit = {},
    onCancelAppointment: (Appointment) -> Unit = {},
    onGenerateFollowUp: (Appointment) -> Unit = {},
    onBack: () -> Unit = {}
) {
    // Cancellation needs its own saved state so rotation does not bypass the confirmation step.
    var confirmingCancellation by rememberSaveable(appointment?.id) { mutableStateOf(false) }
    var showCancellationMessage by remember(appointment?.id) { mutableStateOf(false) }
    LaunchedEffect(showCancellationMessage) {
        if (showCancellationMessage) {
            delay(2_000)
            showCancellationMessage = false
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            // Long notes and enlarged text must not push the navigation controls off screen.
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Appointment details", style = MaterialTheme.typography.headlineLarge)
            when {
                isLoading -> StateMessage("Loading appointment")
                appointment == null -> StateMessage("This appointment could not be found.", true)
                else -> {
                    CareCard(appointment.title) {
                        Text("${appointment.date} at ${appointment.time}")
                        if (appointment.provider.isNotBlank()) Text("Provider: ${appointment.provider}")
                        if (appointment.location.isNotBlank()) Text("Location: ${appointment.location}")
                        if (appointment.notes.isNotBlank()) Text(appointment.notes)
                        Text("Status: ${appointment.status.name.lowercase().replaceFirstChar(Char::uppercase)}")
                    }
                    if (appointment.status != AppointmentStatus.CANCELLED) {
                        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit appointment") }
                        OutlinedButton(onClick = { onGenerateFollowUp(appointment) }, modifier = Modifier.fillMaxWidth()) { Text("Generate follow-up task") }
                        TextButton(onClick = { confirmingCancellation = true }, modifier = Modifier.fillMaxWidth()) { Text("Cancel appointment") }
                    }
                }
            }
            if (successMessage != null) Text(successMessage, color = MaterialTheme.colorScheme.tertiary)
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to appointments") }
        }
        if (showCancellationMessage) {
            Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }) {
                Text("Appointment Cancelled")
            }
        }
    }
    // Confirmation delegates to the parent; the current callback only updates in-memory status.
    if (confirmingCancellation && !isLoading && appointment != null && appointment.status != AppointmentStatus.CANCELLED) AlertDialog(
        onDismissRequest = { confirmingCancellation = false },
        title = { Text("Cancel appointment?") },
        text = { Text("The appointment will remain in CareLink with a Cancelled status.") },
        confirmButton = { TextButton(onClick = {
            confirmingCancellation = false
            onCancelAppointment(appointment)
            showCancellationMessage = true
        }) { Text("Cancel appointment") } },
        dismissButton = { TextButton(onClick = { confirmingCancellation = false }) { Text("Keep appointment") } }
    )
}
