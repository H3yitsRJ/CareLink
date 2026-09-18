package com.example.carelink.notifications

import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus

/** The caller supplies an absolute reminder time after applying the agreed lead-time/time-zone policy. */
enum class AppointmentReminderResult { SCHEDULED, PERMISSION_DENIED, NOT_ELIGIBLE }

/** Small platform boundary so scheduling behavior can be tested without Firebase or a device. */
interface AppointmentReminderAlarms {
    fun notificationsAllowed(): Boolean
    fun schedule(patientId: String, appointmentId: String, triggerAtMillis: Long)
    fun cancel(patientId: String, appointmentId: String)
}

/**
 * Owns one reminder per patient/appointment. Call after a successful save, and call cancel after
 * cancellation/removal. Storage and permission prompts remain the owning features' responsibility.
 */
class AppointmentReminderScheduler(
    private val alarms: AppointmentReminderAlarms,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    fun schedule(appointment: Appointment, triggerAtMillis: Long): AppointmentReminderResult {
        if (appointment.id.isBlank() || appointment.patientId.isBlank()) {
            return AppointmentReminderResult.NOT_ELIGIBLE
        }
        // Clear stale alarms even when an edit makes an appointment ineligible or permission was revoked.
        cancel(appointment)
        if (appointment.status != AppointmentStatus.SCHEDULED || triggerAtMillis <= nowMillis()) {
            return AppointmentReminderResult.NOT_ELIGIBLE
        }
        if (!alarms.notificationsAllowed()) return AppointmentReminderResult.PERMISSION_DENIED
        alarms.schedule(appointment.patientId, appointment.id, triggerAtMillis)
        return AppointmentReminderResult.SCHEDULED
    }

    fun cancel(appointment: Appointment) {
        if (appointment.id.isNotBlank() && appointment.patientId.isNotBlank()) {
            alarms.cancel(appointment.patientId, appointment.id)
        }
    }
}
