package com.example.carelink.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Appointment
import com.example.carelink.model.CareTask
import com.example.carelink.model.validationErrors
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (task == null) "Add care task" else "Edit care task",
            style = MaterialTheme.typography.headlineMedium
        )

        if (sourceAppointment != null) Text("From ${sourceAppointment.title} on ${sourceAppointment.date}")

        OutlinedTextField(
            title,
            { title = it },
            label = { Text("Task title") },
            singleLine = true,
            enabled = !isSaving,
            isError = errors.containsKey("title"),
            supportingText = { errors["title"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            description,
            { description = it },
            label = { Text("Description (optional)") },
            enabled = !isSaving,
            minLines = 3,
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            dueDate,
            { dueDate = it },
            label = { Text("Due date") },
            placeholder = { Text("YYYY-MM-DD") },
            enabled = !isSaving,
            singleLine = true,
            isError = errors.containsKey("dueDate"),
            supportingText = { errors["dueDate"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            time,
            { time = it },
            label = { Text("Time") },
            placeholder = { Text("HH:MM") },
            enabled = !isSaving,
            singleLine = true,
            isError = errors.containsKey("time"),
            supportingText = { errors["time"]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth())

        saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                attempted = true
                val candidate = draft()
                if (candidate.validationErrors(requireFuture = task == null).isEmpty()) {
                    onSave(candidate)
                }
        },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving) "Saving..." else "Save care task")
        }

        OutlinedButton(
            onClick = onCancel,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
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
