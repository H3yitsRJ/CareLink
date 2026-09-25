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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.carelink.model.Appointment

@Composable
fun AddEditAppointmentScreen(
    appointment: Appointment? = null,
    isSaving: Boolean = false,
    saveError: String? = null,
    onSave: (Appointment) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    var title by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.title.orEmpty()) }
    var provider by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.provider.orEmpty()) }
    var date by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.date.orEmpty()) }
    var time by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.time.orEmpty()) }
    var location by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.location.orEmpty()) }
    var notes by rememberSaveable(appointment?.id) { mutableStateOf(appointment?.notes.orEmpty()) }
    var attemptedSave by rememberSaveable(appointment?.id) { mutableStateOf(false) }
    val titleError = attemptedSave && title.isBlank()
    val dateError = if (attemptedSave) appointmentDateError(date) else null
    val timeError = attemptedSave && !isValidTime(time.trim())

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
            isError = dateError != null,
            supportingText = {
                if (dateError != null) {
                    Text(dateError)
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
                    appointmentDateError(date) == null &&
                    isValidTime(time.trim())
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

internal fun appointmentDateError(
    value: String,
    today: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
): String? {
    val date = value.trim()
    return when {
        !isValidDate(date) -> "Use a valid date in YYYY-MM-DD format"
        date < today -> "Choose today or a future date"
        else -> null
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

internal fun isValidTime(value: String): Boolean {
    return Regex("""([01]\d|2[0-3]):[0-5]\d""").matches(value)
}
