package com.example.carelink.model

enum class AppointmentStatus { SCHEDULED, COMPLETED, CANCELLED }

data class Appointment(
    val id: String, val patientId: String, val title: String, val date: String, val time: String,
    val provider: String = "", val location: String = "", val notes: String = "",
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED
)

enum class ConcernSeverity { LOW, MEDIUM, HIGH }
enum class ConcernStatus { ACTIVE, DISCUSSED }

data class HealthConcern(
    val id: String, val patientId: String, val title: String, val severity: ConcernSeverity,
    val recordedDate: String, val status: ConcernStatus = ConcernStatus.ACTIVE
)

// appointmentId is optional because some care tasks start from an appointment and others do not.
data class CareTask(
    val id: String, val patientId: String, val title: String, val dueDate: String = "",
    val completed: Boolean = false, val appointmentId: String? = null
) {
    fun toFirestore(): Map<String, Any?> = mapOf(
        "patientId" to patientId, "title" to title, "dueDate" to dueDate,
        "completed" to completed, "appointmentId" to appointmentId
    )

    companion object {
        fun fromFirestore(id: String, data: Map<String, Any?>): CareTask? {
            val patientId = data["patientId"] as? String ?: return null
            val title = data["title"] as? String ?: return null
            return CareTask(
                id = id,
                patientId = patientId,
                title = title,
                dueDate = data["dueDate"] as? String ?: "",
                completed = data["completed"] as? Boolean ?: false,
                appointmentId = data["appointmentId"] as? String
            )
        }
    }
}

enum class InvitationStatus(val firestoreValue: String) {
    PENDING("pending"), ACCEPTED("accepted"), DECLINED("declined"), REVOKED("revoked")
}

// Invitations expire even if nobody explicitly declines them.
data class CaregiverInvitation(
    val id: String, val senderId: String, val recipientEmail: String, val patientId: String,
    val status: InvitationStatus, val expiresAtMillis: Long
) {
    fun isExpired(nowMillis: Long = System.currentTimeMillis()) = nowMillis >= expiresAtMillis
    fun toFirestore(): Map<String, Any> = mapOf(
        "senderId" to senderId, "recipientEmail" to recipientEmail, "patientId" to patientId,
        "status" to status.firestoreValue, "expiresAtMillis" to expiresAtMillis
    )

    companion object {
        fun fromFirestore(id: String, data: Map<String, Any?>): CaregiverInvitation? {
            val status = InvitationStatus.entries.firstOrNull { it.firestoreValue == data["status"] } ?: return null
            return CaregiverInvitation(
                id, data["senderId"] as? String ?: return null,
                data["recipientEmail"] as? String ?: return null,
                data["patientId"] as? String ?: return null, status,
                (data["expiresAtMillis"] as? Number)?.toLong() ?: return null
            )
        }
    }
}

enum class CarePermission { VIEW, ADD, EDIT, DELETE, RECORD_DOSE, RECEIVE_REMINDERS }

// Access is kept as a set of small permissions so the patient does not have to grant everything.
data class CaregiverAccess(
    val id: String, val patientId: String, val caregiverId: String,
    val permissions: Set<CarePermission>, val revoked: Boolean = false
) {
    fun allows(permission: CarePermission) = !revoked && permission in permissions
}

enum class CareActivityType { MEDICATION, APPOINTMENT, HEALTH_CONCERN, CARE_TASK, CAREGIVER_ACCESS }

data class CareHistoryEntry(
    val id: String, val patientId: String, val occurredAtMillis: Long,
    val type: CareActivityType, val summary: String
)

// Filtering is plain Kotlin so it can be tested without Compose or Firebase.
data class CareHistoryFilter(
    val startMillis: Long? = null, val endMillis: Long? = null,
    val types: Set<CareActivityType> = emptySet()
) {
    fun validate(): String? = if (startMillis != null && endMillis != null && startMillis > endMillis) "Start date must be before end date" else null
    fun apply(entries: List<CareHistoryEntry>): List<CareHistoryEntry> {
        require(validate() == null) { validate()!! }
        return entries.filter { entry ->
            (startMillis == null || entry.occurredAtMillis >= startMillis) &&
                (endMillis == null || entry.occurredAtMillis <= endMillis) &&
                (types.isEmpty() || entry.type in types)
        }
    }
}

enum class RefillRequestStatus { REQUESTED, PROCESSING, COMPLETED, CANCELLED }

data class RefillRequest(
    val id: String, val patientId: String, val medicationId: String,
    val requestedById: String, val note: String = "",
    val status: RefillRequestStatus = RefillRequestStatus.REQUESTED
)
