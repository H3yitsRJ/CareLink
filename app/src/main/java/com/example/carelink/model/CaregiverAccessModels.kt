package com.example.carelink.model

enum class CarePermission {
    VIEW,
    ADD,
    EDIT,
    DELETE,
    RECORD_DOSE,
    RECEIVE_REMINDERS
}

data class CaregiverAccess(
    val id: String,
    val patientId: String,
    val caregiverId: String,
    val permissions: Set<CarePermission>,
    val revoked: Boolean = false
) {
    fun allows(permission: CarePermission) =
        !revoked && permission in permissions
}