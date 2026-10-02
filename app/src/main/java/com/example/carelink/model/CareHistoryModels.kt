package com.example.carelink.model

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class CareActivityType(val label: String) {
    MEDICATION("Medication"),
    APPOINTMENT("Appointment"),
    HEALTH_CONCERN("Health concern"),
    CARE_TASK("Care task"),
    CAREGIVER_ACCESS("Caregiver access")
}

data class CareHistoryEntry(
    val id: String,
    val patientId: String,
    val occurredAtMillis: Long,
    val type: CareActivityType,
    val summary: String
)

data class CareHistoryFilter(
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val types: Set<CareActivityType> = emptySet()
) {
    val isActive: Boolean get() = startMillis != null || endMillis != null || types.isNotEmpty()

    fun validate(): String? =
        if (startMillis != null && endMillis != null && startMillis > endMillis) {
            "Start date must be on or before end date."
        } else null

    fun apply(entries: List<CareHistoryEntry>): List<CareHistoryEntry> {
        require(validate() == null) { validate()!! }
        return entries.filter { entry ->
            (startMillis == null || entry.occurredAtMillis >= startMillis) &&
                (endMillis == null || entry.occurredAtMillis <= endMillis) &&
                (types.isEmpty() || entry.type in types)
        }
    }
}

/** Calendar dates are interpreted in the patient's current timezone, including DST days. */
fun historyDateBoundary(
    text: String,
    endOfDay: Boolean = false,
    timeZone: TimeZone = TimeZone.getDefault()
): Long? {
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(text)) return null
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        this.timeZone = timeZone
    }
    val position = ParsePosition(0)
    val date = format.parse(text, position) ?: return null
    if (position.index != text.length) return null
    if (!endOfDay) return date.time
    return Calendar.getInstance(timeZone).apply {
        time = date
        add(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis - 1
}
