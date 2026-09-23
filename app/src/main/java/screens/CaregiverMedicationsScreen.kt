package com.example.carelink.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.data.MedicationCaregiverStore
import com.example.carelink.data.CaregiverMedicationData
import com.example.carelink.model.CarePermission
import com.example.carelink.model.CaregiverAccess
import com.example.carelink.model.Medication
import com.google.firebase.firestore.FirebaseFirestore

/** The same medication form is used for patient and caregiver edits. */
@Composable
fun CaregiverMedicationsScreen(actorId: String, onBack: () -> Unit, data: CaregiverMedicationData? = null) {
    val store = data ?: remember { MedicationCaregiverStore(FirebaseFirestore.getInstance()) }
    var patientInput by rememberSaveable { mutableStateOf("") }
    var patientId by remember { mutableStateOf<String?>(null) }
    var patientName by remember { mutableStateOf("") }
    var access by remember { mutableStateOf<CaregiverAccess?>(null) }
    var medications by remember { mutableStateOf<List<Medication>>(emptyList()) }
    var selected by remember { mutableStateOf<Medication?>(null) }
    var original by remember { mutableStateOf<Medication?>(null) }
    var loading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    fun cancelEdit() {
        selected = medications.find { it.id == original?.id }
        original = null
        error = null
    }
    BackHandler(enabled = !saving) {
        when { original != null -> cancelEdit(); selected != null -> selected = null; else -> onBack() }
    }

    DisposableEffect(patientId, actorId, retry) {
        val patient = patientId
        if (patient == null) return@DisposableEffect onDispose { }
        loading = true
        val stop = store.watchAccess(patient, actorId) { latest, name, online ->
            connected = online
            access = if (connected) latest else null
            patientName = if (connected && latest?.allows(CarePermission.VIEW) == true) name else ""
            loading = false
            if (connected && latest?.allows(CarePermission.VIEW) == true) {
                error = if (original != null && !latest.allows(CarePermission.EDIT)) "You now have view-only access. Cancel to return to medication details." else null
            } else {
                medications = emptyList()
                selected = null
                original = null
                error = if (!online) "Connect to the internet to verify caregiver access."
                    else "Medication access is unavailable or has been revoked. Ask the patient to grant access."
            }
        }
        onDispose { stop() }
    }
    DisposableEffect(patientId, access?.allows(CarePermission.VIEW), retry) {
        val patient = patientId
        if (patient == null || access?.allows(CarePermission.VIEW) != true) return@DisposableEffect onDispose { }
        loading = true
        val stop = store.watchMedications(patient) { latest, failure ->
            loading = false
            if (failure != null || latest == null) {
                medications = emptyList()
                selected = null
                original = null
                error = failure
            } else {
                medications = latest
                // Never replace the baseline of an in-progress edit with a newer remote record.
                if (original == null) selected = selected?.let { current -> medications.find { it.id == current.id } }
                error = null
            }
        }
        onDispose { stop() }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (patientId != null) {
            Text("Medications for ${patientName.ifBlank { patientId.orEmpty() }}", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
        }
        when {
            original != null && access?.allows(CarePermission.VIEW) == true -> AddEditMedicationScreen(
                medication = original, isSaving = saving, saveError = error,
                canSave = connected && access?.allows(CarePermission.EDIT) == true, canDelete = false,
                onCancel = ::cancelEdit,
                onSave = { edited ->
                    val baseline = original ?: return@AddEditMedicationScreen
                    saving = true
                    error = null
                    store.edit(baseline, edited, actorId) { result ->
                        saving = false
                        result.onSuccess {
                            original = null
                            if (patientId == baseline.patientId && access?.patientId == baseline.patientId && access?.allows(CarePermission.VIEW) == true) {
                                selected = it
                                success = "Medication saved."
                            }
                        }.onFailure {
                            error = "Couldn't save. Check your connection and access. If the medication changed, cancel and reopen it."
                        }
                    }
                }
            )
            selected != null && access?.allows(CarePermission.VIEW) == true -> Column {
                if (success != null) Text(success!!, Modifier.padding(horizontal = 16.dp))
                MedicationDetailsScreen(selected, errorMessage = error,
                    canEdit = connected && access?.allows(CarePermission.EDIT) == true, canRemove = false, showNavigation = false,
                    onEdit = { original = it; success = null; error = null }, onBack = { selected = null })
            }
            else -> Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Caregiver medications", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(patientInput, { patientInput = it }, label = { Text("Patient CareLink ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = {
                    val candidate = patientInput.trim()
                    if (!MedicationCaregiverStore.validId(candidate) || candidate == actorId) {
                        error = "Enter the patient's CareLink ID. Your own medications are on the Medications screen."
                    } else {
                        access = null; medications = emptyList(); selected = null; original = null; success = null
                        patientId = candidate; retry++
                    }
                }, enabled = !loading && !saving, modifier = Modifier.fillMaxWidth()) { Text("Open patient medications") }
                if (loading) Text("Loading medications")
                if (error != null) StateMessage(error!!, true)
                if (access?.allows(CarePermission.VIEW) == true && !loading) {
                    if (!access!!.allows(CarePermission.EDIT)) Text("View-only access")
                    if (medications.isEmpty() && error == null) Text("No medications to display.")
                    medications.forEach { medication -> MedicationCard(medication) { selected = medication; success = null } }
                }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        }
    }
}

@Composable
fun MedicationCaregiverAccessScreen(patientId: String, patientName: String, onBack: () -> Unit) {
    val store = remember { MedicationCaregiverStore(FirebaseFirestore.getInstance()) }
    var grants by remember { mutableStateOf<List<CaregiverAccess>>(emptyList()) }
    var caregiverId by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<CaregiverAccess?>(null) }
    var revoke by remember { mutableStateOf<CaregiverAccess?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    DisposableEffect(patientId, retry) {
        loading = true
        var active = true
        val listener = store.grants(patientId).addSnapshotListener { snapshot, failure ->
            if (!active) return@addSnapshotListener
            loading = false
            error = if (failure != null) "Couldn't load caregiver access. Try again." else null
            grants = snapshot?.documents?.mapNotNull { MedicationCaregiverStore.access(patientId, it.id, it.data.orEmpty()) }.orEmpty()
        }
        onDispose { active = false; listener.remove() }
    }
    fun save(value: CaregiverAccess) {
        saving = true; error = null; success = null
        store.saveAccess(value, patientName)
            .addOnSuccessListener { saving = false; selected = null; success = if (value.revoked) "Access revoked." else "Medication access saved." }
            .addOnFailureListener { saving = false; error = "Couldn't save access. Check your connection and the caregiver's CareLink ID. They need a completed profile." }
    }
    if (selected != null) {
        CaregiverAccessScreen(selected, "Medication access for ${selected!!.caregiverId}", true,
            onSave = { save(it.copy(revoked = false)) }, onCancel = { selected = null; error = null },
            availablePermissions = setOf(CarePermission.VIEW, CarePermission.EDIT), isSaving = saving, error = error)
    } else Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Medication caregiver access", style = MaterialTheme.typography.headlineMedium)
        Text("Your CareLink ID")
        SelectionContainer { Text(patientId) }
        Text("Give this ID to your caregiver. Ask for their CareLink ID before granting access.")
        OutlinedTextField(caregiverId, { caregiverId = it }, label = { Text("Caregiver CareLink ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val id = caregiverId.trim()
            if (!MedicationCaregiverStore.validId(id) || id == patientId) error = "Enter a different person's CareLink ID."
            else { error = null; selected = grants.find { it.caregiverId == id } ?: CaregiverAccess(id, patientId, id, setOf(CarePermission.VIEW)) }
        }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Choose medication access") }
        if (loading) Text("Loading caregiver access")
        if (error != null) { StateMessage(error!!, true); TextButton(onClick = { retry++ }) { Text("Retry") } }
        if (success != null) Text(success!!)
        grants.filter { !it.revoked }.forEach { grant ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(grant.caregiverId)
                Text(if (grant.allows(CarePermission.EDIT)) "View and edit medications" else if (grant.allows(CarePermission.VIEW)) "View medications" else "No medication access")
                TextButton(onClick = { selected = grant }, enabled = !saving) { Text("Edit access") }
                TextButton(onClick = { revoke = grant }, enabled = !saving) { Text("Revoke access") }
            } }
        }
        OutlinedButton(onClick = onBack, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
    revoke?.let { grant -> AlertDialog(onDismissRequest = { revoke = null }, title = { Text("Revoke medication access?") },
        text = { Text("This caregiver will no longer be able to view or edit your medications.") },
        confirmButton = { Button(onClick = { revoke = null; save(grant.copy(revoked = true)) }) { Text("Revoke") } },
        dismissButton = { TextButton(onClick = { revoke = null }) { Text("Cancel") } }) }
}
