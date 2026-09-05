package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Medication

internal data class MedicationEditorErrors(
    val name: String? = null,
    val strength: String? = null,
    val dose: String? = null,
    val frequency: String? = null,
    val reminderTime: String? = null
) {
    val hasErrors get() = listOf(name, strength, dose, frequency, reminderTime).any { it != null }
}

// Keeping validation outside the composable makes it cheap to unit test.
internal fun validateMedicationEditor(name: String, strength: String, dose: String, frequency: String, reminderTime: String) = MedicationEditorErrors(
    name = if (name.isBlank()) "Enter a medication name" else null,
    strength = if (strength.isBlank()) "Enter the medication strength" else null,
    dose = if (dose.isBlank()) "Enter the dose" else null,
    frequency = if (frequency.isBlank()) "Enter a frequency" else null,
    reminderTime = if (!Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$").matches(reminderTime)) "Use a 24-hour time such as 08:30" else null
)

@Composable
fun AddEditMedicationScreen(
    medication: Medication? = null,
    isSaving: Boolean = false,
    saveError: String? = null,
    onSave: (Medication) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    // Saveable state keeps entered values through rotation and process recreation.
    var name by rememberSaveable(medication?.id) { mutableStateOf(medication?.name.orEmpty()) }
    var strength by rememberSaveable(medication?.id) { mutableStateOf(medication?.strength.orEmpty()) }
    var dose by rememberSaveable(medication?.id) { mutableStateOf(medication?.dose.orEmpty()) }
    var frequency by rememberSaveable(medication?.id) { mutableStateOf(medication?.frequency.orEmpty()) }
    var reminderTime by rememberSaveable(medication?.id) { mutableStateOf(medication?.reminderTimes?.firstOrNull().orEmpty()) }
    var instructions by rememberSaveable(medication?.id) { mutableStateOf(medication?.instructions.orEmpty()) }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    val errors = remember(name, strength, dose, frequency, reminderTime, attemptedSave) {
        if (attemptedSave) validateMedicationEditor(name, strength, dose, frequency, reminderTime) else MedicationEditorErrors()
    }

    fun save() {
        // The parent owns persistence. This screen only submits a validated model.
        attemptedSave = true
        val currentErrors = validateMedicationEditor(name, strength, dose, frequency, reminderTime)
        if (!currentErrors.hasErrors && !isSaving) onSave(
            Medication(
                id = medication?.id ?: "med-${System.currentTimeMillis()}",
                patientId = medication?.patientId.orEmpty(),
                name = name.trim(), strength = strength.trim(), dose = dose.trim(), frequency = frequency.trim(),
                reminderTimes = listOf(reminderTime), instructions = instructions.trim(), active = medication?.active ?: true
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(if (medication == null) "Add medication" else "Edit medication", style = MaterialTheme.typography.headlineLarge)
        MedicationFormCard("Medication details") {
            MedicationField(name, { name = it }, "Medication name", errors.name)
            MedicationField(strength, { strength = it }, "Strength", errors.strength)
            MedicationField(dose, { dose = it }, "Dose", errors.dose)
        }
        MedicationFormCard("Schedule") {
            MedicationField(frequency, { frequency = it }, "Frequency", errors.frequency)
            MedicationField(reminderTime, { reminderTime = it.take(5) }, "Reminder time", errors.reminderTime, KeyboardType.Number)
        }
        MedicationFormCard("Instructions") {
            OutlinedTextField(
                value = instructions, onValueChange = { instructions = it }, label = { Text("Instructions (optional)") },
                modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(12.dp)
            )
        }
        if (saveError != null) Text(saveError, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
        Button(onClick = ::save, enabled = !isSaving, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (isSaving) "Saving medication" else "Save medication")
        }
        OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Cancel") }
    }
}

@Composable
// One card helper keeps the three form sections visually consistent.
private fun MedicationFormCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun MedicationField(value: String, onValueChange: (String) -> Unit, label: String, error: String?, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, isError = error != null,
        supportingText = error?.let { message -> { Text(message) } }, keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
    )
}
