package com.example.carelink.model

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class RefillRequestTest {

    private fun request() = RefillRequest(
        id = "medication-1",
        patientId = "patient-1",
        medicationId = "medication-1",
        requestedById = "caregiver-1",
        note = "Please refill this medication.",
        requestedAt = 1_780_000_000_000L,
        medicationName = "Test medication"
    )

    @Test
    fun newRequestContainsRequiredFieldsAndDefaultsToRequested() {
        val request = request()

        assertEquals("medication-1", request.id)
        assertEquals("patient-1", request.patientId)
        assertEquals("medication-1", request.medicationId)
        assertEquals("caregiver-1", request.requestedById)
        assertEquals("Please refill this medication.", request.note)
        assertEquals(1_780_000_000_000L, request.requestedAt)
        assertEquals(RefillRequestStatus.REQUESTED, request.status)
        assertTrue(request.validationErrors().isEmpty())
    }

    @Test
    fun firestoreRoundTripPreservesFieldsWithServerTimestamp() {
        val original = request()
        // The repository adds this server-generated field when saving.
        val fields = original.toFirestore() + (
                "requestedAt" to Timestamp(Date(original.requestedAt))
                )

        val restored = RefillRequest.fromFirestore(
            original.id,
            original.patientId,
            fields
        )

        assertEquals(original, restored)
    }

    @Test
    fun everySupportedStatusSurvivesConversion() {
        val original = request()

        RefillRequestStatus.values().forEach { status ->
            val fields = original.copy(status = status).toFirestore()
            val restored = RefillRequest.fromFirestore(
                original.id,
                original.patientId,
                fields
            )

            assertEquals(status.name, fields["status"])
            assertEquals(status, restored.status)
        }
    }

    @Test
    fun legacyMillisecondDateIsReadCorrectly() {
        val original = request()
        val fields = original.toFirestore() + (
                "requestedAt" to original.requestedAt
                )

        val restored = RefillRequest.fromFirestore(
            original.id,
            original.patientId,
            fields
        )

        assertEquals(original.requestedAt, restored.requestedAt)
    }

    @Test
    fun missingOptionalFieldsUseExistingDefaults() {
        val restored = RefillRequest.fromFirestore(
            "medication-1",
            "patient-1",
            mapOf(
                "medicationId" to "medication-1",
                "requestedById" to "caregiver-1"
            )
        )

        assertEquals("", restored.note)
        assertEquals("", restored.medicationName)
        assertEquals(0L, restored.requestedAt)
        assertEquals(RefillRequestStatus.REQUESTED, restored.status)
    }

    @Test
    fun mismatchedRequestAndMedicationIdsAreRejectedForCreation() {
        val invalid = request().copy(medicationId = "different-medication")

        assertTrue(invalid.validationErrors().containsKey("medication"))
    }

    @Test
    fun missingRequesterIsRejectedForCreation() {
        val invalid = request().copy(requestedById = "")

        assertTrue(invalid.validationErrors().containsKey("identity"))
    }

    @Test
    fun oversizedNoteIsRejectedForCreation() {
        val invalid = request().copy(note = "x".repeat(1001))

        assertTrue(invalid.validationErrors().containsKey("note"))
    }
}