package com.example.carelink.model

/** VIEW = read, ADD = create, EDIT = update, DELETE = removal.
 * Dose recording and reminder delivery are independently representable permissions.
 * A permission's presence does not override feature-specific server authorization.
 */
enum class CarePermission {
    VIEW,
    ADD,
    EDIT,
    DELETE,
    RECORD_DOSE,
    RECEIVE_REMINDERS
}

/**
 * Required identities: [id] is the document ID, [patientId] identifies the care recipient,
 * and [caregiverId] identifies the invited caregiver. [permissions] is required and may
 * be empty (no access). Revocation overrides every permission.
 *
 * New grants default to not revoked. Stored grants must explicitly contain revoked=false
 * to allow access; absent or malformed revocation values deny access.
 * Firestore stores enum names and identities, with [id] supplied from the document path.
 */
data class CaregiverAccess(
    val id: String,
    val patientId: String,
    val caregiverId: String,
    val permissions: Set<CarePermission>,
    val revoked: Boolean = false
) {
    fun allows(permission: CarePermission) =
        !revoked && permission in permissions

    fun toFirestore(): Map<String, Any> = mapOf(
        "patientId" to patientId,
        "caregiverId" to caregiverId,
        "permissions" to permissions.sortedBy { it.ordinal }.map { it.name },
        "revoked" to revoked
    )

    companion object {
        /** Invalid required fields reject the grant. Unknown permission values grant no rights. */
        fun fromFirestore(id: String, data: Map<String, Any?>): CaregiverAccess? {
            if (id.isBlank()) return null
            val patientId = (data["patientId"] as? String)?.takeIf { it.isNotBlank() } ?: return null
            val caregiverId = (data["caregiverId"] as? String)?.takeIf { it.isNotBlank() } ?: return null
            val permissions = (data["permissions"] as? List<*>)?.mapNotNull { value ->
                CarePermission.entries.find { it.name == value }
            }?.toSet() ?: return null
            return CaregiverAccess(id, patientId, caregiverId, permissions, data["revoked"] != false)
        }
    }
}
