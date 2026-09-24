// Care-task form with optional appointment context. Returns a task through onSave; MainActivity opens
// this for new tasks and appointment follow-up.

package com.example.carelink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Appointment
import com.example.carelink.model.CareTask

@Composable
fun AddEditCareTaskScreen(task: CareTask? = null, sourceAppointment: Appointment? = null, patientId: String = "", onSave: (CareTask) -> Unit = {}, onCancel: () -> Unit = {}, isSaving: Boolean = false, saveError: String? = null) {
    // Appointment-based tasks start with useful text but remain editable before saving.
    var title by rememberSaveable(task?.id, sourceAppointment?.id) { mutableStateOf(task?.title ?: sourceAppointment?.let { "Follow up after ${it.title}" }.orEmpty()) }
    var dueDate by rememberSaveable(task?.id) { mutableStateOf(task?.dueDate.orEmpty()) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val newId = rememberSaveable(task?.id, sourceAppointment?.id) { java.util.UUID.randomUUID().toString() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (task == null) "Add care task" else "Edit care task")
        if (sourceAppointment != null) StateMessage("From ${sourceAppointment.title} on ${sourceAppointment.date}")
        OutlinedTextField(title, { title = it }, label = { Text("Task") }, isError = attempted && title.isBlank(), supportingText = if (attempted && title.isBlank()) {{ Text("Enter a task") }} else null, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(dueDate, { dueDate = it }, label = { Text("Due date") }, modifier = Modifier.fillMaxWidth())
        if (saveError != null) StateMessage(saveError, true)
        Button(onClick = {
            attempted = true
            if (title.isNotBlank() && !isSaving) onSave(CareTask(task?.id ?: newId, task?.patientId ?: patientId, title.trim(), dueDate.trim(), task?.completed ?: false, task?.appointmentId ?: sourceAppointment?.id))
        }, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) { Text(if (isSaving) "Saving care task" else "Save care task") }
        OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
