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
fun AddEditCareTaskScreen(task: CareTask? = null, sourceAppointment: Appointment? = null,
    patientId: String = "", isSaving: Boolean = false, saveError: String? = null,
    onSave: (CareTask) -> Unit = {}, onCancel: () -> Unit = {}) {
    val id = rememberSaveable(task?.id) { task?.id ?: UUID.randomUUID().toString() }
    var title by rememberSaveable(task?.id, sourceAppointment?.id) {
        mutableStateOf(task?.title ?: sourceAppointment?.let { "Follow up after ${it.title}" }.orEmpty())
    }
    var description by rememberSaveable(task?.id) { mutableStateOf(task?.description.orEmpty()) }
    var dueDate by rememberSaveable(task?.id) { mutableStateOf(task?.dueDate.orEmpty()) }
    var time by rememberSaveable(task?.id) { mutableStateOf(task?.time.orEmpty()) }
    var attempted by rememberSaveable(task?.id) { mutableStateOf(false) }
    fun draft() = CareTask(id, task?.patientId ?: patientId, title.trim(), description.trim(),
        dueDate.trim(), time.trim(), task?.completed ?: false, task?.appointmentId ?: sourceAppointment?.id)
    val errors = if (attempted) draft().validationErrors() else emptyMap()
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (task == null) "Add care task" else "Edit care task")
        if (sourceAppointment != null) Text("From ${sourceAppointment.title} on ${sourceAppointment.date}")
        OutlinedTextField(title, { title = it }, label = { Text("Task title") }, singleLine = true,
            enabled = !isSaving, isError = errors.containsKey("title"),
            supportingText = { errors["title"]?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") },
            enabled = !isSaving, minLines = 3, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(dueDate, { dueDate = it }, label = { Text("Due date") }, placeholder = { Text("YYYY-MM-DD") },
            enabled = !isSaving, singleLine = true, isError = errors.containsKey("dueDate"),
            supportingText = { errors["dueDate"]?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(time, { time = it }, label = { Text("Time") }, placeholder = { Text("HH:MM") },
            enabled = !isSaving, singleLine = true, isError = errors.containsKey("time"),
            supportingText = { errors["time"]?.let { Text(it) } }, modifier = Modifier.fillMaxWidth())
        saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            attempted = true
            val candidate = draft()
            if (candidate.validationErrors().isEmpty()) onSave(candidate)
        }, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(if (isSaving) "Saving..." else "Save care task")
        }
        OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
