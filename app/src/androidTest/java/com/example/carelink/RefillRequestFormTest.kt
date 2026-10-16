package com.example.carelink

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.model.Medication
import com.example.carelink.model.RefillRequest
import com.example.carelink.screens.RefillRequestScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RefillRequestFormTest {
    @get:Rule val compose = createComposeRule()
    private val medication = Medication("medication", "patient", "Medicine", "10 mg", "1 tablet", "Daily", listOf("08:00"))

    @Test fun emptyMedicationListOffersNavigationAndBack() {
        var opened = false
        var backed = false
        compose.setContent { RefillRequestScreen(patientId = "patient", onOpenMedications = { opened = true }, onCancel = { backed = true }) }
        compose.onNodeWithText("Open medications").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(opened); assertTrue(backed) }
        compose.onNodeWithText("Submit refill request").assertIsNotEnabled()
    }

    @Test fun medicationLoadingAndFailureAreDistinctFromEmptyAndRetryRecovers() {
        val loading = mutableStateOf(true)
        val error = mutableStateOf<String?>(null)
        val medications = mutableStateOf(emptyList<Medication>())
        compose.setContent {
            RefillRequestScreen(medications.value, patientId = "patient", medicationsLoading = loading.value,
                medicationLoadError = error.value, onRetryMedications = { error.value = null; medications.value = listOf(medication) })
        }
        compose.onNodeWithText("Loading medications").assertExists()
        compose.onNodeWithText("Submit refill request").assertIsNotEnabled()
        compose.runOnIdle { loading.value = false; error.value = "Couldn't load medications" }
        compose.onNodeWithText("Couldn't load medications").assertExists()
        compose.onNodeWithText("Retry").performScrollTo().performClick()
        compose.onNodeWithText("Medicine").performScrollTo().performClick()
        compose.onNodeWithText("Selected: Medicine").assertExists()
        compose.onNodeWithText("Submit refill request").assertIsEnabled()
    }

    @Test fun refillHubBackAndCareLinkRequestAreReachable() {
        var backed = false
        var requested = false
        compose.setContent { com.example.carelink.screens.MedicationRefillsScreen(onBack = { backed = true }, onRequestRefill = { requested = true }) }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { assertTrue(backed) }
        compose.onNodeWithText("Request a Refill").performClick()
        compose.onNodeWithText("Request in CareLink").performClick()
        compose.runOnIdle { assertTrue(requested) }
    }

    @Test fun selectionIsRequiredAndConfirmationWaitsForPersistence() {
        var submitted: RefillRequest? = null
        val saving = mutableStateOf(false)
        val saved = mutableStateOf(false)
        val error = mutableStateOf<String?>(null)
        compose.setContent {
            RefillRequestScreen(listOf(medication), patientId = "patient", requestedById = "caregiver",
                isSaving = saving.value, saved = saved.value, saveError = error.value,
                onSave = { submitted = it; saving.value = true })
        }
        compose.onNodeWithText("Submit refill request").performScrollTo().performClick()
        compose.onNodeWithText("Select an eligible medication.").assertExists()
        compose.runOnIdle { assertNull(submitted) }
        compose.onNodeWithText("Medicine").performScrollTo().performClick()
        compose.onNodeWithText("Request note (optional)").performScrollTo().performTextInput("Please refill")
        compose.onNodeWithText("Submit refill request").performScrollTo().performClick()
        compose.onNodeWithText("Submitting refill request").assertIsNotEnabled()
        compose.onNodeWithText("Refill request submitted.").assertDoesNotExist()
        compose.runOnIdle { saving.value = false; error.value = "A refill request is already active for this medication." }
        compose.onNodeWithText("Please refill").assertExists()
        compose.onNodeWithText("A refill request is already active for this medication.").assertExists()
        compose.runOnIdle { error.value = null; saved.value = true }
        compose.onNodeWithText("Refill request submitted.").assertExists()
        compose.onNodeWithText("Submit refill request").assertIsNotEnabled()
    }

    @Test fun existingActiveRequestBlocksSaveAndInactiveMedicationIsExcluded() {
        var submitted = false
        compose.setContent {
            RefillRequestScreen(listOf(medication, medication.copy(id = "inactive", name = "Inactive", active = false)),
                activeRequests = listOf(RefillRequest("medication", "patient", "medication", "caregiver")),
                patientId = "patient", requestedById = "caregiver", onSave = { submitted = true })
        }
        compose.onNodeWithText("Inactive").assertDoesNotExist()
        compose.onNodeWithText("Medicine").performScrollTo().performClick()
        compose.onNodeWithText("Submit refill request").performScrollTo().performClick()
        compose.onNodeWithText("A refill request is already active for this medication.").assertExists()
        compose.runOnIdle { assertFalse(submitted) }
    }

    @Test fun increasedTextSizeAndLongContentKeepFinalActionsReachable() {
        var submitted: RefillRequest? = null
        var cancelled = false
        val longMedication = medication.copy(name = "Example medication with a long name and extended release instructions")
        val longNote = "Please arrange a refill with the patient's usual pharmacy. ".repeat(8)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                RefillRequestScreen(listOf(longMedication), patientId = "patient", requestedById = "caregiver",
                    onSave = { submitted = it }, onCancel = { cancelled = true })
            }
        }
        compose.onNodeWithText(longMedication.name).performScrollTo().performClick()
        compose.onNodeWithText("Request note (optional)").performScrollTo().performTextInput(longNote)
        compose.onNodeWithText("Submit refill request").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals("medication", submitted?.medicationId)
            assertEquals(longNote.trim(), submitted?.note)
        }
        compose.onNodeWithText("Cancel").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(cancelled) }
    }
}
