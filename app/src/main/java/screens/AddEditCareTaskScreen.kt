package com.example.carelink.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Appointment
import com.example.carelink.model.CareTask
import com.example.carelink.model.validationErrors
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
    val id = rememberSaveable(task?.id) { task?.id ?: UUID.randomUUID().toString() }
    var title by rememberSaveable(task?.id, sourceAppointment?.id) {
        mutableStateOf(task?.title ?: sourceAppointment?.let { "Follow up after ${it.title}" }.orEmpty())
    }
    var description by rememberSaveable(task?.id) { mutableStateOf(task?.description ?: sourceAppointment?.notes.orEmpty()) }
    var dueDate by rememberSaveable(task?.id) { mutableStateOf(task?.dueDate.orEmpty()) }
    var time by rememberSaveable(task?.id) { mutableStateOf(task?.time.orEmpty()) }
    var attempted by rememberSaveable(task?.id) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(task?.id) { mutableStateOf(false) }

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
        title.trim(), description.trim(),
        dueDate.trim(),
        time.trim(), task?.completed ?: false,
        task?.appointmentId ?: sourceAppointment?.id
    )

    val errors = if (attempted) {
        draft().validationErrors(requireFuture = task == null)
    } else {
        emptyMap()
    }

    val addTaskButtonShape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (task == null) MaterialTheme.colorScheme.background
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            if (task == null) {
                IconButton(
                    onClick = onCancel,
                    enabled = !isSaving
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cancel adding care task"
                    )
                }
            }

            Text(
                text = if (task == null) "Add Care Task" else "Edit care task",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        if (sourceAppointment != null) Text("From ${sourceAppointment.title} on ${sourceAppointment.date}")

        val taskDetailsFields: @Composable () -> Unit = {
            OutlinedTextField(
                title,
                { title = it },
                label = { Text("Task title") },
                singleLine = true,
                enabled = !isSaving,
                isError = errors.containsKey("title"),
                supportingText = { errors["title"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                description,
                { description = it },
                label = { Text("Description (optional)") },
                enabled = !isSaving,
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (task == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Task details",
                        style = MaterialTheme.typography.titleMedium
                    )
                    taskDetailsFields()
                }
            }
        } else {
            taskDetailsFields()
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
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (task == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Due date & time",
                        style = MaterialTheme.typography.titleMedium
                    )
                    dueDateAndTimeFields()
                }
            }
        } else {
            dueDateAndTimeFields()
        }

        saveError?.let { message ->
            CareErrorState(message = message)
        }

        Button(
            onClick = {
                attempted = true
                val candidate = draft()
                if (candidate.validationErrors(requireFuture = task == null).isEmpty()) {
                    onSave(candidate)
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = if (task == null) addTaskButtonShape else ButtonDefaults.shape
        ) {
            Text(if (isSaving) "Saving..." else "Save care task")
        }

        OutlinedButton(
            onClick = onCancel,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = if (task == null) addTaskButtonShape else ButtonDefaults.shape
        ) {
            Text("Cancel")
        }

        // Delete is available only when editing an existing task.
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

    // The confirmation dialog sits outside the form's Column.
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
