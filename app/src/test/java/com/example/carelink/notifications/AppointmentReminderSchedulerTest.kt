package com.example.carelink.notifications

import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus
import org.junit.Assert.*
import org.junit.Test

class AppointmentReminderSchedulerTest {
    // A fake alarm store verifies externally observable replacement/cancellation without Android or Firebase.
    private class Alarms : AppointmentReminderAlarms {
        var allowed = true
        val scheduled = mutableMapOf<Pair<String, String>, Long>()
        override fun notificationsAllowed() = allowed
        override fun schedule(patientId: String, appointmentId: String, triggerAtMillis: Long) {
            scheduled[patientId to appointmentId] = triggerAtMillis
        }
        override fun cancel(patientId: String, appointmentId: String) {
            scheduled.remove(patientId to appointmentId)
        }
    }

    private val appointment = Appointment("visit", "patient", "Checkup", "2026-09-20", "10:00")
    private val alarms = Alarms()
    private val scheduler = AppointmentReminderScheduler(alarms) { 1000L }

    @Test fun schedulesAndReplacesOnlyTheSelectedAppointment() {
        assertEquals(AppointmentReminderResult.SCHEDULED, scheduler.schedule(appointment, 2000L))
        scheduler.schedule(appointment.copy(id = "other"), 3000L)
        scheduler.schedule(appointment.copy(patientId = "other-patient"), 4000L)
        scheduler.schedule(appointment, 5000L)
        assertEquals(mapOf(("patient" to "visit") to 5000L, ("patient" to "other") to 3000L,
            ("other-patient" to "visit") to 4000L), alarms.scheduled)
        scheduler.cancel(appointment)
        scheduler.cancel(appointment)
        assertEquals(2, alarms.scheduled.size)
    }

    @Test fun deniedPermissionRemovesOldAlarmWithoutSchedulingAnother() {
        scheduler.schedule(appointment, 2000L)
        alarms.allowed = false
        assertEquals(AppointmentReminderResult.PERMISSION_DENIED, scheduler.schedule(appointment, 3000L))
        assertTrue(alarms.scheduled.isEmpty())
    }

    @Test fun pastAndNonScheduledAppointmentsClearStaleReminders() {
        for (invalid in listOf(appointment.copy(status = AppointmentStatus.CANCELLED),
            appointment.copy(status = AppointmentStatus.COMPLETED))) {
            scheduler.schedule(appointment, 2000L)
            assertEquals(AppointmentReminderResult.NOT_ELIGIBLE, scheduler.schedule(invalid, 2000L))
            assertTrue(alarms.scheduled.isEmpty())
        }
        for (time in listOf(999L, 1000L)) {
            scheduler.schedule(appointment, 2000L)
            assertEquals(AppointmentReminderResult.NOT_ELIGIBLE, scheduler.schedule(appointment, time))
            assertTrue(alarms.scheduled.isEmpty())
        }
    }

    @Test fun missingIdentityCannotSchedule() {
        assertEquals(AppointmentReminderResult.NOT_ELIGIBLE, scheduler.schedule(appointment.copy(id = ""), 2000L))
        assertEquals(AppointmentReminderResult.NOT_ELIGIBLE, scheduler.schedule(appointment.copy(patientId = ""), 2000L))
        assertTrue(alarms.scheduled.isEmpty())
    }
}
