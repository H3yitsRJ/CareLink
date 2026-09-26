package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.example.carelink.data.DoseHistoryItem
import com.example.carelink.data.DoseHistoryStore
import com.example.carelink.model.DoseStatus

import com.google.firebase.auth.FirebaseAuth

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import navigation.BottomNavBar
import navigation.BottomNavDestination

private enum class HistoryTab(val label: String) {
    ALL("All"),
    MEDS("Meds"),
    VISITS("Visits"),
    TASKS("Tasks")
}

private enum class DateChoice(val label: String) {
    ALL("All dates"),
    TODAY("Today"),
    YESTERDAY("Yesterday")
}

@Composable
fun CareHistoryScreen(
    onBack: () -> Unit = {},
    onNavigate: (BottomNavDestination) -> Unit = {}
) {
    val patientId = FirebaseAuth.getInstance().currentUser?.uid
    val store = remember { DoseHistoryStore() }

    var records by remember(patientId) { mutableStateOf<List<DoseHistoryItem>>(emptyList()) }
    var loading by remember(patientId) { mutableStateOf(true) }
    var loadError by remember(patientId) { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var selectedTab by rememberSaveable { mutableStateOf(HistoryTab.ALL) }
    var selectedDate by rememberSaveable { mutableStateOf(DateChoice.ALL) }

    LaunchedEffect(patientId, refresh) {
        if (patientId == null) {
            loading = false
            loadError = "Sign in to view your care history."
        } else {
            loading = true
            loadError = null

            store.load(patientId) { result ->
                result.onSuccess {
                    records = it
                    loading = false
                }.onFailure {
                    loadError = "We couldn't load your dose history."
                    loading = false
                }
            }
        }
    }

    val visibleRecords = records.filter {
        matchesDate(
            it.record.scheduledTimeMillis,
            selectedDate
        )
    }



    Scaffold(
        bottomBar = {
            BottomNavBar(
                selectedDestination = BottomNavDestination.Medications,
                onDestinationSelected = onNavigate
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack) {
                Text("‹ Back")
            }

            Text(
                text = "Care History",
                style = MaterialTheme.typography.headlineMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HistoryTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) }
                    )
                }
            }

            if (selectedTab == HistoryTab.ALL ||
                selectedTab == HistoryTab.MEDS
            ) {
                Text(
                    text = "Filter by date",
                    style = MaterialTheme.typography.titleSmall
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DateChoice.entries.forEach { choice ->
                        FilterChip(
                            selected = selectedDate == choice,
                            onClick = { selectedDate = choice },
                            label = { Text(choice.label) }
                        )
                    }
                }

                when {
                    loading -> {
                        CircularProgressIndicator()
                        Text("Loading dose history...")
                    }

                    loadError != null -> {
                        StateMessage(loadError!!, isError = true)
                        Button(onClick = { refresh++ }) {
                            Text("Retry")
                        }
                    }

                    visibleRecords.isEmpty() -> {
                        StateMessage(
                            if (records.isEmpty()) {
                                "No dose history yet. Record a dose from Medication details."
                            } else {
                                "No doses were recorded for this date."
                            }
                        )
                    }

                    else -> {
                        val grouped = visibleRecords
                            .sortedWith(
                                compareByDescending<DoseHistoryItem> {
                                    it.record.scheduledTimeMillis
                                }.thenByDescending {
                                    it.record.completionTimeMillis ?: 0L
                                }
                            )
                            .groupBy {
                                formatDate(it.record.scheduledTimeMillis)
                            }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            grouped.forEach { (date, doses) ->
                                item(key = "date-$date") {
                                    Text(
                                        text = date,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                items(
                                    items = doses,
                                    key = { it.record.id }
                                ) { item ->
                                    DoseHistoryCard(item)
                                }
                            }
                        }
                    }
                }
            } else {
                StateMessage(
                    if (selectedTab == HistoryTab.VISITS) {
                        "Visit history is not connected yet."
                    } else {
                        "Task history is not connected yet."
                    }
                )
            }
        }
    }
}

@Composable
private fun DoseHistoryCard(item: DoseHistoryItem) {

    val statusColor: Color = when (item.record.status) {
        DoseStatus.TAKEN -> MaterialTheme.colorScheme.primary
        DoseStatus.MISSED -> MaterialTheme.colorScheme.error
        DoseStatus.DELAYED -> MaterialTheme.colorScheme.tertiary
        DoseStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
        DoseStatus.SCHEDULED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusLabel = item.record.status.name
        .lowercase()
        .replaceFirstChar(Char::uppercase)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, statusColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.medicationName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text("Dosage: ${item.dosage}")
            Text(
                "Scheduled: ${
                    formatTime(item.record.scheduledTimeMillis)
                }"
            )
            Text(
                text = statusLabel,
                color = statusColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun matchesDate(
    timestamp: Long,
    choice: DateChoice
): Boolean {
    if (choice == DateChoice.ALL) return true

    val selectedDay = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    val targetDay = Calendar.getInstance().apply {
        if (choice == DateChoice.YESTERDAY) {
            add(Calendar.DAY_OF_YEAR, -1)
        }
    }

    return selectedDay.get(Calendar.YEAR) ==
            targetDay.get(Calendar.YEAR) &&
            selectedDay.get(Calendar.DAY_OF_YEAR) ==
            targetDay.get(Calendar.DAY_OF_YEAR)
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat(
        "EEEE, MMM d, yyyy",
        Locale.getDefault()
    ).format(Date(timestamp))

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat(
        "h:mm a",
        Locale.getDefault()
    ).format(Date(timestamp))