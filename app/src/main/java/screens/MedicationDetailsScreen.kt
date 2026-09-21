package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Medication
import navigation.BottomNavBar
import navigation.BottomNavDestination

@Composable
fun MedicationDetailsScreen(
    medication: Medication? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onEdit: (Medication) -> Unit = {},
    onRemove: (Medication) -> Unit = {},
    onBack: () -> Unit = {},
    onNavigate: (BottomNavDestination) -> Unit = {},
    canEdit: Boolean = true,
    canRemove: Boolean = true,
    showNavigation: Boolean = true
) {
    var showRemoveDialog by remember(medication?.id) { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (showNavigation) BottomNavBar(
                BottomNavDestination.Medications,
                onNavigate
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            TextButton(
                onClick = onBack
            ) {
                Text(
                    text = "‹",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Medication details",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error)

            when {
                isLoading -> MedicationLoadingState()

                medication == null -> MedicationMissingState (onBack = onBack)

                else -> MedicationDetailsContent(
                    medication = medication,
                    canEdit = canEdit,
                    canRemove = canRemove,
                    onEdit = { onEdit(medication) },
                    onRemove = { showRemoveDialog = true }
                )
            }
        }
    }

    if (showRemoveDialog && medication != null && canRemove) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove medication?") },
            text = { Text ("Are you sure you want to remove " + "${medication.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveDialog = false
                        onRemove(medication)
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MedicationDetailsContent(
    medication: Medication,
    canEdit: Boolean,
    canRemove: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onBackground,
                shape = RoundedCornerShape(36.dp)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = medication.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MedicationBadge(medication.strength)
                MedicationBadge(medication.dose)
                MedicationBadge(medication.frequency)

                medication.reminderTimes.forEach { reminderTime -> MedicationBadge(formatReminderTime(reminderTime)) }

                MedicationBadge(if (medication.active) "Scheduled" else "Inactive" )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Medication",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        medication.updatedById?.let { editor ->
            Text(if (editor == medication.patientId) "Last edited by patient" else "Last edited by caregiver: $editor",
                style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = medicationInstructions(medication),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(28.dp))

        if (canEdit) Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Edit medication")
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (canRemove) OutlinedButton(
            onClick = onRemove,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Remove medication")
        }
    }
}

@Composable
private fun MedicationBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.surface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 5.dp
            )
        )
    }
}

@Composable
private fun MedicationLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Loading medication...")
        }
    }
}

@Composable
private fun MedicationMissingState(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Medication not found",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "This medication may have been removed or is no longer available.",
            textAlign = TextAlign.Center
        )

        Button(onClick = onBack) {
            Text("Back to medications")
        }
    }
}

private fun medicationInstructions(
    medication: Medication
): String {
    if (medication.instructions.isNotBlank()) {
        return medication.instructions
    }

    val nextDose = medication.reminderTimes
        .firstOrNull()
        ?.let(::formatReminderTime)
        ?: "the scheduled time"

    return "Your next scheduled dose is at $nextDose. " +
            "Follow the instructions on your medication label."
}

private fun formatReminderTime(time: String): String {
    val parts = time.split(":")
    if (parts.size != 2) return time

    val hour = parts[0].toIntOrNull() ?: return time
    val minute = parts[1].toIntOrNull() ?: return time
    val suffix = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }

    return "%d:%02d %s".format(displayHour, minute, suffix)
}
