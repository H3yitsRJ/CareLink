package com.example.carelink.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.carelink.MainActivity
import com.example.carelink.R
import com.google.firebase.auth.FirebaseAuth

class CareTaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val patientId = intent.getStringExtra(
            CareTaskReminderScheduler.EXTRA_PATIENT_ID
        ) ?: return
        val taskId = intent.getStringExtra(
            CareTaskReminderScheduler.EXTRA_TASK_ID
        ) ?: return

        // Only display reminders belonging to the signed-in patient.
        if (FirebaseAuth.getInstance().currentUser?.uid != patientId) return

        val scheduler = CareTaskReminderScheduler(context)
        val record = scheduler.stored(patientId, taskId) ?: return

        if (record.optString("revision") != intent.getStringExtra(
                CareTaskReminderScheduler.EXTRA_REVISION
            )
        ) {
            return
        }

        val deadline = record.optLong("deadline")
        if (deadline <= 0 || deadline > System.currentTimeMillis()) return

        if (!NotificationPermissionManager.canPostNotifications(context)) {
            scheduler.consume(patientId, taskId)
            return
        }

        val manager =
            context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Care task reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.Builder()
                .scheme("carelink")
                .authority("care-task-notification")
                .appendPath(patientId)
                .appendPath(taskId)
                .build()
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dueText =
            "Due ${record.getString("dueDate")} at ${record.getString("time")}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(record.getString("title"))
            .setContentText(dueText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(dueText))
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(
                CareTaskReminderScheduler.key(patientId, taskId),
                0,
                notification
            )
        } catch (_: SecurityException) {
            // Permission may be revoked after the permission check.
        } finally {
            scheduler.consume(patientId, taskId)
        }
    }

    companion object {
        const val CHANNEL_ID = "care_task_reminders"
    }
}