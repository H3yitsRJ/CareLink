package com.example.carelink.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.carelink.R

/** Delivers a private, generic reminder. No provider, notes, or appointment title enters the notification. */
class AppointmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val identity = intent.data ?: return
        if (identity.scheme != "carelink" || identity.authority != "appointment-reminder" ||
            identity.pathSegments.size != 2) return
        // Permission can change between scheduling and delivery.
        if (!AndroidAppointmentReminderAlarms(context).notificationsAllowed()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Appointment reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Appointment reminder")
            .setContentText("You have an upcoming appointment.")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()
        // A permission revocation during delivery must not crash the app.
        try {
            NotificationManagerCompat.from(context).notify(identity.toString(), 0, notification)
        } catch (_: SecurityException) {
            // The patient disabled notification access; the permission-owning flow handles recovery.
        }
    }

    companion object {
        const val CHANNEL_ID = "appointment_reminders"
    }
}
