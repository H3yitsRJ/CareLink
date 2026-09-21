package com.example.carelink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun AddEditCareTaskScreen(task: CareTask? = null, sourceAppointment: Appointment? = null,
                          patientId: String = "",
                          onSave: (CareTask) -> Unit = {}, onCancel: () -> Unit = {}) {
    // Appointment-based tasks start with useful text but remain editable before saving.
    var title by rememberSaveable(task?.id, sourceAppointment?.id) { mutableStateOf(task?.title ?:
    sourceAppointment?.let { "Follow up after ${it.title}" }.orEmpty()) }
    var dueDate by rememberSaveable(task?.id) { mutableStateOf(task?.dueDate.orEmpty()) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (task == null) "Add care task" else "Edit care task")
        if (sourceAppointment != null) StateMessage("From ${sourceAppointment.title} " +
                "on ${sourceAppointment.date}")
        OutlinedTextField(title, { title = it }, label = { Text("Task") }, isError = attempted && title.isBlank(),
            supportingText = if (attempted && title.isBlank()) {{ Text("Enter a task") }} else null, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(dueDate, { dueDate = it }, label = { Text("Due date") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            attempted = true
            if (title.isNotBlank()) {
                onSave(
                    CareTask(
                        id = task?.id ?: "task-${System.currentTimeMillis()}",
                        patientId = task?.patientId ?: patientId,
                        title = title.trim(),
                        description = task?.description ?: "",
                        dueDate = dueDate.trim(),
                        time = task?.time ?: "",
                        completed = task?.completed ?: false,
                        appointmentId = task?.appointmentId ?: sourceAppointment?.id
                    )
                )
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save care task") }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
