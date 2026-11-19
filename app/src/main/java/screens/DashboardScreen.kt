package com.example.carelink.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import com.example.carelink.model.DoseStatus


// The dashboard accepts prepared summaries and stays independent of Firebase queries.
data class DashboardSummary(
    val medication: String,
    val appointment: String,
    val healthConcern: String,
    val careTask: String,
    val openTaskTitles: List<String> = emptyList()
)

@Composable
fun DashboardScreen(
    fullName: String = "",
    summary: DashboardSummary? = null,
    isLoading: Boolean = false,
    error: String? = null,
    unreadNotificationCount: Int = 0,
    onOpen: (String) -> Unit = {},
    onNavigate: (BottomNavDestination) -> Unit = {},
    doseActionsEnabled: Boolean = false,
    isSavingDose: Boolean = false,
    recordedDoseStatus: DoseStatus? = null,
    doseMessage: String? = null,
    doseError: String? = null,
    onRecordDose: (DoseStatus) -> Unit = {}
) {
    val scrollState = rememberScrollState()


    LaunchedEffect(Unit) {
        scrollState.scrollTo(0)
    }

    // Keep the team's bottom navigation while using data-driven summary cards.
    Scaffold(bottomBar = { BottomNavBar(BottomNavDestination.Home, onNavigate) }) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (fullName.isBlank()) "Today"
                    else "Hello, $fullName",
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

            Text(
                text = "Here is what needs your attention today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                isLoading -> LoadingState("Loading your care summary")
                error != null -> StateMessage(error, true)
                summary == null -> StateMessage("Your care summary is not available.")

                else -> listOf(
                    Triple("Medications", summary.medication, "medications"),
                    Triple("Upcoming appointment", summary.appointment, "appointments"),
                    Triple("Health concerns", summary.healthConcern, "health-concerns"),
                    Triple("Care tasks", summary.careTask, "care-tasks")
                ).forEach { (title, body, destination) ->
                    DashboardCareCard(title) {
                        val isEmpty = body.isBlank()

                        if (destination == "care-tasks") {
                            summary.openTaskTitles.forEach { taskTitle ->
                                OutlinedButton(
                                    onClick = { onOpen(destination) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = taskTitle,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("›")
                                    }
                                }
                            }

                            if (body.isNotBlank()) {
                                Text(
                                    text = body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (body.startsWith("Attention:")) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }

                        } else if (isEmpty) {
                            Text(
                                text = "No ${title.lowercase()} to display yet.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else if (destination == "medications") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    body.lines().forEachIndexed { index, line ->
                                        Text(
                                            text = line,
                                            style = if (index == 0) {
                                                MaterialTheme.typography.titleMedium
                                            } else {
                                                MaterialTheme.typography.bodyMedium
                                            },
                                            color = if (line.startsWith("Attention:")) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    DoseStatus.TAKEN to "Taken",
                                    DoseStatus.MISSED to "Missed",
                                    DoseStatus.DELAYED to "Delayed"
                                ).forEach { (status, label) ->
                                    OutlinedButton(
                                        onClick = { onRecordDose(status) },
                                        enabled = doseActionsEnabled && !isSavingDose,
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(
                                            horizontal = 4.dp,
                                            vertical = 8.dp
                                        ),
                                    ) {
                                        Text(
                                            text = if (recordedDoseStatus == status) "✓ $label" else label,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            if (isSavingDose) {
                                Text("Saving dose…")
                            }

                            doseMessage?.let { message ->
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            doseError?.let { message ->
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                        } else if (destination == "health-concerns") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HealthAndSafety,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    body.lines().forEach { line ->
                                        if (line.startsWith("Attention:")) {
                                            Text(
                                                text = line,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        } else {
                                            line.split(" • ").forEach { concernTitle ->
                                                Text(
                                                    text = concernTitle,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                        } else if (
                            (title == "Medications" ||
                                    title == "Care tasks" ||
                                    title == "Health concerns") &&
                            body.contains("Attention:")
                        ) {
                            val parts = body.split("\n", limit = 2)

                            Text(
                                text = parts[0],
                                style = MaterialTheme.typography.bodyLarge
                            )

                            if (parts.size > 1) {
                                Text(
                                    text = parts[1],
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else if (destination == "appointments") {
                            val lines = body.lines()

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = lines.first(),
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    lines.drop(1).forEach { line ->
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(body)
                        }

                        if (destination == "appointments") {
                            Button(
                                onClick = { onOpen(destination) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("View appointments")
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    if (isEmpty && destination == "medications") {
                                        onOpen("add-medication")
                                    } else {
                                        onOpen(destination)
                                    }
                                }
                            ) {
                                Text(
                                    if (isEmpty && destination != "care-tasks") "Get started"
                                    else "View $title"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCareCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            content()
        }
    }
}