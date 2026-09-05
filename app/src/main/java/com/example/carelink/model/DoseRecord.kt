package com.example.carelink.model

// Firestore values are lowercase and independent of Kotlin enum names.
enum class DoseStatus(val firestoreValue: String) {
    SCHEDULED("scheduled"),
    TAKEN("taken"),
    MISSED("missed"),
    DELAYED("delayed");

    companion object {
        fun fromFirestore(value: String?): DoseStatus? = entries.firstOrNull {
            it.firestoreValue == value
        }
    }
}

/**
 * One scheduled medication dose and its recorded outcome.
 *
 * SCHEDULED has no completion time. TAKEN, MISSED, and DELAYED describe a
 * completed patient action and require a completion time.
 */
data class DoseRecord(
    val id: String,
    val medicationId: String,
    val scheduledTimeMillis: Long,
    val status: DoseStatus = DoseStatus.SCHEDULED,
    val completionTimeMillis: Long? = null
) {
    init {
        require(medicationId.isNotBlank()) { "Medication ID is required" }
        require(scheduledTimeMillis > 0) { "Scheduled time must be positive" }
        require(status == DoseStatus.SCHEDULED || completionTimeMillis != null) {
            "Completed dose records require a completion time"
        }
    }

    // Keeping conversion beside the model makes the stored shape easy to review and test.
    fun toFirestore(): Map<String, Any?> = mapOf(
        "medicationId" to medicationId,
        "scheduledTimeMillis" to scheduledTimeMillis,
        "status" to status.firestoreValue,
        "completionTimeMillis" to completionTimeMillis
    )

    companion object {
        // A malformed record is skipped rather than shown as a misleading dose entry.
        fun fromFirestore(id: String, data: Map<String, Any?>): DoseRecord? {
            val medicationId = data["medicationId"] as? String ?: return null
            val scheduledTime = (data["scheduledTimeMillis"] as? Number)?.toLong() ?: return null
            val status = DoseStatus.fromFirestore(data["status"] as? String) ?: return null
            val completionTime = (data["completionTimeMillis"] as? Number)?.toLong()
            return runCatching {
                DoseRecord(id, medicationId, scheduledTime, status, completionTime)
            }.getOrNull()
        }
    }
}
