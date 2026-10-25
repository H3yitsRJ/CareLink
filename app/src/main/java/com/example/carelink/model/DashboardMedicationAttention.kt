package com.example.carelink.model

import java.util.Calendar

object DashboardMedicationAttention {

    private const val GRACE_PERIOD_MINUTES = 30L

    fun overdueCount(
        medications: List<Medication>,
        doseRecords: List<DoseRecord>,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        val now = Calendar.getInstance().apply {
            timeInMillis = nowMillis
        }

        val today = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val completed = doseRecords
            .filter { it.status != DoseStatus.SCHEDULED }
            .map { it.medicationId to it.scheduledTimeMillis }
            .toSet()

        var count = 0

        medications.filter { it.active }.forEach { medication ->

            val scheduledDays = medication.frequency
                .trim()
                .lowercase()

            val todayName = when (now.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "sun"
                Calendar.MONDAY -> "mon"
                Calendar.TUESDAY -> "tue"
                Calendar.WEDNESDAY -> "wed"
                Calendar.THURSDAY -> "thu"
                Calendar.FRIDAY -> "fri"
                else -> "sat"
            }

            val dailyFrequencies = setOf(
                "daily", "every day",
                "once daily", "once a day",
                "1 time daily", "1 time a day",
                "twice daily", "twice a day",
                "2 times daily", "2 times a day",
                "three times daily", "three times a day",
                "3 times daily", "3 times a day",
                "four times daily", "four times a day",
                "4 times daily", "4 times a day"
            )

            val isScheduledToday =
                scheduledDays in dailyFrequencies ||
                        scheduledDays.split(",").map { it.trim() }
                            .contains(todayName)

            if (!isScheduledToday) return@forEach
            medication.reminderTimes.forEach { time ->
                val parts = time.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull()
                val minute = parts.getOrNull(1)?.toIntOrNull()

                if (hour != null && minute != null &&
                    hour in 0..23 && minute in 0..59
                ) {
                    val scheduled = (today.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                    }.timeInMillis

                    val graceMillis = GRACE_PERIOD_MINUTES * 60_000L

                    if (
                        scheduled <= now.timeInMillis - graceMillis &&
                        (medication.id to scheduled) !in completed
                    ) {
                        count++
                    }
                }
            }
        }

        return count
    }
}