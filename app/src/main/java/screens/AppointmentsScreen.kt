package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus
import navigation.BottomNavBar
import navigation.BottomNavDestination

@Composable
fun AppointmentsScreen(
    appointments: List<Appointment> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    onAddAppointment: () -> Unit = {},
    onAppointmentSelected: (Appointment) -> Unit = {},
    onEditAppointment: (Appointment) -> Unit = {},
    onRetry: () -> Unit = {},
    onNavigate: (BottomNavDestination) -> Unit = {}
) {
    val upcomingAppointments = appointments
        .filter { it.status == AppointmentStatus.SCHEDULED }
        .sortedWith(compareBy<Appointment> { it.date }.thenBy { it.time })

    val pastAppointments = appointments
        .filter { it.status != AppointmentStatus.SCHEDULED }
        .sortedWith(
            compareByDescending<Appointment> { it.date }
                .thenByDescending { it.time }
        )

    Scaffold(
        bottomBar = {
            BottomNavBar(
                BottomNavDestination.Appointments,
                onNavigate
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Appointments",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = "View and manage your upcoming visits.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Button(
                onClick = onAddAppointment,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp)
            ) {
                Text("Add appointment")
            }

            if (successMessage != null) {
                Text(
                    text = successMessage,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    AppointmentErrorState(
                        message = errorMessage,
                        onRetry = onRetry
                    )
                }

                appointments.isEmpty() -> {
                    AppointmentEmptyState(
                        onAddAppointment = onAddAppointment
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (upcomingAppointments.isNotEmpty()) {
                            item {
                                SectionHeading("Upcoming Appointments")
                            }

                            items(upcomingAppointments) { appointment ->
                                AppointmentCard(
                                    appointment = appointment,
                                    showEditButton = true,
                                    onSelect = {
                                        onAppointmentSelected(appointment)
                                    },
                                    onEdit = {
                                        onEditAppointment(appointment)
                                    }
                                )
                            }
                        }

                        if (pastAppointments.isNotEmpty()) {
                            item {
                                SectionHeading(
                                    title = "Past Appointments",
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }

                            items(pastAppointments) { appointment ->
                                AppointmentCard(
                                    appointment = appointment,
                                    showEditButton = false,
                                    onSelect = {
                                        onAppointmentSelected(appointment)
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    showEditButton: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = appointment.title,
                    style = MaterialTheme.typography.titleMedium
                )

                if (appointment.provider.isNotBlank()) {
                    Text(
                        text = appointment.provider,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "${appointment.date} • ${appointment.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = if (appointment.location.isBlank()) {
                        "Location not provided"
                    } else {
                        appointment.location
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = appointment.status.displayName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (showEditButton) {
                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(onClick = onEdit) {
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
private fun AppointmentEmptyState(
    onAddAppointment: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "No appointments yet",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Add an appointment to keep track of upcoming visits.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(onClick = onAddAppointment) {
            Text("Add your first appointment")
        }
    }
}

@Composable
private fun AppointmentErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedButton(onClick = onRetry) {
            Text("Try again")
        }
    }
}

private fun AppointmentStatus.displayName(): String {
    return name
        .lowercase()
        .replaceFirstChar { character ->
            character.uppercase()
        }
}