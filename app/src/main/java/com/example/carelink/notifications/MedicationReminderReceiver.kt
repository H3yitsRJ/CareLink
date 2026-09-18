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
import android.app.PendingIntent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MedicationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d(
            "MedicationReminderReceiver",
            "Received action: ${intent.action}"
        )
        when (intent.action) {
            ACTION_TAKEN -> {
                recordDoseAction(context, intent, "taken")
                return
            }

            ACTION_SKIPPED -> {
                recordDoseAction(context, intent, "skipped")
                return
            }
        }
        // Permission may have been revoked after the alarm was originally scheduled.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val name = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: return
        val medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID) ?: return
        val doseTime = intent.getStringExtra(EXTRA_DOSE_TIME) ?: return
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, 0)
        val scheduledTimeMillis =
            intent.getLongExtra(EXTRA_SCHEDULED_TIME_MILLIS, 0L)

        val takenIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = ACTION_TAKEN
            putExtra(EXTRA_MEDICATION_NAME, name)
            putExtra(EXTRA_DOSE_TIME, doseTime)
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULED_TIME_MILLIS, scheduledTimeMillis)
        }

        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId * 10 + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val skippedIntent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = ACTION_SKIPPED
            putExtra(EXTRA_MEDICATION_NAME, name)
            putExtra(EXTRA_DOSE_TIME, doseTime)
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULED_TIME_MILLIS, scheduledTimeMillis)

        }

        val skippedPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId * 10 + 2,
            skippedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        // Creating the same channel again is safe, which keeps setup close to notification delivery.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Medication reminders", NotificationManager.IMPORTANCE_HIGH))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Medication reminder")
            .setContentText("$name is scheduled for $doseTime")
            .addAction(0, "Taken", takenPendingIntent)
            .addAction(0, "Skipped", skippedPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(reminderId, notification)
    }
    private fun recordDoseAction(
        context: Context,
        intent: Intent,
        status: String
    ) {
        android.util.Log.d(
            "MedicationReminderReceiver",
            "recordDoseAction started: status=$status"
        )
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val medicationId =
            intent.getStringExtra(EXTRA_MEDICATION_ID) ?: return
        val doseTime =
            intent.getStringExtra(EXTRA_DOSE_TIME) ?: return
        val reminderId =
            intent.getIntExtra(EXTRA_REMINDER_ID, 0)
        val scheduledTimeMillis =
            intent.getLongExtra(EXTRA_SCHEDULED_TIME_MILLIS, 0L)
        android.util.Log.d(
            "MedicationReminderReceiver",
            "medicationId=$medicationId, doseTime=$doseTime, scheduledTimeMillis=$scheduledTimeMillis"
        )

        if (scheduledTimeMillis == 0L) return

        val doseRecord = hashMapOf(
            "medicationId" to medicationId,
            "scheduledTimeMillis" to scheduledTimeMillis,
            "status" to status,
            "completionTimeMillis" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("doseRecords")
            .document(reminderId.toString())
            .set(doseRecord)
            .addOnSuccessListener {
                NotificationManagerCompat.from(context).cancel(reminderId)
            }
            .addOnFailureListener { exception ->
                android.util.Log.e(
                    "MedicationReminder",
                    "Failed to record dose action",
                    exception
                )
            }
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val EXTRA_MEDICATION_NAME = "medicationName"
        const val EXTRA_DOSE_TIME = "doseTime"
        const val EXTRA_REMINDER_ID = "reminderId"
        const val ACTION_TAKEN = "com.example.carelink.ACTION_TAKEN"
        const val ACTION_SKIPPED = "com.example.carelink.ACTION_SKIPPED"
        const val EXTRA_MEDICATION_ID = "medicationId"
        const val EXTRA_SCHEDULED_TIME_MILLIS = "scheduledTimeMillis"
    }
}
