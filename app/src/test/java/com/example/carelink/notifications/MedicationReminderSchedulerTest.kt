package com.example.carelink.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

// Pure scheduling helpers are tested here because AlarmManager itself needs a device.
class MedicationReminderSchedulerTest {
    @Test
    fun `uses a stable unique identifier per medication and time`() {
        val first = AndroidMedicationReminderScheduler.reminderId("med-1", "08:00")
        assertEquals(first, AndroidMedicationReminderScheduler.reminderId("med-1", "08:00"))
        assertNotEquals(first, AndroidMedicationReminderScheduler.reminderId("med-1", "20:00"))
        assertNotEquals(first, AndroidMedicationReminderScheduler.reminderId("med-2", "08:00"))
    }

    @Test
    fun `schedules the next occurrence in the future`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 3, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val next = AndroidMedicationReminderScheduler.nextOccurrence("08:30", now)!!
        assertTrue(next > now)
        assertEquals(8, Calendar.getInstance().apply { timeInMillis = next }.get(Calendar.HOUR_OF_DAY))
    }
}
