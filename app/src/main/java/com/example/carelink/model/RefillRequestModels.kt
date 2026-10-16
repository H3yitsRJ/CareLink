package com.example.carelink.model

enum class RefillRequestStatus {
    REQUESTED,
    PROCESSING,
    COMPLETED,
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
}

private fun validRefillId(id: String) = id.isNotBlank() && id.length <= 128 && '/' !in id && id != "." && id != ".."
