package com.example.carelink

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.data.*
import com.example.carelink.model.*
import com.example.carelink.screens.CareRecipientSelector
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CareRecipientSelectorTest {
    @get:Rule val compose = createComposeRule()
    private var directoryCallback: ((Result<List<CareRecipient>>) -> Unit)? = null
    private val recipients = listOf(CareRecipient("a", "Alice"), CareRecipient("b", "Bob"))
    private var initial = recipients
    private var chosen: String? = null
    private val directory = object : CareRecipientDirectory {
        override fun watch(caregiverId: String, changed: (Result<List<CareRecipient>>) -> Unit): () -> Unit {
            directoryCallback = changed; changed(Result.success(initial)); return { directoryCallback = null }
        }
    }
    private val oldMedicationCallbacks = mutableMapOf<String, (List<Medication>?, String?) -> Unit>()
    private val data = object : CaregiverMedicationData {
        override fun watchAccess(patientId: String, caregiverId: String, changed: (CaregiverAccess?, String, Boolean) -> Unit): () -> Unit {
            changed(CaregiverAccess(caregiverId, patientId, caregiverId, setOf(CarePermission.VIEW)),
                if (patientId == "a") "Alice" else "Bob", true)
            return {}
        }
        override fun watchMedications(patientId: String, changed: (List<Medication>?, String?) -> Unit): () -> Unit {
            oldMedicationCallbacks[patientId] = changed
            changed(listOf(medication(patientId)), null); return {}
        }
        override fun edit(original: Medication, edited: Medication, actorId: String, completed: (Result<Medication>) -> Unit) {}
    }
    private fun medication(id: String) = Medication("med-$id", id, "Medication $id", "5 mg", "1 tablet", "Daily", listOf("08:00"))
    private fun open() {
        compose.setContent {
            CareLinkTheme {
                var selected by rememberSaveable { mutableStateOf<String?>(null) }
                var visible by remember { mutableStateOf(true) }
                if (visible) CareRecipientSelector("caregiver", selected, { selected = it; chosen = it }, directory, data, { visible = false })
                else Button(onClick = { visible = true }) { Text("Return to caregiver") }
            }
        }
    }
    @Test fun switchingRecipientsReplacesDataAndIgnoresOldCallbacks() {
        open()
        compose.onNodeWithText("Alice").performClick()
        compose.onNodeWithText("Current care recipient: Alice").assertExists()
        compose.onNodeWithText("Medication a").assertExists()
        compose.onNodeWithText("Change recipient").performClick()
        compose.onNodeWithText("Bob").performClick()
        compose.onNodeWithText("Current care recipient: Bob").assertExists()
        compose.onNodeWithText("Medication b").assertExists()
        compose.runOnIdle { oldMedicationCallbacks["a"]?.invoke(listOf(medication("a")), null) }
        compose.onNodeWithText("Medication a").assertDoesNotExist()
    }
    @Test fun selectionSurvivesDetailsAndLeavingTheCaregiverScreen() {
        open()
        compose.onNodeWithText("Bob").performClick()
        compose.onNodeWithText("Medication b").performClick()
        compose.onNodeWithText("Medication details").assertExists()
        compose.onNodeWithText("Current care recipient: Bob").assertExists()
        // The detail screen's back control returns to the selected recipient's list.
        compose.onNodeWithText("Medication details").performClick()
        compose.onNodeWithText("Back").performScrollTo().performClick()
        compose.onNodeWithText("Return to caregiver").performClick()
        compose.onNodeWithText("Current care recipient: Bob").assertExists()
        compose.runOnIdle { assertEquals("b", chosen) }
    }
    @Test fun revocationRemovesRecipientAndTheirData() {
        open()
        compose.onNodeWithText("Alice").performClick()
        compose.runOnIdle { directoryCallback?.invoke(Result.success(listOf(recipients[1]))) }
        compose.onNodeWithText("Medication a").assertDoesNotExist()
        compose.onNodeWithText("Alice").assertDoesNotExist()
        compose.onNodeWithText("Bob").assertExists()
        compose.runOnIdle { assertNull(chosen) }
    }
    @Test fun noAccessShowsEmptyState() {
        initial = emptyList(); open()
        compose.onNodeWithText("You don't have access to any care recipients yet.").assertExists()
        compose.onNodeWithText("Patient CareLink ID").assertDoesNotExist()
    }
    @Test fun verificationFailureHidesRecordsAndRetryRecovers() {
        open()
        compose.onNodeWithText("Alice").performClick()
        compose.runOnIdle { directoryCallback?.invoke(Result.failure(Exception("Offline"))) }
        compose.onNodeWithText("Medication a").assertDoesNotExist()
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithText("Current care recipient: Alice").assertExists()
    }
}
