package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import navigation.BottomNavBar
import navigation.BottomNavDestination
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.LaunchedEffect

// The dashboard accepts prepared summaries and stays independent of Firebase queries.
data class DashboardSummary(val medication: String, val appointment: String, val healthConcern: String, val careTask: String)

@Composable
fun DashboardScreen(
    fullName: String = "",
    summary: DashboardSummary? = null,
    isLoading: Boolean = false,
    error: String? = null,
    unreadNotificationCount: Int = 0,
    onOpen: (String) -> Unit = {},
    onNavigate: (BottomNavDestination) -> Unit = {}
) {
    val scrollState = rememberScrollState()


    LaunchedEffect(Unit) {
        scrollState.scrollTo(0)
    }

    // Keep the team's bottom navigation while using data-driven summary cards.
    Scaffold(bottomBar = { BottomNavBar(BottomNavDestination.Home, onNavigate) }) { innerPadding ->
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState).padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (fullName.isBlank()) "Today"
                    else "Good morning, $fullName",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f)
                )
                BadgedBox(
                    badge = {
                        if (unreadNotificationCount > 0) {
                            Badge {
                                Text(
                                    text = unreadNotificationCount.toString()
                                )
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = { onOpen("notifications") }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Open notifications"
                        )
                    }
                }


            }

            when {
                isLoading -> StateMessage("Loading your care summary")
                error != null -> StateMessage(error, true)
                summary == null -> StateMessage("Your care summary is not available.")
                else -> listOf(
                    Triple("Medications", summary.medication, "medications"),
                    Triple("Appointments", summary.appointment, "appointments"),
                    Triple("Health concerns", summary.healthConcern, "health-concerns"),
                    Triple("Care tasks", summary.careTask, "care-tasks")
                ).forEach { (title, body, destination) ->
                    CareCard(title) {
                        Text(body)
                        TextButton(onClick = { onOpen(destination) }) { Text("View $title") }
                    }
                }
            }
        }
    }
}
