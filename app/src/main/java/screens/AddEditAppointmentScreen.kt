package com.example.carelink.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddEditAppointmentScreen(
    appointment: Appointment? = null,
    isSaving: Boolean = false,
    saveError: String? = null,
    onSave: (Appointment) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    BackHandler {
        if (!isSaving) onCancel()
    }

    var title by rememberSaveable(appointment?.id) {
        mutableStateOf(appointment?.title.orEmpty())
    }
    var provider by rememberSaveable(appointment?.id) {
        mutableStateOf(appointment?.provider.orEmpty())
    }
    var date by rememberSaveable(appointment?.id) {
        mutableStateOf(appointment?.date.orEmpty())
    }
    var time by rememberSaveable(appointment?.id) {
        mutableStateOf(appointment?.time.orEmpty())
    }
    var location by rememberSaveable(appointment?.id) {
        mutableStateOf(appointment?.location.orEmpty())
    }
    var notes by rememberSaveable(appointment?.id) {
        mutableStateOf(appointment?.notes.orEmpty())
    }
    var attemptedSave by rememberSaveable(appointment?.id) {
        mutableStateOf(false)
    }

    val context = LocalContext.current
    val isAddingAppointment = appointment == null
    val actionShape = if (isAddingAppointment) {
        ButtonDefaults.shape
    } else {
        RoundedCornerShape(8.dp)
    }

    fun showDatePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                date = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    year,
                    month + 1,
                    dayOfMonth
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    fun showTimePicker() {
        val calendar = Calendar.getInstance()

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                time = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    val titleError = attemptedSave && title.isBlank()
    val dateError = if (attemptedSave) appointmentDateError(date) else null
    val timeError = attemptedSave && !isValidTime(time.trim())

    val titleField: @Composable () -> Unit = {
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
    }

    val providerField: @Composable () -> Unit = {
        OutlinedTextField(
            value = provider,
            onValueChange = { provider = it },
            label = { Text("Provider") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }

    val locationField: @Composable () -> Unit = {
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }

    val dateField: @Composable () -> Unit = {
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date") },
            placeholder = { Text("YYYY-MM-DD") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSaving,
            trailingIcon = {
                TextButton(onClick = { showDatePicker() }, enabled = !isSaving) {
                    Text("Choose date")
                }
            },
            isError = dateError != null,
            supportingText = {
                if (dateError != null) {
                    Text(dateError)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    val timeField: @Composable () -> Unit = {
        OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Time") },
            placeholder = { Text("HH:MM") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSaving,
            trailingIcon = {
                TextButton(onClick = { showTimePicker() }, enabled = !isSaving) {
                    Text("Choose time")
                }
            },
            isError = timeError,
            supportingText = {
                if (timeError) {
                    Text("Enter a valid time (HH:MM)")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    val notesField: @Composable () -> Unit = {
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("What would you like to discuss?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = RoundedCornerShape(12.dp)
        )
    }

    val saveButton: @Composable (Modifier) -> Unit = { buttonModifier ->
        Button(
            onClick = {
                attemptedSave = true

                if (
                    title.isNotBlank() &&
                    appointmentDateError(date) == null &&
                    isValidTime(time.trim())
                ) {
                    onSave(
                        Appointment(
                            id = appointment?.id.orEmpty(),
                            patientId = appointment?.patientId.orEmpty(),
                            title = title.trim(),
                            date = date.trim(),
                            time = time.trim(),
                            provider = provider.trim(),
                            location = location.trim(),
                            notes = notes.trim(),
                            status = appointment?.status ?: AppointmentStatus.SCHEDULED
                        )
                    )
                }
            },
            enabled = !isSaving,
            modifier = buttonModifier,
            shape = actionShape
        ) {
            Text(
                when {
                    isSaving -> "Saving..."
                    isAddingAppointment -> "Save appointment"
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            if (!isAddingAppointment) {
                IconButton(onClick = onCancel, enabled = !isSaving) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cancel appointment editing"
                    )
                }
            }

            Text(
                text = if (isAddingAppointment) "Add Appointment" else "Edit Appointment",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF172426)
            )
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
                    text = "Visit details",
                    style = MaterialTheme.typography.titleMedium
                )
                titleField()
                providerField()
                locationField()
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
                    text = "Date and time",
                    style = MaterialTheme.typography.titleMedium
                )
                dateField()
                timeField()
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
                    text = "Reason for visit",
                    style = MaterialTheme.typography.titleMedium
                )
                notesField()
            }
        }

        if (saveError != null) {
            CareErrorState(message = saveError)
        }

        if (isAddingAppointment) {
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
