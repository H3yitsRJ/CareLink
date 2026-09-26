package com.example.carelink

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.data.HealthConcernRepository
import com.example.carelink.model.*
import com.example.carelink.screens.HealthConcernsFlow
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HealthConcernDetailsTest {
    @get:Rule val compose = createComposeRule()
    private val first = HealthConcern("one", "patient", "Sleep concern", ConcernSeverity.LOW, "2026-09-20", "Waking overnight")
    private val second = HealthConcern("two", "patient", "Knee concern", ConcernSeverity.HIGH, "2026-09-21", "Pain after walking", appointmentId = "appt")
    private val appointment = Appointment("appt", "patient", "Clinic visit", "2099-01-01", "09:00")
    private val store = FakeStore()
    private inner class FakeStore : HealthConcernRepository {
        val records = linkedMapOf(first.id to first, second.id to second)
        var selected: String? = null
        var delay = false
        var fail = false
        var observer: ((Result<HealthConcern?>) -> Unit)? = null
        override fun watchList(patientId: String, changed: (Result<List<HealthConcern>>) -> Unit): () -> Unit {
            changed(Result.success(records.values.toList())); return {}
        }
        override fun watchConcern(patientId: String, id: String, changed: (Result<HealthConcern?>) -> Unit): () -> Unit {
            selected = id; observer = changed
            if (!delay) changed(Result.success(records[id]))
            return { observer = null }
        }
        override fun watchAppointments(patientId: String, changed: (Result<List<Appointment>>) -> Unit): () -> Unit {
            changed(Result.success(listOf(appointment))); return {}
        }
        override fun setStatus(patientId: String, id: String, status: ConcernStatus, completed: (Result<Unit>) -> Unit) {
            if (fail) { completed(Result.failure(Exception("Unavailable"))); return }
            records[id] = records.getValue(id).copy(status = status)
            observer?.invoke(Result.success(records[id])); completed(Result.success(Unit))
        }
        override fun linkAppointment(patientId: String, id: String, appointmentId: String?, completed: (Result<Unit>) -> Unit) {
            records[id] = records.getValue(id).copy(appointmentId = appointmentId)
            observer?.invoke(Result.success(records[id])); completed(Result.success(Unit))
        }
    }
    private fun open(title: String = "Knee concern") {
        compose.setContent { CareLinkTheme { HealthConcernsFlow("patient", store) } }
        compose.onNodeWithText(title).performClick()
    }
    @Test fun selectionDisplaysAllSavedFieldsAndBackReturnsToList() {
        open()
        compose.runOnIdle { assertEquals("two", store.selected) }
        listOf("Knee concern", "Description: Pain after walking", "Severity: High", "Date: 2026-09-21", "Status: Active",
            "Linked appointment: Clinic visit — 2099-01-01 at 09:00").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Back to health concerns").performScrollTo().performClick()
        compose.onNodeWithText("Health concerns").assertExists()
        compose.onNodeWithText("Sleep concern").performClick()
        compose.onNodeWithText("Description: Waking overnight").assertExists()
    }
    @Test fun statusChangesPersistAndFailedUpdateCanBeRetried() {
        open()
        compose.runOnIdle { store.fail = true }
        compose.onNodeWithText("Mark as discussed").performScrollTo().performClick()
        compose.onNodeWithText("We couldn't save your changes. Please try again.").assertExists()
        compose.onNodeWithText("Status: Active").assertExists()
        compose.runOnIdle { store.fail = false }
        compose.onNodeWithText("Mark as discussed").performScrollTo().performClick()
        compose.onNodeWithText("Status: Discussed").assertExists()
        compose.runOnIdle { assertEquals(ConcernStatus.DISCUSSED, store.records.getValue("two").status) }
    }
    @Test fun appointmentCanBeLinkedAndUnlinked() {
        open("Sleep concern")
        compose.onNodeWithText("Linked appointment: None").assertExists()
        compose.onNodeWithText("Link to Appointment").performScrollTo().performClick()
        compose.onNodeWithText("Clinic visit — 2099-01-01 at 09:00").performClick()
        compose.onNodeWithText("Linked appointment: Clinic visit — 2099-01-01 at 09:00").assertExists()
        compose.runOnIdle { assertEquals("appt", store.records.getValue("one").appointmentId) }
        compose.onNodeWithText("Link to Appointment").performScrollTo().performClick()
        compose.onNodeWithText("Remove appointment link").performClick()
        compose.onNodeWithText("Linked appointment: None").assertExists()
    }
    @Test fun loadingAndMissingRecordStillAllowBackNavigation() {
        store.delay = true
        open()
        compose.onNodeWithText("Loading health concern...").assertExists()
        compose.runOnIdle { store.observer?.invoke(Result.success(null)) }
        compose.onNodeWithText("This health concern could not be found.").assertExists()
        compose.onNodeWithText("Link to Appointment").assertDoesNotExist()
        compose.onNodeWithText("Back to health concerns").performClick()
        compose.onNodeWithText("Health concerns").assertExists()
    }
    @Test fun loadFailureOffersRetryAndRecovers() {
        store.delay = true
        open()
        compose.runOnIdle { store.observer?.invoke(Result.failure(Exception("Offline"))); store.delay = false }
        compose.onNodeWithText("We couldn't load this health concern.").assertExists()
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithText("Description: Pain after walking").assertExists()
    }
}
