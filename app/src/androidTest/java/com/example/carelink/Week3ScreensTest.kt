package com.example.carelink

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.carelink.model.*
import com.example.carelink.screens.AppointmentDetailsScreen
import com.example.carelink.screens.HealthConcernsScreen
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Isolated screen contracts. These tests deliberately do not stand in for repository/navigation integration. */
class Week3ScreensTest {
    @get:Rule val compose = createComposeRule()
    private val appointment = Appointment("a", "p", "Checkup", "2026-09-20", "10:00",
        "Dr Example", "Clinic", "Bring questions")
    private val concerns = listOf(
        HealthConcern("low", "p", "Low concern", ConcernSeverity.LOW, "2026-09-15"),
        HealthConcern("high", "p", "High concern", ConcernSeverity.HIGH, "2026-09-15"),
        HealthConcern("discussed", "p", "Discussed concern", ConcernSeverity.HIGH, "2026-09-14", ConcernStatus.DISCUSSED)
    )

    @Test fun appointmentShowsFieldsAndEmitsEditAndBackActions() {
        var edits = 0
        var backs = 0
        compose.setContent { CareLinkTheme {
            AppointmentDetailsScreen(appointment, onEdit = { edits++ }, onBack = { backs++ })
        } }
        listOf("Checkup", "2026-09-20 at 10:00", "Provider: Dr Example", "Location: Clinic",
            "Bring questions", "Status: Scheduled").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Edit appointment").performScrollTo().performClick()
        compose.onNodeWithText("Back to appointments").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, edits); assertEquals(1, backs) }
    }

    @Test fun appointmentLoadingMissingAndCancelledStatesDoNotExposeEdit() {
        val loading = mutableStateOf(true)
        val selected = mutableStateOf<Appointment?>(null)
        compose.setContent { CareLinkTheme { AppointmentDetailsScreen(selected.value, loading.value) } }
        compose.onNodeWithText("Loading appointment").assertExists()
        compose.onNodeWithText("Edit appointment").assertDoesNotExist()
        compose.runOnIdle { loading.value = false }
        compose.onNodeWithText("This appointment could not be found.").assertExists()
        compose.runOnIdle { selected.value = appointment.copy(status = AppointmentStatus.CANCELLED) }
        compose.onNodeWithText("Status: Cancelled").assertExists()
        compose.onNodeWithText("Edit appointment").assertDoesNotExist()
        compose.onNodeWithText("Cancel appointment").assertDoesNotExist()
    }

    @Test fun longAppointmentAtLargeTextKeepsBackReachable() {
        var backs = 0
        compose.setContent { CareLinkTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                Box(Modifier.size(320.dp, 480.dp)) {
                    AppointmentDetailsScreen(appointment.copy(notes = "Long appointment note. ".repeat(100)),
                        onBack = { backs++ })
                }
            }
        } }
        compose.onNodeWithText("Back to appointments").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, backs) }
    }

    @Test fun concernTitleSelectsCorrectRecordAndAddEmitsAction() {
        var selected: HealthConcern? = null
        var adds = 0
        compose.setContent { CareLinkTheme {
            HealthConcernsScreen(concerns, onAdd = { adds++ }, onSelect = { selected = it })
        } }
        compose.onNodeWithText("Add concern").performClick()
        compose.onNodeWithText("High concern").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(concerns[1], selected); assertEquals(1, adds) }
    }

    @Test fun concernFiltersCombineAndCanBeCleared() {
        compose.setContent { CareLinkTheme { HealthConcernsScreen(concerns) } }
        compose.onNodeWithText("Active", useUnmergedTree = true).performClick()
        compose.onNodeWithText("High", useUnmergedTree = true).performClick()
        compose.onNodeWithText("High concern").assertExists()
        compose.onNodeWithText("Low concern").assertDoesNotExist()
        compose.onNodeWithText("Discussed concern").assertDoesNotExist()
        compose.onNodeWithText("Medium", useUnmergedTree = true).performClick()
        compose.onNodeWithText("No health concerns match these filters.").assertExists()
        compose.onNodeWithText("Medium", useUnmergedTree = true).performClick()
        compose.onNodeWithText("All", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Discussed concern").performScrollTo().assertExists()
    }

    @Test fun concernLoadingErrorAndEmptyStatesReplaceRecords() {
        val loading = mutableStateOf(true)
        val error = mutableStateOf<String?>(null)
        compose.setContent { CareLinkTheme {
            HealthConcernsScreen(isLoading = loading.value, error = error.value)
        } }
        compose.onNodeWithText("Loading health concerns").assertExists()
        compose.runOnIdle { loading.value = false; error.value = "Could not load concerns." }
        compose.onNodeWithText("Could not load concerns.").assertExists()
        compose.runOnIdle { error.value = null }
        compose.onNodeWithText("No health concerns recorded.").assertExists()
    }

    @Test fun concernFiltersRemainReachableAtLargeText() {
        compose.setContent { CareLinkTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                Box(Modifier.size(320.dp, 480.dp)) { HealthConcernsScreen(concerns) }
            }
        } }
        compose.onNodeWithText("Discussed", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("High", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        // Lazy lists do not compose off-screen records until the list itself scrolls to them.
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Discussed concern"))
        compose.onNodeWithText("Discussed concern").assertIsDisplayed()
    }
}
