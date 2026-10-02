package com.example.carelink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Medication
import com.example.carelink.model.RefillRequest
import com.example.carelink.model.RefillRequestStatus

// Only one open request per medication prevents accidental duplicate submissions.
internal fun canCreateRefill(medicationId: String, activeRequests: List<RefillRequest>) = medicationId.isNotBlank() && activeRequests.none {
    it.medicationId == medicationId && it.status in setOf(RefillRequestStatus.REQUESTED, RefillRequestStatus.PROCESSING)
}

@Composable
fun RefillRequestScreen(
    eligibleMedications: List<Medication> = emptyList(), activeRequests: List<RefillRequest> = emptyList(),
    patientId: String = "", requestedById: String = "", onSave: (RefillRequest) -> Unit = {}
) {
    var medicationId by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var saved by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Request a refill")
        eligibleMedications.forEach { medication ->
            Button(onClick = { medicationId = medication.id }, modifier = Modifier.fillMaxWidth()) { Text(if (medicationId == medication.id) "Selected: ${medication.name}" else medication.name) }
        }
        if (eligibleMedications.isEmpty()) StateMessage("No medications are eligible for a refill.")
        OutlinedTextField(note, { note = it }, label = { Text("Request note") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        // Recalculate from current inputs so the message and submit action agree.
        val valid = canCreateRefill(medicationId, activeRequests)
        if (attempted && !valid) StateMessage(if (medicationId.isBlank()) "Select a medication." else "A refill request is already active for this medication.", true)
        Button(onClick = {
            attempted = true
            if (valid) {
                onSave(RefillRequest("refill-${System.currentTimeMillis()}", patientId, medicationId, requestedById, note.trim()))
                saved = true
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Submit refill request") }
        if (saved) StateMessage("Refill request submitted.")
    }
}
