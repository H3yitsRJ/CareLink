package com.example.carelink

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.data.NotificationData
import com.example.carelink.model.CareNotification
import com.example.carelink.model.Medication
import com.example.carelink.screens.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ScreenUsabilityTest {
    @get:Rule val compose = createComposeRule()
    private class Notifications : NotificationData {
        var emit: ((Result<List<CareNotification>>) -> Unit)? = null
        var complete: ((Result<Unit>) -> Unit)? = null
        var attempts = 0
        override fun observeNotifications(userId: String, onResult: (Result<List<CareNotification>>) -> Unit): () -> Unit {
            emit = onResult
            onResult(Result.success(listOf(CareNotification(id = "one", title = "Care update", message = "Review your care activity"))))
            return {}
        }
        override fun markAsRead(userId: String, notificationId: String, onResult: (Result<Unit>) -> Unit) {
            attempts++
            complete = onResult
        }
    }
    @Test fun notificationFailureKeepsContentAndCanBeRetried() {
        val data = Notifications()
        var backed = false
        compose.setContent { NotificationsScreen(userId = "patient", data = data, onBack = { backed = true }) }
        compose.onNodeWithText("Mark as read").performClick()
        compose.onNodeWithText("Marking as read").assertIsNotEnabled()
        compose.runOnIdle { data.complete!!(Result.failure(Exception("internal detail"))) }
        compose.onNodeWithText("Care update").assertExists()
        compose.onNodeWithText("internal detail").assertDoesNotExist()
        compose.onNodeWithText("Mark as read").performClick()
        compose.runOnIdle { data.complete!!(Result.success(Unit)); assertEquals(2, data.attempts) }
        compose.onNodeWithText("Read").assertExists()
        compose.onNodeWithText("Mark as read").assertDoesNotExist()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { assertTrue(backed) }
    }
    @Test fun notificationOfflinePreventsWritesAndLoadFailureOffersRetry() {
        val data = Notifications()
        compose.setContent { CompositionLocalProvider(LocalConnectionAvailable provides false) { NotificationsScreen(userId = "patient", data = data) } }
        compose.onNodeWithText("Mark as read").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, data.attempts); data.emit!!(Result.failure(Exception("internal failure"))) }
        compose.onNodeWithText("We couldn't load notifications. Please try again.").assertExists()
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithText("Care update").assertExists()
    }
    @Test fun schedulerSelectsMedicationAndNavigationWorks() {
        val medication = Medication("one", "patient", "Medicine", "5 mg", "1 tablet", "Daily", listOf("08:00"))
        var selected: Medication? = null
        var backed = false
        var navigated = false
        compose.setContent { MedicationScheduleContent(listOf(medication), onEditMedication = { selected = it }, onBack = { backed = true }, onNavigate = { navigated = true }) }
        compose.onNodeWithText("Medicine").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(medication, selected); assertTrue(backed) }
        compose.onNodeWithText("Home").performClick()
        compose.runOnIdle { assertTrue(navigated) }
    }
    @Test fun emptySchedulerCanAddMedication() {
        var added = false
        compose.setContent { MedicationScheduleContent(emptyList(), onAddMedication = { added = true }) }
        compose.onNodeWithText("Add medication").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(added) }
    }

    @Test fun caregiverMedicationDetailsDoNotOfferUnwiredDoseActions() {
        val medication = Medication("one", "patient", "Medicine", "5 mg", "1 tablet", "Daily", listOf("08:00"))
        compose.setContent { MedicationDetailsScreen(medication, canRecordDose = false, canRemove = false, showNavigation = false) }
        compose.onNodeWithText("Record today's dose").assertDoesNotExist()
        compose.onNodeWithText("Taken").assertDoesNotExist()
        compose.onNodeWithText("Edit medication").performScrollTo().assertIsDisplayed()
    }

    @Test fun systemBackLeavesMedicationEditorWithoutSaving() {
        var cancelled = false
        var saved = false
        compose.setContent { AddEditMedicationScreen(onCancel = { cancelled = true }, onSave = { saved = true }) }
        androidx.test.espresso.Espresso.pressBack()
        compose.runOnIdle { assertTrue(cancelled); assertFalse(saved) }
    }

    @Test fun medicationActionsAndListRemainReachableOnSmallDisplayWithLargeText() {
        val medication = Medication("one", "patient", "Medicine", "5 mg", "1 tablet", "Daily", listOf("08:00"))
        var added = false
        var scheduled = false
        var selected = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                Box(Modifier.width(320.dp).height(520.dp)) {
                    MedicationsContent(listOf(medication), onAddMedication = { added = true },
                        onMedicationScheduler = { scheduled = true }, onMedicationSelected = { selected = true })
                }
            }
        }
        compose.onNodeWithText("Add Medication").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Medication Scheduler").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Medicine").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(added); assertTrue(scheduled); assertTrue(selected) }
    }
}
