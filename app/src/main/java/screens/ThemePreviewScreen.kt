package com.example.carelink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding

@Composable
fun ThemePreviewScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "CareLink",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Your Care, Connected",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Manage your medications, appointments, care tasks, " +
                    "and health information in one place.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Upcoming Appointment",
            style = MaterialTheme.typography.labelLarge
        )

        Button(
            onClick = { }
        ) {
            Text(
                text = "View Appointment",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}