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

/**
 * Shared history event.
 * actorId identifies the user who performed the action.
 * relatedRecordId identifies the affected record, when available.
 * Older display entries may have an unknown actor.
 */
data class CareHistoryEntry(
    val id: String,
    val patientId: String,
    val occurredAtMillis: Long,
    val type: CareActivityType,
    val summary: String,
    val actorId: String? = null,
    val relatedRecordId: String? = null
) {
    /** The event ID is stored as the Firestore document ID. */
    fun toFirestore(): Map<String, Any> {
        require(id.isNotBlank()) { "Event ID is required." }
        require(patientId.isNotBlank()) { "Patient ID is required." }
        require(!actorId.isNullOrBlank()) { "Acting user ID is required." }
        require(summary.isNotBlank()) { "Event description is required." }
        require(relatedRecordId == null || relatedRecordId.isNotBlank()) {
            "Related record ID must not be blank."
        }

        return mutableMapOf<String, Any>(
            "patientId" to patientId,
            "actorId" to requireNotNull(actorId),
            "occurredAtMillis" to occurredAtMillis,
            "type" to type.name,
            "summary" to summary
        ).apply {
            relatedRecordId?.let { put("relatedRecordId", it) }
        }
    }

    companion object {
        fun fromFirestore(
            id: String,
            data: Map<String, Any?>
        ): CareHistoryEntry? {
            fun text(key: String): String? =
                (data[key] as? String)?.takeIf { it.isNotBlank() }

            if (id.isBlank()) return null
            val patientId = text("patientId") ?: return null
            val actorId = text("actorId") ?: return null
            val summary = text("summary") ?: return null
            val time = data["occurredAtMillis"] as? Long ?: return null
            val type = CareActivityType.entries.firstOrNull {
                it.name == data["type"]
            } ?: return null

            val relatedId = data["relatedRecordId"]
            if (relatedId != null &&
                (relatedId !is String || relatedId.isBlank())
            ) return null

            return CareHistoryEntry(
                id = id,
                patientId = patientId,
                occurredAtMillis = time,
                type = type,
                summary = summary,
                actorId = actorId,
                relatedRecordId = relatedId as? String
            )
        }

        /** Newest first; event ID provides consistent ordering for ties. */
        fun newestFirst(entries: List<CareHistoryEntry>): List<CareHistoryEntry> =
            entries.sortedWith(
                compareByDescending<CareHistoryEntry> { it.occurredAtMillis }
                    .thenBy { it.id }
            )
    }
}

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
