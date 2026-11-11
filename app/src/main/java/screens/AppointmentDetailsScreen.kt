package com.example.carelink.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus

@Composable
fun AppointmentDetailsScreen(
    appointment: Appointment? = null,
    isLoading: Boolean = false,
    successMessage: String? = null,
    onEdit: () -> Unit = {},
    onCancelAppointment: (Appointment) -> Unit = {},
    onGenerateFollowUp: (Appointment) -> Unit = {},
    onBack: () -> Unit = {},
    canManage: Boolean = true
) {
    // Cancellation needs its own saved state so rotation does not bypass the confirmation step.
    var confirmingCancellation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(canManage) {
        if (!canManage) {
            confirmingCancellation = false
        }
    }

    BackHandler {
        if (confirmingCancellation) {
            confirmingCancellation = false
        } else {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to appointments"
                )
            }

            Text(
                text = "Appointment Details",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        when {
            isLoading -> LoadingState("Loading appointment")
            appointment == null -> StateMessage("This appointment could not be found.", true)

            else -> {
                CareCard(appointment.title) {
                    val statusLabel = appointment.status.name
                        .lowercase()
                        .replaceFirstChar(Char::uppercase)

                    val statusColor = if (appointment.status == AppointmentStatus.CANCELLED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (appointment.provider.isNotBlank()) {
                            Text(
                                text = appointment.provider,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = statusColor,
                            border = BorderStroke(1.dp, statusColor)
                        ) {
                            Text(
                                text = statusLabel,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                CareCard("Visit information") {
                    AppointmentInfoRow("Date", appointment.date)
                    AppointmentInfoRow("Time", appointment.time)

                    if (appointment.location.isNotBlank()) {
                        AppointmentInfoRow("Location", appointment.location)
                    }
                }

                if (appointment.notes.isNotBlank()) {
                    CareCard("Notes & questions") {
                        Text(appointment.notes)
                    }
                }

                if (!canManage) {
                    Text("View-only access. You cannot change this appointment.")
                } else if (appointment.status != AppointmentStatus.CANCELLED) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val actionShape = RoundedCornerShape(8.dp)
                        val cancelColor = MaterialTheme.colorScheme.error

                        OutlinedButton(
                            onClick = {
                                if (canManage) onGenerateFollowUp(appointment)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = actionShape,
                        ) {
                            Text("Generate follow-up task")
                        }

                        Button(
                            onClick = {
                                if (canManage) onEdit()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = actionShape

                        ) {
                            Text("Edit appointment")
                        }

                        OutlinedButton(
                            onClick = {
                                if (canManage) confirmingCancellation = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = actionShape,
                            border = BorderStroke(1.dp, cancelColor),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = cancelColor
                            )
                        ) {
                            Text("Cancel Appointment")
                        }

                    }
                }
            }
        }

        if (successMessage != null) {
            Text(successMessage, color = MaterialTheme.colorScheme.tertiary)
        }
    }

    // The parent callback performs storage and reminder cleanup after confirmation.
    if (
        confirmingCancellation &&
        appointment != null &&
        canManage &&
        appointment.status != AppointmentStatus.CANCELLED
    ) {
        AlertDialog(
            onDismissRequest = { confirmingCancellation = false },
            title = { Text("Cancel appointment?") },
            text = {
                Text("The appointment will remain in CareLink with a Cancelled status. Future reminders will stop.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingCancellation = false
                        if (canManage && appointment.status != AppointmentStatus.CANCELLED) {
                            onCancelAppointment(appointment)
                        }
                    }
                ) {
                    Text("Cancel appointment")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingCancellation = false }) {
                    Text("Keep appointment")
                }
            }
        )
    }
}

@Composable
private fun AppointmentInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}