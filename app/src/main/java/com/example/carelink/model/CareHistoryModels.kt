package com.example.carelink.model

enum class CareActivityType {
    MEDICATION,
    APPOINTMENT,
    HEALTH_CONCERN,
    CARE_TASK,
    CAREGIVER_ACCESS
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
    fun validate(): String? =
        if (startMillis != null && endMillis != null && startMillis > endMillis) {
            "Start date must be before end date"
        } else {
            null
        }

    fun apply(entries: List<CareHistoryEntry>): List<CareHistoryEntry> {
        require(validate() == null) { validate()!! }

        return entries.filter { entry ->
            (startMillis == null || entry.occurredAtMillis >= startMillis) &&
                    (endMillis == null || entry.occurredAtMillis <= endMillis) &&
                    (types.isEmpty() || entry.type in types)
        }
    }
}