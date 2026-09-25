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

@Composable
fun AddEditAppointmentScreen(
    appointment: Appointment? = null,
    isSaving: Boolean = false,
    saveError: String? = null,
    onSave: (Appointment) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    var title by rememberSaveable { mutableStateOf(appointment?.title ?: "") }
    var provider by rememberSaveable { mutableStateOf(appointment?.provider ?: "") }
    var date by rememberSaveable { mutableStateOf(appointment?.date ?: "") }
    var time by rememberSaveable { mutableStateOf(appointment?.time ?: "") }
    var location by rememberSaveable { mutableStateOf(appointment?.location ?: "") }
    var notes by rememberSaveable { mutableStateOf(appointment?.notes ?: "") }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    val titleError = attemptedSave && title.isBlank()
    val dateError = attemptedSave && date.isBlank()
    val timeError = attemptedSave && time.isBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if   (appointment == null) {
                "Add appointment"
            } else {
                "Edit appointment"
            }
        )

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
            placeholder = { Text("YYYY-MM-DD") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = dateError,
            supportingText = {
                if (dateError) {
                    Text("Use a valid date in YYYY-MM-DD format")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Time") },
            placeholder = { Text("HH:MM") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = timeError,
            supportingText = {
                if (timeError) {
                    Text("Use 24-hour time in HH:MM format")
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

        if (saveError != null) {
            Text(saveError)
        }

        Button(
            onClick = {
                attemptedSave = true

                if (
                    title.isNotBlank() &&
                    date.isNotBlank() &&
                    time.isNotBlank()
                ) {
                    onSave(
                        Appointment (
                            id = appointment?.id.orEmpty(),
                            patientId = appointment?.patientId.orEmpty(),
                            title = title.trim(),
                            date = date.trim(),
                            time = time.trim(),
                            provider = provider.trim(),
                            location = location.trim(),
                            notes = notes.trim(),
                            status = appointment?.status
                                ?: com.example.carelink.model.AppointmentStatus.SCHEDULED

                        )
                    )
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isSaving) {
                    "Saving..."
                } else {
                    "Save appointment"
                }
            )
        }

        OutlinedButton(
            onClick = onCancel,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}

private fun isValidDate(value: String): Boolean {
    if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(value)) {
        return false
    }

    val parts = value.split("-")
    val year = parts[0].toIntOrNull() ?: return false
    val month = parts[1].toIntOrNull() ?: return false
    val day = parts[2].toIntOrNull() ?: return false

    if (year < 1900 || month !in 1..12) {
        return false
    }

    val daysInMonth = when (month) {
        2 -> {
            if (
                year % 400 == 0 ||
                (year % 4 == 0 && year % 100 != 0)
            ) {
                29
            } else {
                28
            }
        }

        4, 6, 9, 11 -> 30
        else -> 31
    }

    return day in 1..daysInMonth
}

private fun isValidTime(value: String): Boolean {
    return Regex("""([01]\d|2[0-3]):[0-5]\d""").matches(value)
}