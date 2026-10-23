package com.example.carelink.model

import com.google.firebase.Timestamp

enum class RefillRequestStatus {
    /** The request has been submitted and is awaiting processing. */
    REQUESTED,

    /** The request is being handled. */
    PROCESSING,

    /** The refill request has been fulfilled. */
    COMPLETED,

    /** The request was cancelled and is no longer active. */
    CANCELLED
}

data class RefillRequest(
    val id: String,
    val patientId: String,
    val medicationId: String,
    val requestedById: String,
    val note: String = "",
    val status: RefillRequestStatus = RefillRequestStatus.REQUESTED,
    val requestedAt: Long = System.currentTimeMillis(),
    val medicationName: String = ""
) {
    fun validationErrors(): Map<String, String> = buildMap {
        if (!validRefillId(patientId) || !validRefillId(requestedById)) put("identity", "Patient and caregiver are required")
        if (!validRefillId(medicationId) || id != medicationId) put("medication", "Choose a medication")
        if (note.length > 1000) put("note", "Use up to 1000 characters")
        if (status != RefillRequestStatus.REQUESTED) put("status", "New requests must be requested")
    }

    fun toFirestore(): Map<String, Any> = mapOf(
        "patientId" to patientId, "medicationId" to medicationId,
        "requestedById" to requestedById, "note" to note, "status" to status.name
    ) + if (medicationName.isNotBlank()) mapOf("medicationName" to medicationName) else emptyMap()

    companion object {
        /**
         * Reads a refill request using the document ID and patient path.
         * Supports Firestore timestamps and legacy millisecond dates.
         * Missing dates are represented by 0 rather than the current time.
         */
        fun fromFirestore(
            id: String,
            patientId: String,
            data: Map<String, Any>
        ): RefillRequest {
            return RefillRequest(
                id = id,
                patientId = patientId,
                medicationId = data["medicationId"] as? String ?: "",
                requestedById = data["requestedById"] as? String ?: "",
                medicationName = data["medicationName"] as? String ?: "",
                note = data["note"] as? String ?: "",
                status = runCatching {
                    RefillRequestStatus.valueOf(
                        data["status"] as? String ?: "REQUESTED"
                    )
                }.getOrDefault(RefillRequestStatus.REQUESTED),
                requestedAt = when (val value = data["requestedAt"]) {
                    is Timestamp -> value.toDate().time
                    is Number -> value.toLong()
                    else -> 0L
                }
            )
        }
    }

}

private fun validRefillId(id: String) = id.isNotBlank() && id.length <= 128 && '/' !in id && id != "." && id != ".."
