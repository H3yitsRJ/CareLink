package com.example.carelink

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus
import com.example.carelink.screens.AddEditAppointmentScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppointmentFormTest {
    @get:Rule val compose = createComposeRule()
    private val original = Appointment("appointment1", "patient1", "Checkup", "2099-09-23", "10:30",
        "Dr. Smith", "Clinic", "Bring records", AppointmentStatus.SCHEDULED)

    @Test fun editingPrefillsAllFieldsAndPreservesAppointment() {
        var saved: Appointment? = null
        compose.setContent { AddEditAppointmentScreen(original, onSave = { saved = it }) }
        listOf(original.title, "09-23-2099", "10:30 AM", original.provider, original.location, original.notes)
            .forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Save Changes").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(original, saved) }
    }

    @Test fun pastDateBlocksSaveAndCanBeCorrected() {
        var saved: Appointment? = null
        compose.setContent { AddEditAppointmentScreen(original.copy(date = "2000-01-01"), onSave = { saved = it }) }
        compose.onNodeWithText("Save Changes").performScrollTo().performClick()
        compose.onNodeWithText("Choose today or a future date").assertExists()
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithText("Date").performScrollTo().performTextReplacement("09-23-2099")
        compose.onNodeWithText("Save Changes").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(original, saved) }
    }
    @Test fun savingLocksEveryFieldAndFailureAllowsEditingAgain() {
        val saving = androidx.compose.runtime.mutableStateOf(true)
        compose.setContent { AddEditAppointmentScreen(original, isSaving = saving.value) }
        val labels = listOf(
            "Appointment title", "Provider", "Date", "Time", "Location", "What would you like to discuss?"
        )
        labels.forEach { compose.onNodeWithText(it).assertIsNotEnabled() }
        compose.runOnIdle { saving.value = false }
        labels.forEach { compose.onNodeWithText(it).assertIsEnabled() }
        compose.onNodeWithText("Appointment title").performScrollTo().performTextReplacement("Updated checkup")
        compose.onNodeWithText("Updated checkup").assertExists()
    }
}
