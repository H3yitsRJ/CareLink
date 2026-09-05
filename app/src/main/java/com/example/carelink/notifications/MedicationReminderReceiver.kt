package com.example.carelink.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.carelink.R

class MedicationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Permission may have been revoked after the alarm was originally scheduled.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val name = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: return
        val doseTime = intent.getStringExtra(EXTRA_DOSE_TIME) ?: return
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, 0)
        val manager = context.getSystemService(NotificationManager::class.java)
        // Creating the same channel again is safe, which keeps setup close to notification delivery.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Medication reminders", NotificationManager.IMPORTANCE_HIGH))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Medication reminder")
            .setContentText("$name is scheduled for $doseTime")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(reminderId, notification)
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val EXTRA_MEDICATION_NAME = "medicationName"
        const val EXTRA_DOSE_TIME = "doseTime"
        const val EXTRA_REMINDER_ID = "reminderId"
    }
}
