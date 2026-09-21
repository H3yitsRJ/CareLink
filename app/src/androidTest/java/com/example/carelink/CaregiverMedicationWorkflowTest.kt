package com.example.carelink

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.data.CaregiverMedicationData
import com.example.carelink.data.MedicationCaregiverStore
import com.example.carelink.model.*
import com.example.carelink.screens.CaregiverMedicationsScreen
import com.example.carelink.screens.CaregiverAccessScreen
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CaregiverMedicationWorkflowTest {
    @get:Rule val compose = createComposeRule()
    private val data = FakeCaregiverData()

    private fun open(edit: Boolean = true) {
        if (!edit) data.grant = data.grant.copy(permissions = setOf(CarePermission.VIEW))
        compose.setContent { CareLinkTheme { CaregiverMedicationsScreen("caregiver", {}, data) } }
        compose.onNodeWithText("Patient CareLink ID").performTextInput("patient")
        compose.onNodeWithText("Open patient medications").performClick()
        compose.onNodeWithText("Medications for Test patient").assertExists()
        compose.onNodeWithText("Example medication").performScrollTo().performClick()
    }
    private fun edit() {
        open()
        compose.onNodeWithText("Edit medication").performScrollTo().performClick()
        compose.onNodeWithText("Remove medication").assertDoesNotExist()
    }

    @Test fun caregiverSaveTargetsPatientAndRefreshesDetailsAndList() {
        edit()
        compose.onNodeWithText("Medication name").performTextReplacement("Updated medication")
        compose.onNodeWithText("Save medication").performScrollTo().performClick()
        compose.onNodeWithText("Updated medication").assertExists()
        compose.onNodeWithText("Medication saved.").assertExists()
        compose.runOnIdle {
            assertEquals("patient", data.medication.patientId)
            assertEquals("med", data.medication.id)
            assertEquals(listOf("08:00", "20:00"), data.medication.reminderTimes)
            assertEquals("caregiver", data.medication.updatedById)
            assertEquals(1, data.saves)
        }
        compose.onNodeWithText("Medication details").performClick()
        compose.onNodeWithText("Updated medication").assertExists()
    }
    @Test fun caregiverCancelLeavesStoredMedicationUnchanged() {
        edit()
        compose.onNodeWithText("Medication name").performTextReplacement("Unsaved")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Example medication").assertExists()
        compose.runOnIdle { assertEquals(0, data.saves) }
    }
    @Test fun viewOnlyAccessHasNoEditOrRemoveAction() {
        open(false)
        compose.onNodeWithText("Edit medication").assertDoesNotExist()
        compose.onNodeWithText("Remove medication").assertDoesNotExist()
    }
    @Test fun revocationClosesAnOpenEditAndClearsMedicationDetails() {
        edit()
        compose.runOnIdle { data.changeAccess(data.grant.copy(revoked = true)) }
        compose.onNodeWithText("Save medication").assertDoesNotExist()
        compose.onNodeWithText("Example medication").assertDoesNotExist()
        compose.onNodeWithText("Medication access is unavailable or has been revoked. Ask the patient to grant access.").assertExists()
        compose.runOnIdle { assertEquals(0, data.saves) }
    }
    @Test fun editPermissionRemovalDisablesSave() {
        edit()
        compose.runOnIdle { data.changeAccess(data.grant.copy(permissions = setOf(CarePermission.VIEW))) }
        compose.onNodeWithText("Save medication").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsEnabled()
    }
    @Test fun failedSaveKeepsFormValuesForRecovery() {
        edit()
        data.failSave = true
        compose.onNodeWithText("Medication name").performTextReplacement("Retry this value")
        compose.onNodeWithText("Save medication").performScrollTo().performClick()
        compose.onNodeWithText("Retry this value").assertExists()
        val tree = compose.onRoot().printToString()
        compose.runOnIdle { assertEquals(tree, 1, data.attempts) }
        assertTrue(tree, tree.contains("Couldn't save. Check your connection and access. If the medication changed, cancel and reopen it."))
        compose.runOnIdle { assertEquals("Example medication", data.medication.name) }
    }
    @Test fun concurrentChangeRequiresReopeningAndCancelShowsLatestValues() {
        edit()
        compose.runOnIdle { data.remoteChange(data.medication.copy(name = "Changed by patient")) }
        compose.onNodeWithText("Medication name").performTextReplacement("Stale edit")
        compose.onNodeWithText("Save medication").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("A concurrent edit must not be overwritten", 0, data.saves) }
        val tree = compose.onRoot().printToString()
        assertTrue(tree, tree.contains("Couldn't save."))
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Changed by patient").assertExists()
        compose.runOnIdle { assertEquals(0, data.saves) }
    }

    @Test fun medicationPermissionEditorKeepsViewAndEditConsistentAndCancelDoesNotSave() {
        var saves = 0
        var cancels = 0
        compose.setContent { CareLinkTheme {
            CaregiverAccessScreen(data.grant.copy(permissions = emptySet()), "Caregiver", true,
                onSave = { saves++ }, onCancel = { cancels++ }, availablePermissions = setOf(CarePermission.VIEW, CarePermission.EDIT))
        } }
        compose.onNodeWithContentDescription("edit access").performClick()
        compose.onNodeWithContentDescription("view access").assertIsOn()
        compose.onNodeWithContentDescription("view access").performClick()
        compose.onNodeWithContentDescription("edit access").assertIsOff()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, saves); assertEquals(1, cancels) }
    }

    private class FakeCaregiverData : CaregiverMedicationData {
        var medication = Medication("med", "patient", "Example medication", "5 mg", "1 tablet", "Daily", listOf("08:00", "20:00"))
        var grant = CaregiverAccess("caregiver", "patient", "caregiver", setOf(CarePermission.VIEW, CarePermission.EDIT))
        var saves = 0
        var failSave = false
        var attempts = 0
        private var accessChanged: ((CaregiverAccess?, String, Boolean) -> Unit)? = null
        private var medicationsChanged: ((List<Medication>?, String?) -> Unit)? = null
        fun changeAccess(value: CaregiverAccess) { grant = value; accessChanged?.invoke(value, "Test patient", true) }
        fun remoteChange(value: Medication) { medication = value; medicationsChanged?.invoke(listOf(value), null) }
        override fun watchAccess(patientId: String, caregiverId: String, changed: (CaregiverAccess?, String, Boolean) -> Unit): () -> Unit {
            assertEquals("patient", patientId); assertEquals("caregiver", caregiverId)
            accessChanged = changed; changed(grant, "Test patient", true)
            return { accessChanged = null }
        }
        override fun watchMedications(patientId: String, changed: (List<Medication>?, String?) -> Unit): () -> Unit {
            assertEquals("patient", patientId)
            medicationsChanged = changed; changed(listOf(medication), null)
            return { medicationsChanged = null }
        }
        override fun edit(original: Medication, edited: Medication, actorId: String, completed: (Result<Medication>) -> Unit) {
            attempts++
            if (failSave || !grant.allows(CarePermission.EDIT) || runCatching { MedicationCaregiverStore.editFields(original, medication, edited) }.isFailure) {
                completed(Result.failure(IllegalStateException("Save rejected"))); return
            }
            medication = edited.copy(updatedById = actorId); saves++
            medicationsChanged?.invoke(listOf(medication), null)
            completed(Result.success(medication))
        }
    }
}
