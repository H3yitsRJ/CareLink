package com.example.carelink.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// These tests lock down the document shape used by Firestore.
class DoseRecordTest {
    @Test
    fun `round trips through Firestore data`() {
        val record = DoseRecord("dose-1", "med-1", 1_800_000L, DoseStatus.TAKEN, 1_900_000L)

        assertEquals(record, DoseRecord.fromFirestore(record.id, record.toFirestore()))
    }

    @Test
    fun `rejects unknown status`() {
        assertNull(DoseRecord.fromFirestore("dose-1", mapOf(
            "medicationId" to "med-1",
            "scheduledTimeMillis" to 1_800_000L,
            "status" to "ignored"
        )))
    }
}
