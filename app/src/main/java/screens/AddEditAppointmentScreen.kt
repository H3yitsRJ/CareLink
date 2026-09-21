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
import androidx.compose.material3.MaterialTheme
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddEditAppointmentScreen(
    onSave: (Appointment) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    var title by rememberSaveable { mutableStateOf("") }
    var provider by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    val titleError = attemptedSave && title.isBlank()
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val dateError = attemptedSave && (
            date.isBlank() ||
                    errorMessage == "Enter a valid date using MM/DD/YYYY." ||
                    errorMessage == "Appointment date cannot be in the past."
            )
    val timeError = attemptedSave && time.isBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Appointment")

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
                    Text("Enter a time")
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
        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                attemptedSave = true


                if (
                    title.isNotBlank() &&
                    date.isNotBlank() &&
                    time.isNotBlank()
                ) {
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    dateFormat.isLenient = false

                    val appointmentDate = try {
                        dateFormat.parse(date)
                    } catch (e: Exception) {
                        null
                    }

                    if (appointmentDate == null) {
                        errorMessage = "Enter a valid date using MM/DD/YYYY."
                        return@Button
                    }

                    val today = dateFormat.parse(dateFormat.format(Date()))

                    if (appointmentDate.before(today)) {
                        errorMessage = "Appointment date cannot be in the past."
                        return@Button
                    }
                    val appointment = Appointment(
                        id = "",
                        patientId = "",
                        title = title.trim(),
                        date = date.trim(),
                        time = time.trim(),
                        provider = provider.trim(),
                        location = location.trim(),
                        notes = notes.trim()
                    )

                    onSave(appointment)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Appointment")
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}