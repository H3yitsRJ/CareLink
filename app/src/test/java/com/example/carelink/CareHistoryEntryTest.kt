package com.example.carelink

import com.example.carelink.model.CareActivityType
import com.example.carelink.model.CareHistoryEntry
import org.junit.Assert.*
import org.junit.Test

class CareHistoryEntryTest {
    private fun event(
        id: String = "event-1",
        time: Long = 1000L,
        type: CareActivityType = CareActivityType.MEDICATION,
        relatedId: String? = "record-1"
    ) = CareHistoryEntry(
        id = id,
        patientId = "patient-1",
        occurredAtMillis = time,
        type = type,
        summary = "Activity recorded",
        actorId = "caregiver-1",
        relatedRecordId = relatedId
    )

    @Test
    fun everySupportedTypeRoundTripsThroughFirestore() {
        CareActivityType.entries.forEach { type ->
            val original = event(type = type)
            assertEquals(
                original,
                CareHistoryEntry.fromFirestore(
                    original.id,
                    original.toFirestore()
                )
            )
        }
    }

    @Test
    fun relatedRecordIsOptional() {
        val original = event(relatedId = null)
        val data = original.toFirestore()

        assertFalse(data.containsKey("relatedRecordId"))
        assertEquals(
            original,
            CareHistoryEntry.fromFirestore(original.id, data)
        )
    }

    @Test
    fun malformedEventsAreRejected() {
        val valid = event().toFirestore()

        for (field in listOf(
            "patientId", "actorId", "occurredAtMillis", "type", "summary"
        )) {
            assertNull(
                "Missing $field must be rejected",
                CareHistoryEntry.fromFirestore("event-1", valid - field)
            )
        }

        assertNull(CareHistoryEntry.fromFirestore("", valid))
        assertNull(CareHistoryEntry.fromFirestore(
            "event-1", valid + ("type" to "UNKNOWN")
        ))
        assertNull(CareHistoryEntry.fromFirestore(
            "event-1", valid + ("actorId" to " ")
        ))
        assertNull(CareHistoryEntry.fromFirestore(
            "event-1", valid + ("relatedRecordId" to 123L)
        ))
        assertNull(CareHistoryEntry.fromFirestore(
            "event-1", valid + ("occurredAtMillis" to "yesterday")
        ))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownActorCannotBeWrittenAsANewEvent() {
        event().copy(actorId = null).toFirestore()
    }

    @Test
    fun chronologyIsNewestFirstWithStableTies() {
        val entries = listOf(
            event("old", 100L),
            event("b", 200L),
            event("new", 300L),
            event("a", 200L)
        )

        assertEquals(
            listOf("new", "a", "b", "old"),
            CareHistoryEntry.newestFirst(entries).map { it.id }
        )
    }
}