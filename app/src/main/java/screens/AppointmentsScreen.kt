package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import navigation.BottomNavBar
import navigation.BottomNavDestination
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import com.example.carelink.model.Appointment

@Composable
fun AppointmentsScreen(
    appointments: List<Appointment> = emptyList(),
    onNavigate: (BottomNavDestination) -> Unit = {},
    onAddAppointment: () -> Unit = {}
) {
    Scaffold(bottomBar = {
        BottomNavBar(BottomNavDestination.Appointments, onNavigate)
    }) { innerPadding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(innerPadding).padding(16.dp)
        ) {
            Text("Appointments", style = MaterialTheme.typography.headlineLarge)
            if (appointments.isEmpty()) {
                Text(
                    "No appointments yet.",
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                appointments.forEach { appointment ->
                    Text(
                        text = "${appointment.title} - ${appointment.date} at ${appointment.time}",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            Button(
                onClick = onAddAppointment,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text("Add Appointment")
            }
        }

    }
}
