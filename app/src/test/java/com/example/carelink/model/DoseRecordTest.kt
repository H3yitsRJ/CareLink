package com.example.carelink.model

import org.junit.Assert.*
import org.junit.Test

class DoseRecordTest {
    @Test fun everyOutcomeRoundTripsAndDaysHaveDifferentIds() {
        listOf(DoseStatus.TAKEN, DoseStatus.MISSED, DoseStatus.DELAYED, DoseStatus.SKIPPED).forEach { status ->
            val first = DoseRecord.completed("med-1", 1_800_000_000_000L, status, 1_800_000_001_000L)
            assertEquals(first, DoseRecord.fromFirestore(first.id, first.toFirestore()))
            assertNotEquals(first.id, DoseRecord.completed("med-1", 1_800_086_400_000L, status).id)
        }
    }
    @Test fun rejectsMalformedHistory() {
        assertNull(DoseRecord.fromFirestore("bad", mapOf("medicationId" to "med", "scheduledTime" to "08:00", "status" to "taken")))
        val data = DoseRecord.completed("med", 1000, DoseStatus.TAKEN, 2000).toFirestore()
        assertNull(DoseRecord.fromFirestore("bad", data + ("status" to "unknown")))
        assertNull(DoseRecord.fromFirestore("bad", data + ("completionTimeMillis" to -1L)))
    }
}
