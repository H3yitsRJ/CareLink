// Shared appointment, health-concern, care-task, caregiver-access, history, and refill models.
// Serialization helpers define document fields; they do not perform writes or enforce server
// permissions.

package com.example.carelink.model

internal fun isValidCareDocumentId(id: String) =
    id.isNotBlank() && '/' !in id && id != "." && id != ".."

enum class AppointmentStatus { SCHEDULED, COMPLETED, CANCELLED }

data class Appointment(
    val id: String,
    val patientId: String,
    val title: String,
    val date: String,
    val time: String,
    val provider: String = "",
    val location: String = "",
    val notes: String = "",
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED
) {
    fun toFirestore(): Map<String, Any?> = mapOf(
        "patientId" to patientId,
        "title" to title,
        "date" to date,
        "time" to time,
        "provider" to provider,
        "location" to location,
        "notes" to notes,
        "status" to status.name
    )

    companion object {
        fun fromFirestore(
            id: String,
            data: Map<String, Any?>
        ): Appointment {
            val statusValue = data["status"] as? String

            val parsedStatus = try {
                if (statusValue != null) {
                    AppointmentStatus.valueOf(statusValue)
                } else {
                    AppointmentStatus.SCHEDULED
                }
            } catch (_: IllegalArgumentException) {
                AppointmentStatus.SCHEDULED
            }

            return Appointment(
                id = id,
                patientId = data["patientId"] as? String ?: "",
                title = data["title"] as? String ?: "",
                date = data["date"] as? String ?: "",
                time = data["time"] as? String ?: "",
                provider = data["provider"] as? String ?: "",
                location = data["location"] as? String ?: "",
                notes = data["notes"] as? String ?: "",
                status = parsedStatus
            )
        }
    }
}

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
    fun validate(): String? = when {
        !isValidCareDocumentId(id) -> "Care task ID is required"
        !isValidCareDocumentId(patientId) -> "Patient ID is required"
        title.isBlank() -> "Enter a task"
        appointmentId != null && !isValidCareDocumentId(appointmentId) -> "Invalid appointment reference"
        else -> null
    }

    fun toFirestore(): Map<String, Any?> = mapOf(
        "patientId" to patientId, "title" to title, "dueDate" to dueDate,
        "completed" to completed, "appointmentId" to appointmentId
    )

    companion object {
        fun fromFirestore(id: String, data: Map<String, Any?>): CareTask? {
            val patientId = data["patientId"] as? String ?: return null
            val title = data["title"] as? String ?: return null
            // Missing optional fields retain their defaults; malformed values are not records.
            if (data["dueDate"] != null && data["dueDate"] !is String) return null
            if (data["completed"] != null && data["completed"] !is Boolean) return null
            if (data["appointmentId"] != null && data["appointmentId"] !is String) return null
            return CareTask(
                id = id,
                patientId = patientId,
                title = title,
                dueDate = data["dueDate"] as? String ?: "",
                completed = data["completed"] as? Boolean ?: false,
                appointmentId = data["appointmentId"] as? String
            ).takeIf { it.validate() == null }
        }
    }
}

enum class InvitationStatus(val firestoreValue: String) {
    // pending: awaiting a response; accepted: recipient agreed; declined: recipient refused;
    // revoked: sender withdrew the invitation. Expiration is evaluated separately from status.
    PENDING("pending"), ACCEPTED("accepted"), DECLINED("declined"), REVOKED("revoked")
}

// Invitations expire even if nobody explicitly declines them.
data class CaregiverInvitation(
    val id: String, val senderId: String, val recipientEmail: String, val patientId: String,
    val status: InvitationStatus, val expiresAtMillis: Long
) {
    fun isExpired(nowMillis: Long = System.currentTimeMillis()) = nowMillis >= expiresAtMillis
    fun validate(): String? = when {
        listOf(id, senderId, patientId).any { !isValidCareDocumentId(it) } -> "Invitation, sender and patient IDs are required"
        !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(recipientEmail) -> "Enter a recipient email"
        expiresAtMillis <= 0 -> "Expiration must be a positive epoch timestamp"
        else -> null
    }
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
                (data["expiresAtMillis"] as? Long) ?: return null
            ).takeIf { it.validate() == null }
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

    fun toFirestore(): Map<String, Any> = mapOf(
        "patientId" to patientId,
        "caregiverId" to caregiverId,
        "permissions" to permissions.map(CarePermission::name).sorted(),
        "revoked" to revoked
    )

    companion object {
        fun fromFirestore(id: String, data: Map<String, Any?>): CaregiverAccess? {
            val patientId = data["patientId"] as? String ?: return null
            val caregiverId = data["caregiverId"] as? String ?: return null
            val permissions = (data["permissions"] as? List<*>)
                ?.mapNotNull { value -> CarePermission.entries.firstOrNull { it.name == value } }
                ?.toSet().orEmpty()
            return CaregiverAccess(
                id = id,
                patientId = patientId,
                caregiverId = caregiverId,
                permissions = permissions,
                revoked = data["revoked"] as? Boolean ?: false
            )
        }
    }
}

enum class CareActivityType { MEDICATION, APPOINTMENT, HEALTH_CONCERN, CARE_TASK, CAREGIVER_ACCESS }

data class CareHistoryEntry(
    val id: String, val patientId: String, val occurredAtMillis: Long,
    val type: CareActivityType, val summary: String, val changedById: String = ""
) {
    fun toFirestore(): Map<String, Any> = mapOf(
        "patientId" to patientId,
        "occurredAtMillis" to occurredAtMillis,
        "type" to type.name,
        "summary" to summary,
        "changedById" to changedById
    )
}

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
