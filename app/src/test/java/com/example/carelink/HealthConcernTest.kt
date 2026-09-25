package com.example.carelink

import com.example.carelink.model.ConcernSeverity
import com.example.carelink.model.ConcernStatus
import com.example.carelink.model.HealthConcern
import org.junit.Assert.*
import org.junit.Test

class HealthConcernTest {
    private val concern = HealthConcern(
        id = "concern1", patientId = "patient1", title = "Trouble sleeping",
        description = "Waking several times each night", severity = ConcernSeverity.MEDIUM,
        recordedDate = "2026-09-23", status = ConcernStatus.DISCUSSED, appointmentId = "appointment1"
    )

    @Test fun writesExpectedFirestoreFields() {
        assertEquals(mapOf<String, Any?>(
            "patientId" to "patient1", "title" to "Trouble sleeping",
            "description" to "Waking several times each night", "severity" to "MEDIUM",
            "recordedDate" to "2026-09-23", "status" to "DISCUSSED", "appointmentId" to "appointment1"
        ), concern.toFirestore())
    }

    @Test fun allSupportedSeveritiesAndStatusesRoundTrip() {
        ConcernSeverity.entries.forEach { severity ->
            ConcernStatus.entries.forEach { status ->
                val original = concern.copy(severity = severity, status = status)
                assertEquals(original, HealthConcern.fromFirestore(original.id, original.toFirestore()))
            }
        }
    }

    @Test fun unlinkedConcernRoundTripsAndMissingStatusDefaultsToActive() {
        val unlinked = concern.copy(appointmentId = null, status = ConcernStatus.ACTIVE)
        assertEquals(unlinked, HealthConcern.fromFirestore(unlinked.id, unlinked.toFirestore()))
        assertEquals(unlinked, HealthConcern.fromFirestore(unlinked.id,
            unlinked.toFirestore() - "appointmentId" - "status"))
        assertEquals(ConcernStatus.ACTIVE, HealthConcern("id", "patient", "Title",
            ConcernSeverity.LOW, "2026-09-23", "Description").status)
    }

    @Test fun invalidRequiredFieldsAreRejectedWithoutCrashing() {
        val data = concern.toFirestore()
        listOf("patientId", "title", "description", "recordedDate", "severity").forEach { key ->
            assertNull("Missing $key", HealthConcern.fromFirestore(concern.id, data - key))
            listOf(null, "", "  ", 123, true).forEach { value ->
                assertNull("Invalid $key: $value", HealthConcern.fromFirestore(concern.id, data + (key to value)))
            }
        }
        assertNull(HealthConcern.fromFirestore(" ", data))
    }

    @Test fun unknownEnumsAreRejectedRatherThanMisrepresented() {
        assertNull(HealthConcern.fromFirestore(concern.id, concern.toFirestore() + ("severity" to "UNKNOWN")))
        listOf(null, "UNKNOWN", "active", 123).forEach { value ->
            assertNull(HealthConcern.fromFirestore(concern.id, concern.toFirestore() + ("status" to value)))
        }
    }

    @Test fun malformedOptionalAppointmentIdIsIgnoredAndDocumentIdWins() {
        listOf(null, "", " ", 123, false).forEach { value ->
            assertEquals(concern.copy(appointmentId = null), HealthConcern.fromFirestore(concern.id,
                concern.toFirestore() + ("appointmentId" to value) + ("id" to "untrusted-id")))
        }
    }
}
