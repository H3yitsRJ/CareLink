package com.example.carelink

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.model.*
import com.example.carelink.screens.*
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class Week4CareTaskScreenTest {
    @get:Rule val compose = createComposeRule()
    private val appointment = Appointment("appointment-1", "patient", "Checkup", "2026-09-21", "10:00")

    @Test fun smallViewportWithLargeTextKeepsFieldsAndActionsReachable() {
        var saved: CareTask? = null
        var cancelled = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                CareLinkTheme { Box(Modifier.size(320.dp, 480.dp)) {
                    AddEditCareTaskScreen(sourceAppointment = appointment, patientId = appointment.patientId,
                        onSave = { saved = it }, onCancel = { cancelled = true })
                } }
            }
        }
        compose.onNodeWithText("Task").performScrollTo().performTextReplacement("Call clinic")
        compose.onNodeWithText("Due date").performScrollTo().performTextInput("2026-10-01")
        compose.onNodeWithText("Save care task").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Cancel").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals("Call clinic", saved!!.title)
            assertEquals("2026-10-01", saved!!.dueDate)
            assertEquals(appointment.id, saved!!.appointmentId)
            assertTrue(cancelled)
        }
    }

    @Test fun appointmentActionPrefillsEditableTaskAndSaveShowsLinkedListItem() {
        var saved: CareTask? = null
        compose.setContent { CareLinkTheme {
            var editing by remember { mutableStateOf(false) }
            var task by remember { mutableStateOf<CareTask?>(null) }
            when {
                task != null -> CareTasksScreen(tasks = listOf(task!!))
                editing -> AddEditCareTaskScreen(sourceAppointment = appointment, patientId = appointment.patientId,
                    onSave = { saved = it; task = it })
                else -> AppointmentDetailsScreen(appointment, onGenerateFollowUp = { editing = true })
            }
        } }
        compose.onNodeWithText("Generate follow-up task").performScrollTo().performClick()
        compose.onNodeWithText("Follow up after Checkup").assertExists()
        compose.onNodeWithText("From Checkup on 2026-09-21").assertExists()
        compose.onNodeWithText("Task").performTextReplacement("Call the clinic")
        compose.onNodeWithText("Save care task").performScrollTo().performClick()
        compose.onNodeWithText("Call the clinic").assertExists()
        compose.onNodeWithText("Linked to appointment").assertExists()
        compose.runOnIdle { assertEquals(appointment.id, saved!!.appointmentId); assertEquals("patient", saved!!.patientId) }
    }

    @Test fun validationCancelAndFailedSavePreserveTheDraftAndStableId() {
        var saves = 0
        var cancels = 0
        val ids = mutableListOf<String>()
        compose.setContent { CareLinkTheme {
            var error by remember { mutableStateOf<String?>(null) }
            AddEditCareTaskScreen(patientId = "patient", saveError = error,
                onSave = { saves++; ids += it.id; error = "Couldn't save the care task." }, onCancel = { cancels++ })
        } }
        compose.onNodeWithText("Save care task").performClick()
        compose.onNodeWithText("Enter a task").assertExists()
        compose.onNodeWithText("Task").performTextInput("Call clinic")
        compose.onNodeWithText("Save care task").performScrollTo().performClick()
        compose.onNodeWithText("Couldn't save the care task.").assertExists()
        compose.onNodeWithText("Call clinic").assertExists()
        compose.onNodeWithText("Save care task").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, saves); assertEquals(1, cancels); assertEquals(1, ids.distinct().size) }
    }

    @Test fun savingDisablesSubmissionAndCancel() {
        compose.setContent { CareLinkTheme { AddEditCareTaskScreen(patientId = "patient", isSaving = true) } }
        compose.onNodeWithText("Saving care task").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsNotEnabled()
    }
}
