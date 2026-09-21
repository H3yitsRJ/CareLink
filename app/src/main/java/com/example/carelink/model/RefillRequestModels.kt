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
    val status: RefillRequestStatus = RefillRequestStatus.REQUESTED
)