package com.example.carelink

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.model.CareTask
import com.example.carelink.model.Medication
import com.example.carelink.model.RefillRequest
import com.example.carelink.screens.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReportedDefectsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failedDoseAttemptDoesNotShowPreviousSuccess() {
        compose.setContent { MedicationDetailsScreen(
            medication = Medication("one", "patient", "Medicine", "5 mg", "1 tablet", "Daily", listOf("08:00")),
            doseMessage = "Dose recorded as taken.", doseError = "Connect before saving."
        ) }
        compose.onNodeWithText("Connect before saving.").assertExists()
        compose.onNodeWithText("Dose recorded as taken.").assertDoesNotExist()
    }

    @Test fun completionLabelTogglesTaskWithoutOpeningEditor() {
        val task = CareTask(id = "task", patientId = "patient", title = "Follow up", dueDate = "2026-10-16", time = "09:00")
        var completed = false
        var opened = false
        compose.setContent { CareTasksScreen(tasks = listOf(task),
            onCompletedChange = { _, value -> completed = value }, onTaskSelected = { opened = true }) }
        compose.onAllNodesWithText("Completed")[1].performClick()
        compose.runOnIdle { assertTrue(completed); assertFalse(opened) }
    }

    @Test fun legacyRefillWithRemovedMedicationHasReadableFallback() {
        compose.setContent { RefillRequestTrackingScreen(requests = listOf(
            RefillRequest("internal-id", "patient", "internal-id", "patient")
        )) }
        compose.onNodeWithText("Medication no longer available").assertExists()
        compose.onNodeWithText("internal-id").assertDoesNotExist()
        compose.onNodeWithText("Medication no longer available").performClick()
        compose.onNodeWithText("Medication: Medication no longer available").assertExists()
    }

    @Test fun dashboardGreetingDoesNotDependOnTimeOfDay() {
        compose.setContent { DashboardScreen(fullName = "Jane Doe") }
        compose.onNodeWithText("Hello, Jane Doe").assertExists()
    }

    @Test fun refillRetainsSavedNameWhenMedicationIsRemovedOrRenamed() {
        compose.setContent { RefillRequestTrackingScreen(requests = listOf(
            RefillRequest("one", "patient", "one", "patient", medicationName = "Original medicine")
        ), medicationNames = mapOf("one" to "Renamed medicine")) }
        compose.onNodeWithText("Original medicine").assertExists()
        compose.onNodeWithText("Original medicine").performClick()
        compose.onNodeWithText("Medication: Original medicine").assertExists()
    }
    @Test fun openRefillDetailsFollowUpdatesAndDismissRemovedRecords() {
        val original = RefillRequest("one", "patient", "one", "patient", note = "Original note", medicationName = "Medicine")
        val requests = androidx.compose.runtime.mutableStateOf(listOf(original))
        compose.setContent { RefillRequestTrackingScreen(requests = requests.value) }
        compose.onNodeWithText("Medicine").performClick()
        compose.onNodeWithText("Note: Original note").assertExists()
        compose.runOnIdle {
            requests.value = listOf(original.copy(status = com.example.carelink.model.RefillRequestStatus.COMPLETED, note = "Updated note"))
        }
        compose.onNodeWithText("Note: Updated note").assertExists()
        compose.onNode(hasText("Status: COMPLETED") and hasAnyAncestor(isDialog())).assertExists()
        compose.onNodeWithText("Note: Original note").assertDoesNotExist()
        compose.runOnIdle { requests.value = emptyList() }
        compose.onNodeWithText("Refill Request Details").assertDoesNotExist()
    }

    @Test fun refillDetailsCloseWhenLoadingFails() {
        val error = androidx.compose.runtime.mutableStateOf<String?>(null)
        compose.setContent { RefillRequestTrackingScreen(requests = listOf(
            RefillRequest("one", "patient", "one", "patient", medicationName = "Medicine")
        ), error = error.value) }
        compose.onNodeWithText("Medicine").performClick()
        compose.onNodeWithText("Refill Request Details").assertExists()
        compose.runOnIdle { error.value = "Couldn't load requests" }
        compose.onNodeWithText("Refill Request Details").assertDoesNotExist()
        compose.onNodeWithText("Couldn't load requests").assertExists()
    }
    @Test fun missingRefillTimestampShowsDateUnavailable() {
        compose.setContent { RefillRequestTrackingScreen(requests = listOf(
            RefillRequest("one", "patient", "one", "patient", requestedAt = 0, medicationName = "Medicine")
        )) }
        compose.onNodeWithText("Date unavailable").assertExists()
        compose.onNodeWithText("Medicine").performClick()
        compose.onNode(hasText("Date unavailable") and hasAnyAncestor(isDialog())).assertExists()
    }
}
