package com.example.carelink.data

import com.example.carelink.model.CareActivityType
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class CaregiverHistoryEntryTest {
    private val event = mapOf<String, Any>(
        "patientId" to "patient", "type" to "CAREGIVER_ACCESS",
        "action" to "REVOKED", "occurredAt" to Timestamp(100L, 0),
        "title" to "untrusted display text"
    )
    @Test fun accessEventsUseCanonicalSummaryAndTimestamp() {
        val entry = caregiverHistoryEntry("event", "patient", event)!!
        assertEquals("access:event", entry.id)
        assertEquals(100_000L, entry.occurredAtMillis)
        assertEquals(CareActivityType.CAREGIVER_ACCESS, entry.type)
        assertEquals("Caregiver access revoked", entry.summary)
    }
    @Test fun malformedAndWrongPatientEventsAreExcluded() {
        assertNull(caregiverHistoryEntry("event", "other", event))
        assertNull(caregiverHistoryEntry("event", "patient", event - "occurredAt"))
        assertNull(caregiverHistoryEntry("event", "patient", event + ("action" to "UNKNOWN")))
    }
}
