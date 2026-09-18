// Appointment form for both creation and editing. Validates title, date presence, and 24-hour time, then
// returns a model to MainActivity for its in-memory list.

package com.example.carelink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

internal fun validateAppointment(title: String, date: String, time: String): String? = when {
    title.isBlank() -> "Enter an appointment title"
    date.isBlank() -> "Enter the appointment date"
    !Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$").matches(time) -> "Use a 24-hour time such as 09:30"
    else -> null
}

@Composable
fun AddEditAppointmentScreen(
    appointment: Appointment? = null,
    patientId: String = "",
    onSave: (Appointment) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    var title by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.title.orEmpty()) }
    var provider by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.provider.orEmpty()) }
    var date by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.date.orEmpty()) }
    var time by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.time.orEmpty()) }
    var location by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.location.orEmpty()) }
    var notes by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.notes.orEmpty()) }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    val titleError = attemptedSave && title.isBlank()
    val dateError = attemptedSave && date.isBlank()
    val timeError = attemptedSave && validateAppointment(title, date, time) != null && title.isNotBlank() && date.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (appointment == null) "Add Appointment" else "Edit Appointment")

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Appointment title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = titleError,
            supportingText = {
                if (titleError) {
                    Text("Enter an appointment title")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = provider,
            onValueChange = { provider = it },
            label = { Text("Provider") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = dateError,
            supportingText = {
                if (dateError) {
                    Text("Enter a date")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Time") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = timeError,
            supportingText = {
                if (timeError) {
                    Text("Use a 24-hour time such as 09:30")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = {
                attemptedSave = true

                if (
                    validateAppointment(title, date, time) == null
                ) {
                    val saved = Appointment(
                        id = appointment?.id ?: "appointment-${System.currentTimeMillis()}",
                        patientId = appointment?.patientId ?: patientId,
                        title = title.trim(),
                        date = date.trim(),
                        time = time.trim(),
                        provider = provider.trim(),
                        location = location.trim(),
                        notes = notes.trim(),
                        status = appointment?.status ?: com.example.carelink.model.AppointmentStatus.SCHEDULED
                    )

                    onSave(saved)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Appointment")
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
