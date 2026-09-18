package com.example.carelink.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Android adapter. Inexact alarms follow the existing medication scheduler and need no exact-alarm grant. */
class AndroidAppointmentReminderAlarms(private val context: Context) : AppointmentReminderAlarms {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun notificationsAllowed(): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun schedule(patientId: String, appointmentId: String, triggerAtMillis: Long) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerAtMillis,
            pendingIntent(patientId, appointmentId, PendingIntent.FLAG_UPDATE_CURRENT)!!
        )
    }

    override fun cancel(patientId: String, appointmentId: String) {
        // FLAG_NO_CREATE makes cancellation safe when there has never been an alarm for this record.
        pendingIntent(patientId, appointmentId, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
        NotificationManagerCompat.from(context).cancel(identity(patientId, appointmentId).toString(), 0)
    }

    private fun pendingIntent(patientId: String, appointmentId: String, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(context, 0,
            Intent(context, AppointmentReminderReceiver::class.java).apply {
                data = identity(patientId, appointmentId)
            }, flags or PendingIntent.FLAG_IMMUTABLE)

    // Intent data, rather than a hash request code, distinguishes appointments without hash collisions.
    private fun identity(patientId: String, appointmentId: String): Uri = Uri.Builder()
        .scheme("carelink").authority("appointment-reminder")
        .appendPath(patientId).appendPath(appointmentId).build()
}
