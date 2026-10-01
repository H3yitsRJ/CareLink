package com.example.carelink

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.data.CareTaskRepository
import com.example.carelink.data.InMemoryCareTaskRepository
import com.example.carelink.model.Appointment
import com.example.carelink.model.CareTask
import com.example.carelink.screens.AppointmentDetailsScreen
import com.example.carelink.screens.CareTasksFlow
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppointmentFollowUpTest {
    @get:Rule val compose = createComposeRule()
    private val store = InMemoryCareTaskRepository()
    private val appointment = Appointment("appointment", "patient", "Checkup", "2026-10-01", "09:00", notes = "Ask about results")

    private fun open(repository: CareTaskRepository = store) {
        compose.setContent {
            var followingUp by remember { mutableStateOf(false) }
            CareLinkTheme {
                if (followingUp) CareTasksFlow("patient", repository, sourceAppointment = appointment,
                    onCancelFollowUp = { followingUp = false })
                else AppointmentDetailsScreen(appointment, onGenerateFollowUp = { followingUp = true })
            }
        }
        compose.onNodeWithText("Generate follow-up task").performClick()
    }
    private fun fill() {
        compose.onNodeWithText("Task title").performScrollTo().performTextReplacement("Call provider")
        compose.onNodeWithText("Due date").performScrollTo().performTextReplacement("2099-01-01")
        compose.onNodeWithText("Time").performScrollTo().performTextReplacement("09:00")
    }
    @Test fun longAppointmentDetailsRemainReachableWithLargerText() {
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, 1.5f)) {
                CareLinkTheme {
                    AppointmentDetailsScreen(appointment.copy(notes = "Long appointment notes. ".repeat(100)))
                }
            }
        }
        compose.onNodeWithText("Generate follow-up task").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Back to appointments").performScrollTo().assertIsDisplayed()
    }
    @Test fun editableFollowUpSavesWithAppointmentLinkAndAppearsInList() {
        open()
        compose.onNodeWithText("From Checkup on 2026-10-01").assertExists()
        compose.onNodeWithText("Follow up after Checkup").assertExists()
        compose.onNodeWithText("Ask about results").assertExists()
        fill()
        compose.onNodeWithText("Save care task").performScrollTo().performClick()
        compose.onNodeWithText("Call provider").assertExists()
        compose.onNodeWithText("Linked to appointment").assertExists()
        compose.runOnIdle { store.list("patient") { assertEquals("appointment", it.getOrThrow().single().appointmentId) } }
        compose.onNodeWithText("Add care task").performClick()
        compose.onNodeWithText("From Checkup on 2026-10-01").assertDoesNotExist()
    }
    @Test fun cancelReturnsToAppointmentWithoutCreatingTask() {
        open()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("Appointment details").assertExists()
        compose.runOnIdle { store.list("patient") { assertTrue(it.getOrThrow().isEmpty()) } }
    }
    @Test fun failedSavePreservesEditedDraftAndRetryCreatesOneTask() {
        var fail = true
        val repository = object : CareTaskRepository by store {
            override fun create(task: CareTask, completed: (Result<CareTask>) -> Unit) {
                if (fail) completed(Result.failure(IllegalStateException("Unavailable")))
                else store.create(task, completed)
            }
        }
        open(repository); fill()
        compose.onNodeWithText("Save care task").performScrollTo().performClick()
        compose.onNodeWithText("We couldn't save the care task. Please try again.").assertExists()
        compose.onNodeWithText("Call provider").assertExists()
        compose.runOnIdle { fail = false }
        compose.onNodeWithText("Save care task").performScrollTo().performClick()
        compose.runOnIdle { store.list("patient") { assertEquals(1, it.getOrThrow().size) } }
    }
}
