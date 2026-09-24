package com.example.carelink

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.model.Appointment
import com.example.carelink.screens.AddEditAppointmentScreen
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppointmentIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private val appointment = Appointment("appointment", "patient", "Checkup", "2026-10-01", "10:00",
        "Dr Example", "Clinic", "Bring questions")

    @Test fun editPrefillsFieldsAndPreservesIdentityAndUnchangedDetails() {
        var saved: Appointment? = null
        compose.setContent { CareLinkTheme {
            AddEditAppointmentScreen(appointment, onSave = { saved = it })
        } }
        compose.onNodeWithText("Checkup").assertExists()
        compose.onNodeWithText("Appointment title").performTextReplacement("Follow-up visit")
        compose.onNodeWithText("Save appointment").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(appointment.copy(title = "Follow-up visit"), saved) }
    }

    @Test fun invalidTimePreventsSaveAndAsyncSavingDisablesActions() {
        var saves = 0
        val saving = androidx.compose.runtime.mutableStateOf(false)
        compose.setContent { CareLinkTheme {
            AddEditAppointmentScreen(appointment, isSaving = saving.value, onSave = { saves++ })
        } }
        compose.onNodeWithText("Time", substring = false).performScrollTo().performTextReplacement("9:30")
        compose.onNodeWithText("Save appointment").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, saves); saving.value = true }
        compose.onNodeWithText("Saving...").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Cancel").performScrollTo().assertIsNotEnabled()
    }
}
