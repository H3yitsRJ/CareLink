package com.example.carelink.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Appointment
import com.example.carelink.model.CareTask
import com.example.carelink.model.validationErrors
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Composable
fun AddEditCareTaskScreen(
    task: CareTask? = null,
    sourceAppointment: Appointment? = null,
    patientId: String = "",
    isSaving: Boolean = false,
    saveError: String? = null,
    onSave: (CareTask) -> Unit = {},
    onCancel: () -> Unit = {},
    onDelete: (() -> Unit)? = null
) {
    val id = rememberSaveable(task?.id) {
        task?.id ?: UUID.randomUUID().toString()
    }
    var title by rememberSaveable(task?.id, sourceAppointment?.id) {
        mutableStateOf(
            task?.title ?: sourceAppointment?.let { "Follow up after ${it.title}" }.orEmpty()
        )
    }
    var description by rememberSaveable(task?.id) {
        mutableStateOf(task?.description ?: sourceAppointment?.notes.orEmpty())
    }
    var dueDate by rememberSaveable(task?.id) {
        mutableStateOf(task?.dueDate.orEmpty())
    }
    var time by rememberSaveable(task?.id) {
        mutableStateOf(task?.time.orEmpty())
    }
    var attempted by rememberSaveable(task?.id) {
        mutableStateOf(false)
    }
    var confirmDelete by rememberSaveable(task?.id) {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    fun showDueDatePicker() {
        val initialDate = Calendar.getInstance()
        val parts = dueDate.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull()
        val month = parts.getOrNull(1)?.toIntOrNull()
        val day = parts.getOrNull(2)?.toIntOrNull()

        if (year != null && month != null && day != null) {
            initialDate.set(year, month - 1, day)
        }

        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                dueDate = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )
            },
            initialDate.get(Calendar.YEAR),
            initialDate.get(Calendar.MONTH),
            initialDate.get(Calendar.DAY_OF_MONTH)
        ).apply {
            if (task == null) {
                datePicker.minDate = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        }.show()
    }

    fun showTaskTimePicker() {
        val now = Calendar.getInstance()
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()
        val minute = parts.getOrNull(1)?.toIntOrNull()

        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                time = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
            },
            hour ?: now.get(Calendar.HOUR_OF_DAY),
            minute ?: now.get(Calendar.MINUTE),
            false
        ).show()
    }

    fun draft() = CareTask(
        id,
        task?.patientId ?: patientId,
        title.trim(),
        description.trim(),
        dueDate.trim(),
        time.trim(),
        task?.completed ?: false,
        task?.appointmentId ?: sourceAppointment?.id
    )

    val errors = if (attempted) {
        draft().validationErrors(requireFuture = task == null)
    } else {
        emptyMap()
    }

    val actionShape = RoundedCornerShape(8.dp)
    val fieldShape = if (task == null) {
        OutlinedTextFieldDefaults.shape
    } else {
        RoundedCornerShape(12.dp)
    }

    val taskDetailsFields: @Composable () -> Unit = {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task title") },
            singleLine = true,
            enabled = !isSaving,
            isError = errors.containsKey("title"),
            supportingText = { errors["title"]?.let { Text(it) } },
            shape = fieldShape,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            enabled = !isSaving,
            minLines = 3,
            shape = fieldShape,
            modifier = Modifier.fillMaxWidth()
        )
    }

    val dueDateAndTimeFields: @Composable () -> Unit = {
        OutlinedTextField(
            value = dueDate,
            onValueChange = {},
            readOnly = true,
            label = { Text("Due date") },
            placeholder = { Text("Select date") },
            enabled = !isSaving,
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = { showDueDatePicker() }, enabled = !isSaving) {
                    Text("Choose date")
                }
            },
            isError = errors.containsKey("dueDate"),
            supportingText = { errors["dueDate"]?.let { Text(it) } },
            shape = fieldShape,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = time,
            onValueChange = {},
            readOnly = true,
            label = { Text("Time") },
            placeholder = { Text("Select time") },
            enabled = !isSaving,
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = { showTaskTimePicker() }, enabled = !isSaving) {
                    Text("Choose time")
                }
            },
            isError = errors.containsKey("time"),
            supportingText = {
                errors["time"]?.let { Text(it) }
                if (time.isNotBlank()) {
                    TextButton(onClick = { time = "" }, enabled = !isSaving) {
                        Text("Clear time")
                    }
                }
            },
            shape = fieldShape,
            modifier = Modifier.fillMaxWidth()
        )
    }

    val saveButton: @Composable (Modifier) -> Unit = { buttonModifier ->
        Button(
            onClick = {
                attempted = true
                val candidate = draft()
                if (candidate.validationErrors(requireFuture = task == null).isEmpty()) {
                    onSave(candidate)
                }
            },
            enabled = !isSaving,
            modifier = buttonModifier,
            shape = actionShape
        ) {
            Text(
                when {
                    isSaving -> "Saving..."
                    task == null -> "Save care task"
                    else -> "Save Changes"
                }
            )
        }
    }

    val cancelButton: @Composable (Modifier) -> Unit = { buttonModifier ->
        OutlinedButton(
            onClick = onCancel,
            enabled = !isSaving,
            modifier = buttonModifier,
            shape = actionShape
        ) {
            Text("Cancel")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFDCEEEF))
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            IconButton(onClick = onCancel, enabled = !isSaving) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (task == null) {
                        "Cancel adding care task"
                    } else {
                        "Cancel care task editing"
                    }
                )
            }

            Text(
                text = if (task == null) "Add Care Task" else "Edit Care Task",
                style = if (task == null) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = Color(0xFF172426)
            )
        }

        if (sourceAppointment != null) {
            Text("From ${sourceAppointment.title} on ${sourceAppointment.date}")
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            contentColor = Color(0xFF172426),
            border = BorderStroke(1.dp, Color(0xFFADB7B7))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Task details",
                    style = MaterialTheme.typography.titleMedium
                )
                taskDetailsFields()
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            contentColor = Color(0xFF172426),
            border = BorderStroke(1.dp, Color(0xFFADB7B7))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Due date & time",
                    style = MaterialTheme.typography.titleMedium
                )
                dueDateAndTimeFields()
            }
        }

        saveError?.let { message ->
            CareErrorState(message = message)
        }

        if (task == null) {
            saveButton(Modifier.fillMaxWidth())
            cancelButton(Modifier.fillMaxWidth())
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cancelButton(Modifier.weight(1f))
                saveButton(Modifier.weight(2f))
            }
        }

        if (task != null && onDelete != null) {
            OutlinedButton(
                onClick = { confirmDelete = true },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Delete care task",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (confirmDelete && task != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete care task?") },
            text = {
                Text("This will remove the task and cancel its reminder.")
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}