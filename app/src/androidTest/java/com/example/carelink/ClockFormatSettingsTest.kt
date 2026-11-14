package com.example.carelink

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.carelink.model.Appointment
import com.example.carelink.screens.AddEditAppointmentScreen
import com.example.carelink.screens.ClockFormat
import com.example.carelink.screens.LocalClockFormat
import com.example.carelink.screens.SettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ClockFormatSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun choosingTwentyFourHourUpdatesTheAppointmentEditor() {
        val format = mutableStateOf(ClockFormat.TwelveHour)
        val inSettings = mutableStateOf(true)
        val original = Appointment("appointment", "patient", "Checkup", "2099-09-23", "13:30")
        var saved: Appointment? = null
        compose.setContent {
            CompositionLocalProvider(LocalClockFormat provides format.value) {
                if (inSettings.value) {
                    SettingsScreen(
                        onBack = { inSettings.value = false },
                        clockFormat = format.value,
                        onClockFormatChange = { format.value = it }
                    )
                } else {
                    AddEditAppointmentScreen(original, onSave = { saved = it })
                }
            }
        }
        compose.onNodeWithText("Clock format").performClick()
        compose.onNodeWithText("24-hour (13:30)").performClick()
        compose.onNodeWithText("24-hour").assertExists()
        compose.runOnIdle { inSettings.value = false }
        compose.onNodeWithText("13:30").assertExists()
        compose.onNodeWithText("Save Changes").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(original, saved) }
    }
}
