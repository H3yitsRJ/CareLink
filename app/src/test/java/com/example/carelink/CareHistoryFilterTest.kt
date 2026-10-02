package com.example.carelink

import com.example.carelink.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class CareHistoryFilterTest {
    private val zone = TimeZone.getTimeZone("America/New_York")
    private fun entry(id: String, time: Long, type: CareActivityType = CareActivityType.MEDICATION) =
        CareHistoryEntry(id, "patient", time, type, id)

    @Test fun inclusiveLocalDayIncludesMidnightAndLastMillisecondAcrossDst() {
        val start = historyDateBoundary("2026-03-08", timeZone = zone)!!
        val end = historyDateBoundary("2026-03-08", true, zone)!!
        assertEquals(23 * 60 * 60 * 1000L, end - start + 1)
        val entries = listOf(entry("before", start - 1), entry("start", start), entry("end", end), entry("after", end + 1))
        assertEquals(listOf("start", "end"), CareHistoryFilter(start, end).apply(entries).map { it.id })
    }
    @Test fun multipleTypesCombineWithDateRangeAndClearRestoresEverything() {
        val entries = listOf(entry("dose", 10), entry("access", 15, CareActivityType.CAREGIVER_ACCESS), entry("task", 20, CareActivityType.CARE_TASK))
        val filter = CareHistoryFilter(10, 15, setOf(CareActivityType.MEDICATION, CareActivityType.CAREGIVER_ACCESS))
        assertEquals(listOf("dose", "access"), filter.apply(entries).map { it.id })
        assertEquals(entries, CareHistoryFilter().apply(entries))
        assertTrue(filter.isActive)
        assertFalse(CareHistoryFilter().isActive)
    }
    @Test fun malformedDatesAndReversedRangesAreRejected() {
        listOf("2026-02-30", "2026-2-01", "bad", "2026-10-01x").forEach {
            assertNull(historyDateBoundary(it, timeZone = zone))
        }
        assertNotNull(CareHistoryFilter(20, 10).validate())
        assertNull(CareHistoryFilter(10, 10).validate())
        assertTrue(CareHistoryFilter(types = setOf(CareActivityType.CARE_TASK)).apply(listOf(entry("dose", 10))).isEmpty())
    }
}
