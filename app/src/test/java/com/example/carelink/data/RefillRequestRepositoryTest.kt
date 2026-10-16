package com.example.carelink.data

import com.example.carelink.model.Medication
import com.example.carelink.model.RefillRequest
import com.example.carelink.model.RefillRequestStatus
import org.junit.Assert.*
import org.junit.Test

class RefillRequestRepositoryTest {
    private val medication = Medication("medication", "patient", "Medicine", "10 mg", "1 tablet", "Daily", listOf("08:00"))
    private val request = RefillRequest("medication", "patient", "medication", "caregiver", "Please refill")

    @Test fun validRequestPersistsInitialStatusAndDuplicateIsRejected() {
        val repository = InMemoryRefillRequestRepository { listOf(medication) }
        repository.create(request) {
            assertEquals(request, it.getOrThrow())
            assertEquals(RefillRequestStatus.REQUESTED, it.getOrThrow().status)
        }
        repository.create(request.copy(requestedById = "another-caregiver")) { assertTrue(it.isFailure) }
    }

    @Test fun invalidIdentityNoteStatusAndIneligibleMedicationDoNotPersist() {
        val repository = InMemoryRefillRequestRepository { listOf(medication) }
        listOf(request.copy(medicationId = ""), request.copy(id = "different"),
            request.copy(requestedById = "bad/id"), request.copy(note = "x".repeat(1001)),
            request.copy(status = RefillRequestStatus.PROCESSING), request.copy(patientId = "other"))
            .forEach { repository.create(it) { result -> assertTrue(result.isFailure) } }
        repository.create(request) { assertTrue(it.isSuccess) }
        InMemoryRefillRequestRepository { listOf(medication.copy(active = false)) }
            .create(request) { assertTrue(it.isFailure) }
    }

    @Test fun firestoreFieldsUseEstablishedNamesAndRequestedStatus() {
        assertEquals("REQUESTED", request.toFirestore()["status"])
        assertEquals("Please refill", request.toFirestore()["note"])
        assertEquals("caregiver", request.toFirestore()["requestedById"])
    }

    @Test fun trackingReadsServerTimestampsAndLegacyMillis() {
        val fields = request.toFirestore()
        val millis = 1_780_000_000_000L
        val timestamp = com.google.firebase.Timestamp(java.util.Date(millis))
        assertEquals(millis, refillRequestFromFirestore("medication", "patient", fields + ("requestedAt" to timestamp)).requestedAt)
        assertEquals(millis, refillRequestFromFirestore("medication", "patient", fields + ("requestedAt" to millis)).requestedAt)
        assertEquals(0L, refillRequestFromFirestore("medication", "patient", fields).requestedAt)
        assertEquals(RefillRequestStatus.REQUESTED, refillRequestFromFirestore("medication", "patient", fields).status)
    }

    @Test fun medicationNameSurvivesFirestoreRoundTripAndLegacyRequestsRemainReadable() {
        val named = request.copy(medicationName = "Original medicine")
        assertEquals("Original medicine", refillRequestFromFirestore(named.id, named.patientId, named.toFirestore()).medicationName)
        assertEquals("", refillRequestFromFirestore(request.id, request.patientId, request.toFirestore()).medicationName)
    }
}
