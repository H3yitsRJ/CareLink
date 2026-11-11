package com.example.carelink.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.carelink.data.HealthConcernRepository
import com.example.carelink.model.ConcernSeverity
import com.example.carelink.model.ConcernStatus
import com.example.carelink.model.HealthConcern
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddHealthConcernScreen(
    patientId: String,
    repository: HealthConcernRepository,
    onSaved: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val context = LocalContext.current
    val connectionAvailable = LocalConnectionAvailable.current

    var title by rememberSaveable(patientId) { mutableStateOf("") }
    var notes by rememberSaveable(patientId) { mutableStateOf("") }
    var severityName by rememberSaveable(patientId) {
        mutableStateOf(ConcernSeverity.MEDIUM.name)
    }
    var dateMillis by rememberSaveable(patientId) {
        mutableStateOf(Calendar.getInstance().timeInMillis)
    }
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
    val onsetDate = Date(dateMillis)
    val dateParts = listOf("d", "MMM", "yyyy").map { pattern ->
        SimpleDateFormat(pattern, Locale.getDefault()).format(onsetDate)
    }

    Scaffold(
        modifier = Modifier
            .safeDrawingPadding()
            .imePadding(),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !saving,
                        onClick = {
                            if (saving) return@Button
                            attemptedSave = true
                            saveError = null
                            if (!connectionAvailable) {
                                saveError = OFFLINE_ACTION_MESSAGE
                                return@Button
                            }

                            if (title.isNotBlank()) {
                                if (patientId.isBlank()) {
                                    saveError = "Select a care recipient before saving."
                                } else {
                                    saving = true

                                    val recordedDate = SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        Locale.US
                                    ).format(Date(dateMillis))

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
                        SaveButtonLabel(saving, "Save Concern")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onCancel, enabled = !saving) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cancel adding health concern"
                    )
                }

                Text(
                    text = "Add Health Concern",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFADB7B7))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Concern details",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Concern or symptom",
                        style = MaterialTheme.typography.labelLarge
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            saveError = null
                        },
                        placeholder = { Text("Enter concern or symptom") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !saving,
                        singleLine = true,
                        isError = titleInvalid,
                        supportingText = if (titleInvalid) {
                            { Text("Enter a concern or symptom.") }
                        } else {
                            null
                        }
                    )

                    Text("Severity")


                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConcernSeverity.entries.forEach { severity ->
                            val label = severity.displayLabel
                            val selected = severityName == severity.name

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .selectable(
                                        selected = selected,
                                        enabled = !saving,
                                        role = Role.RadioButton,
                                        onClick = { severityName = severity.name }
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    enabled = !saving
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFADB7B7))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Onset date",
                        style = MaterialTheme.typography.titleMedium
                    )

                    val openDatePicker: () -> Unit = {
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = dateMillis
                        }

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                dateMillis = Calendar.getInstance().apply {
                                    clear()
                                    set(year, month, day)
                                }.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dateParts.forEach { part ->
                            OutlinedButton(
                                onClick = openDatePicker,
                                enabled = !saving,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(part)
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFADB7B7))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Optional details",
                        style = MaterialTheme.typography.labelLarge
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            saveError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
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
        }
    }
}