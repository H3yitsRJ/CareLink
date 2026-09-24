package com.example.carelink.notifications

import java.util.Calendar
import java.util.Locale

/** Wall-clock schedules follow the device time zone, including daylight-saving changes. */
internal object MedicationSchedule {
    fun validTime(time: String) = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$").matches(time)

    fun requiredTimes(frequency: String): Int? = when (frequency.trim().lowercase(Locale.ROOT)) {
        "once daily", "once a day" -> 1
        "twice daily", "twice a day" -> 2
        "three times daily", "3 times daily" -> 3
        "four times daily", "4 times daily" -> 4
        else -> null
    }

    fun days(frequency: String): Set<Int>? {
        val value = frequency.trim().lowercase(Locale.ROOT)
        if (value in setOf("daily", "every day", "once daily", "once a day", "twice daily", "twice a day",
                "three times daily", "3 times daily", "four times daily", "4 times daily")) return (1..7).toSet()
        val names = mapOf("sun" to 1, "mon" to 2, "tue" to 3, "wed" to 4, "thu" to 5, "fri" to 6, "sat" to 7)
        val parts = value.split(',').map { it.trim() }
        if (parts.any { it !in names }) return null
        return parts.map { names.getValue(it) }.toSet()
    }

    fun next(time: String, frequency: String, afterMillis: Long): Long? {
        if (!validTime(time)) return null
        val days = days(frequency) ?: return null
        val (hour, minute) = time.split(':').map(String::toInt)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = afterMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        repeat(8) {
            if (calendar.timeInMillis > afterMillis && calendar.get(Calendar.DAY_OF_WEEK) in days) return calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
        }
        return null
    }
}
