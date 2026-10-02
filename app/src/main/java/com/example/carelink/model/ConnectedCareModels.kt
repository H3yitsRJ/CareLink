package com.example.carelink.model

internal fun validCareDocumentId(id: String) = id.isNotBlank() && '/' !in id && id != "." && id != ".."

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

/**
 * A care recipient's recorded health concern.
 *
 * Required: [id] (Firestore document ID), [patientId] (owner), [title],
 * [severity], and [recordedDate] (YYYY-MM-DD). Required text must be nonblank when read.
 * [status] defaults to ACTIVE for new concerns and documents without a status field.
 * [appointmentId] is optional; null means the concern is not linked to an appointment.
 * [description] contains optional details; missing or malformed values become empty text.
 * Severity and status are stored as the exact enum names defined above.
 * The document ID is supplied separately on read and is not duplicated in document data.
 */
data class HealthConcern(
    val id: String,
    val patientId: String,
    val title: String,
    val severity: ConcernSeverity,
    val recordedDate: String,
    val description: String,
    val status: ConcernStatus = ConcernStatus.ACTIVE,
    val appointmentId: String? = null
) {
    fun toFirestore(): Map<String, Any?> = mapOf(
        "patientId" to patientId,
        "title" to title,
        "description" to description,
        "severity" to severity.name,
        "recordedDate" to recordedDate,
        "status" to status.name,
        "appointmentId" to appointmentId
    )

    companion object {
        /**
         * Returns null for missing/invalid required fields or unsupported enum values,
         * rather than inventing a severity or silently changing a recorded status.
         * Missing, null, blank, or wrongly typed optional appointment IDs become null.
         */
        fun fromFirestore(id: String, data: Map<String, Any?>): HealthConcern? {
            if (id.isBlank()) return null
            fun requiredText(key: String) = (data[key] as? String)?.takeIf { it.isNotBlank() }
            val severity = ConcernSeverity.entries.find { it.name == data["severity"] } ?: return null
            val status = if (!data.containsKey("status")) ConcernStatus.ACTIVE
                else ConcernStatus.entries.find { it.name == data["status"] } ?: return null
            return HealthConcern(
                id = id,
                patientId = requiredText("patientId") ?: return null,
                title = requiredText("title") ?: return null,
                description = (data["description"] as? String).orEmpty(),
                severity = severity,
                recordedDate = requiredText("recordedDate") ?: return null,
                status = status,
                appointmentId = (data["appointmentId"] as? String)?.takeIf { it.isNotBlank() }
            )
        }
    }
}

enum class CareTaskStatus { PENDING, COMPLETED }

// Required: id, patientId, and title.
// Optional: description, dueDate, time, and appointmentId.
// A new task starts pending unless completed is explicitly set.
data class CareTask(
    val id: String,
    val patientId: String,
    val title: String,
    val description: String = "",
    val dueDate: String = "",
    val time: String = "",
    val completed: Boolean = false,
    val appointmentId: String? = null
) {
    val status: CareTaskStatus
        get() = if (completed) CareTaskStatus.COMPLETED else CareTaskStatus.PENDING

    fun toFirestore(): Map<String, Any?> = mapOf(
        "patientId" to patientId,
        "title" to title,
        "description" to description,
        "dueDate" to dueDate,
        "time" to time,
        "completed" to completed,
        "status" to status.name,
        "appointmentId" to appointmentId
    )

    companion object {
        fun fromFirestore(id: String, data: Map<String, Any?>): CareTask? {
            val patientId = data["patientId"] as? String ?: return null
            val title = data["title"] as? String ?: return null

            // Older records may have "completed" but no "status".
            val completed = when (data["status"] as? String) {
                CareTaskStatus.COMPLETED.name -> true
                CareTaskStatus.PENDING.name -> false
                else -> data["completed"] as? Boolean ?: false
            }

            return CareTask(
                id = id,
                patientId = patientId,
                title = title,
                dueDate = data["dueDate"] as? String ?: "",
                completed = completed,
                appointmentId = data["appointmentId"] as? String,
                description = data["description"] as? String ?: "",
                time = data["time"] as? String
                    ?: data["dueTime"] as? String
                    ?: ""
            )
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
        listOf(id, senderId, patientId).any { !validCareDocumentId(it) } -> "Invitation, sender and patient IDs are required"
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
