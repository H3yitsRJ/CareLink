package com.example.carelink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Medication
import com.example.carelink.model.RefillRequest
import com.example.carelink.model.RefillRequestStatus
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton

// Only one open request per medication prevents accidental duplicate submissions.
internal fun canCreateRefill(medicationId: String, activeRequests: List<RefillRequest>) = medicationId.isNotBlank() && activeRequests.none {
    it.medicationId == medicationId && it.status in setOf(RefillRequestStatus.REQUESTED, RefillRequestStatus.PROCESSING)
}

@Composable
fun RefillRequestScreen(
    eligibleMedications: List<Medication> = emptyList(), activeRequests: List<RefillRequest> = emptyList(),
    patientId: String = "", requestedById: String = "", onSave: (RefillRequest) -> Unit = {},
    isSaving: Boolean = false, saveError: String? = null, saved: Boolean = false,
    canSave: Boolean = true, onCancel: () -> Unit = {}, initialMedicationId: String = "",
    medicationsLoading: Boolean = false, medicationLoadError: String? = null,
    onRetryMedications: () -> Unit = {}, onOpenMedications: (() -> Unit)? = null
) {
    BackHandler { if (!isSaving) onCancel() }
    var medicationId by rememberSaveable(patientId, requestedById) { mutableStateOf(initialMedicationId) }
    var note by rememberSaveable(patientId, requestedById) { mutableStateOf("") }
    var attempted by rememberSaveable(patientId, requestedById) { mutableStateOf(false) }
    val connected = LocalConnectionAvailable.current
    val eligible = eligibleMedications.filter { it.patientId == patientId && it.active }
    val request = RefillRequest(medicationId, patientId, medicationId, requestedById, note.trim())
    val errors = request.validationErrors()
    val valid = eligible.any { it.id == medicationId } && canCreateRefill(medicationId, activeRequests) && errors.isEmpty()
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(onClick = onCancel, enabled = !isSaving) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text("Request a refill", style = MaterialTheme.typography.headlineMedium)
        Text("Select a medication (required)")
        if (medicationsLoading) LoadingState("Loading medications")
        if (medicationLoadError != null) {
            StateMessage(medicationLoadError, true)
            TextButton(onClick = onRetryMedications, enabled = !medicationsLoading) { Text("Retry") }
        }
        eligible.forEach { medication ->
            OutlinedButton(onClick = { medicationId = medication.id }, enabled = !isSaving && !saved, modifier = Modifier.fillMaxWidth()) { Text(if (medicationId == medication.id) "Selected: ${medication.name}" else medication.name) }
        }
        if (eligible.isEmpty() && !medicationsLoading && medicationLoadError == null) {
            StateMessage("No active medications are available. Add a medication in Medications before requesting a refill.")
            if (onOpenMedications != null) TextButton(onClick = onOpenMedications) { Text("Open medications") }
        }
        OutlinedTextField(note, { note = it }, label = { Text("Request note (optional)") }, enabled = !isSaving && !saved, isError = attempted && "note" in errors, supportingText = { if (attempted && "note" in errors) Text(errors.getValue("note")) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        // Recalculate from current inputs so the message and submit action agree.
        if (attempted && !valid) StateMessage(when {
            eligible.none { it.id == medicationId } -> "Select an eligible medication."
            !canCreateRefill(medicationId, activeRequests) -> "A refill request is already active for this medication."
            else -> errors.values.first()
        }, true)
        if (!connected) StateMessage(OFFLINE_ACTION_MESSAGE, true)
        else if (!canSave) StateMessage("Medication edit access is required to request a refill.", true)
        if (saveError != null) Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) { StateMessage(saveError, true) }
        Button(onClick = {
            attempted = true
            if (valid && connected && canSave && !isSaving && !saved) onSave(request)
        }, enabled = connected && canSave && !isSaving && !saved && !medicationsLoading && medicationLoadError == null && eligible.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { SaveButtonLabel(isSaving, "Submit refill request", "Submitting refill request") }
        if (saved) Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) { StateMessage("Refill request submitted.") }
        OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) { Text(if (saved) "Done" else "Cancel") }
    }
}
