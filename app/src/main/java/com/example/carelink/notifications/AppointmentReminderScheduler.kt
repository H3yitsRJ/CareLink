package com.example.carelink.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus
import java.text.SimpleDateFormat
import java.util.Locale

class AppointmentReminderScheduler(private val context: Context) {
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(appointment: Appointment) {
        cancel(appointment)

        if (appointment.status != AppointmentStatus.SCHEDULED) return

        val appointmentTime = runCatching {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                isLenient = false
            }.parse("${appointment.date} ${appointment.time}")?.time
        }.getOrNull() ?: return

        // Show one reminder an hour before the appointment.
        val reminderTime = appointmentTime - 60 * 60 * 1000L
        if (reminderTime <= System.currentTimeMillis()) return

        val pendingIntent = pendingIntent(
            appointment,
            PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )
    }

    fun cancel(appointment: Appointment) {
        val pendingIntent = pendingIntent(
            appointment,
            PendingIntent.FLAG_NO_CREATE
        ) ?: return

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun pendingIntent(
        appointment: Appointment,
        lookupFlag: Int
    ): PendingIntent? {
        if (appointment.patientId.isBlank() || appointment.id.isBlank()) return null

        val intent = Intent(
            context,
            AppointmentReminderReceiver::class.java
        ).apply {
            data = Uri.Builder()
                .scheme("carelink")
                .authority("appointment-reminder")
                .appendPath(appointment.patientId)
                .appendPath(appointment.id)
                .build()
            putExtra(AppointmentReminderReceiver.EXTRA_PATIENT_ID, appointment.patientId)
            putExtra(AppointmentReminderReceiver.EXTRA_APPOINTMENT_ID, appointment.id)
        }

        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            lookupFlag or PendingIntent.FLAG_IMMUTABLE
        )
    }
}