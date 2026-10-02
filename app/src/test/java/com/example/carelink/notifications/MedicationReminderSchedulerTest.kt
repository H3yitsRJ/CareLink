package com.example.carelink.notifications

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class MedicationReminderSchedulerTest {
    @Test fun recurrenceAdvancesAndHonorsWeekdays() {
        val monday = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 21, 8, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val daily = MedicationSchedule.next("08:30", "Daily", monday)!!
        assertEquals(22, Calendar.getInstance().apply { timeInMillis = daily }.get(Calendar.DAY_OF_MONTH))
        val weekly = MedicationSchedule.next("08:30", "Mon", monday)!!
        assertEquals(28, Calendar.getInstance().apply { timeInMillis = weekly }.get(Calendar.DAY_OF_MONTH))
        assertNull(MedicationSchedule.next("08:30", "whenever", monday))
    }
    @Test fun rejectsMalformedTime() {
        assertNull(AndroidMedicationReminderScheduler.nextOccurrence("08:30:22", 1_800_000L))
    }

    @Test fun nextOccurrenceIsFutureAndKeepsLocalTime() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val next = AndroidMedicationReminderScheduler.nextOccurrence("08:30", now)!!
        assertTrue(next > now)
        val date = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(21, date.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, date.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, date.get(Calendar.MINUTE))
    }
}
