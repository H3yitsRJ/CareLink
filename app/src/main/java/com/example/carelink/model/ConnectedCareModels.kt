package com.example.carelink.model

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
    val id: String,
    val patientId: String,
    val title: String,
    val description: String = "",
    val dueDate: String = "",
    val time: String = "",
    val completed: Boolean = false,
    val appointmentId: String? = null
) {
    fun toFirestore(): Map<String, Any?> = mapOf(
        "patientId" to patientId,
        "title" to title,
        "description" to description,
        "dueDate" to dueDate,
        "time" to time,
        "completed" to completed,
        "appointmentId" to appointmentId
    )

    companion object {
        fun fromFirestore(id: String, data: Map<String, Any?>): CareTask? {
            val patientId = data["patientId"] as? String ?: return null
            val title = data["title"] as? String ?: return null

            return CareTask(
                id = id,
                patientId = patientId,
                title = title,
                description = data["description"] as? String ?: "",
                dueDate = data["dueDate"] as? String ?: "",
                time = data["time"] as? String ?: "",
                completed = data["completed"] as? Boolean ?: false,
                appointmentId = data["appointmentId"] as? String
            )
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
                val status =
                    InvitationStatus.entries.firstOrNull { it.firestoreValue == data["status"] }
                        ?: return null
                return CaregiverInvitation(
                    id, data["senderId"] as? String ?: return null,
                    data["recipientEmail"] as? String ?: return null,
                    data["patientId"] as? String ?: return null, status,
                    (data["expiresAtMillis"] as? Number)?.toLong() ?: return null
                )
            }
        }
    }
}



