
package com.example.carelink

import com.example.carelink.model.DashboardMedicationAttention
import com.example.carelink.model.DoseRecord
import com.example.carelink.model.DoseStatus
import com.example.carelink.model.Medication
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class DashboardMedicationAttentionTest {

    private fun todayAt(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun medication(
        time: String,
        frequency: String = "once daily"
    ) = Medication(
        id = "med-1",
        patientId = "patient-1",
        name = "Test Medication",
        strength = "10 mg",
        dose = "1 tablet",
        frequency = frequency,
        reminderTimes = listOf(time)
    )

    @Test
    fun overdueDoseIsCounted() {
        val now = todayAt(12, 0)

        val count = DashboardMedicationAttention.overdueCount(
            medications = listOf(medication("09:00")),
            doseRecords = emptyList(),
            nowMillis = now
        )

        assertEquals(1, count)
    }

    @Test
    fun takenDoseIsNotOverdue() {
        val scheduled = todayAt(9, 0)
        val now = todayAt(12, 0)

        val taken = DoseRecord.completed(
            medicationId = "med-1",
            scheduledTimeMillis = scheduled,
            status = DoseStatus.TAKEN,
            completedAtMillis = todayAt(9, 5)
        )

        val count = DashboardMedicationAttention.overdueCount(
            medications = listOf(medication("09:00")),
            doseRecords = listOf(taken),
            nowMillis = now
        )

        assertEquals(0, count)
    }

    @Test
    fun futureDoseIsNotOverdue() {
        val count = DashboardMedicationAttention.overdueCount(
            medications = listOf(medication("18:00")),
            doseRecords = emptyList(),
            nowMillis = todayAt(12, 0)
        )

        assertEquals(0, count)
    }

    @Test
    fun medicationOnAnotherDayIsNotOverdue() {
        val now = todayAt(12, 0)

        val today = Calendar.getInstance().apply {
            timeInMillis = now
        }.get(Calendar.DAY_OF_WEEK)

        val otherDay = if (today == Calendar.MONDAY) {
            "tue"
        } else {
            "mon"
        }

        val count = DashboardMedicationAttention.overdueCount(
            medications = listOf(
                medication("09:00", otherDay)
            ),
            doseRecords = emptyList(),
            nowMillis = now
        )

        assertEquals(0, count)
    }
}
