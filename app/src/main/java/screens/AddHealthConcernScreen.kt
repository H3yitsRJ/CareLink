package com.example.carelink.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.carelink.data.HealthConcernRepository
import com.example.carelink.model.ConcernSeverity
import com.example.carelink.model.ConcernStatus
import com.example.carelink.model.HealthConcern
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AddHealthConcernScreen(
    patientId: String,
    repository: HealthConcernRepository,
    onSaved: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val context = LocalContext.current

    var title by rememberSaveable(patientId) { mutableStateOf("") }
    var notes by rememberSaveable(patientId) { mutableStateOf("") }
    var severityName by rememberSaveable(patientId) { mutableStateOf(ConcernSeverity.MEDIUM.name) }
    var dateMillis by rememberSaveable(patientId) { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var attemptedSave by rememberSaveable(patientId) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val alive = remember { mutableStateOf(true) }
    val savedCallback by rememberUpdatedState(onSaved)

    DisposableEffect(Unit) {
        alive.value = true
        onDispose { alive.value = false }
    }

    // Consume Back during saving; otherwise cancel without writing.
    BackHandler {
        if (!saving) onCancel()
    }

    val titleInvalid = attemptedSave && title.isBlank()
    val dateLabel = SimpleDateFormat(
        "MMM d, yyyy",
        Locale.getDefault()
    ).format(java.util.Date(dateMillis))

    Scaffold(
        modifier = Modifier.safeDrawingPadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(
                onClick = onCancel,
                enabled = !saving
            ) {
                Text("‹ Back")
            }

            Text(
                text = "Add Health Concern",
                style = MaterialTheme.typography.headlineMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Concern details",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            saveError = null
                        },
                        label = { Text("Concern or symptom") },
                        placeholder = { Text("Enter concern or symptom") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving,
                        singleLine = true,
                        isError = titleInvalid,
                        supportingText = {
                            if (titleInvalid) {
                                Text("Enter a concern or symptom.")
                            }
                        }
                    )

                    Text("Severity")

                    // Equal widths keep all three choices on small screens.
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ConcernSeverity.entries.forEach { severity ->
                            val label = when (severity) {
                                ConcernSeverity.LOW -> "Mild"
                                ConcernSeverity.MEDIUM -> "Moderate"
                                ConcernSeverity.HIGH -> "Severe"
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected =
                                        severityName == severity.name,
                                    onClick = {
                                        severityName = severity.name
                                    },
                                    enabled = !saving
                                )

                                TextButton(
                                    onClick = {
                                        severityName = severity.name
                                    },
                                    enabled = !saving,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Onset date",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedButton(
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = dateMillis
                            }

                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    dateMillis =
                                        Calendar.getInstance().apply {
                                            clear()
                                            set(year, month, day)
                                        }.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Text(dateLabel)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Notes",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            saveError = null
                        },
                        label = { Text("Optional details") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving,
                        minLines = 3
                    )
                }
            }

            saveError?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                onClick = {
                    attemptedSave = true
                    saveError = null

                    if (title.isNotBlank()) {
                        if (patientId.isBlank()) {
                            saveError =
                                "Select a care recipient before saving."
                        } else {
                            saving = true

                            val recordedDate = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.US
                            ).format(java.util.Date(dateMillis))

                            val concern = HealthConcern(
                                id = "",
                                patientId = patientId,
                                title = title.trim(),
                                severity = ConcernSeverity.valueOf(severityName),
                                recordedDate = recordedDate,
                                description = notes.trim(),
                                status = ConcernStatus.ACTIVE,
                                appointmentId = null
                            )

                            repository.addConcern(
                                patientId = patientId,
                                concern = concern
                            ) { result ->
                                if (alive.value) {
                                    saving = false

                                    result.fold(
                                        onSuccess = {
                                            savedCallback()
                                        },
                                        onFailure = {
                                            saveError =
                                                "We couldn't save the concern. " +
                                                        "Please try again."
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            ) {
                Text(if (saving) "Saving..." else "Save Concern")
            }

            OutlinedButton(
                onClick = onCancel,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}