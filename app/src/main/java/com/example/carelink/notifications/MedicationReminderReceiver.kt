package com.example.carelink.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.carelink.R
import com.example.carelink.model.DoseRecord
import com.example.carelink.model.DoseStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.atomic.AtomicBoolean

class MedicationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val patientId = FirebaseAuth.getInstance().currentUser?.uid
        val scheduler = AndroidMedicationReminderScheduler(context)
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) {
            scheduler.restore(patientId)
            return
        }
        val record = handle(context, intent, patientId) ?: return
        // Keep the receiver alive for the write, but never exceed the broadcast time limit.
        val pending = goAsync()
        val handler = Handler(Looper.getMainLooper())
        val finished = AtomicBoolean(false)
        val finish = Runnable { if (finished.compareAndSet(false, true)) pending.finish() }
        handler.postDelayed(finish, 8_000)
        FirebaseFirestore.getInstance().collection("users").document(patientId!!)
            .collection("doseRecords").document(record.id).set(record.toFirestore())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) NotificationManagerCompat.from(context).cancel(
                    AndroidMedicationReminderScheduler.key(patientId, record.medicationId),
                    record.scheduledTimeMillis.hashCode())
                handler.removeCallbacks(finish)
                finish.run()
            }
    }

    /** Uses the same path for platform delivery and deterministic emulator tests. */
    internal fun handle(context: Context, intent: Intent, signedInPatientId: String?): DoseRecord? {
        val patientId = intent.getStringExtra(EXTRA_PATIENT_ID) ?: return null
        if (patientId != signedInPatientId) return null
        val medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID) ?: return null
        val time = intent.getStringExtra(EXTRA_DOSE_TIME) ?: return null
        val at = intent.getLongExtra(EXTRA_SCHEDULED_TIME, 0)
        if (at <= 0) return null
        val scheduler = AndroidMedicationReminderScheduler(context)
        val (medication, revision) = scheduler.stored(patientId, medicationId) ?: return null
        if (!medication.active || time !in medication.reminderTimes ||
            revision != intent.getStringExtra(EXTRA_REVISION)) return null
        val status = when (intent.action) {
            ACTION_TAKEN -> DoseStatus.TAKEN
            ACTION_SKIPPED -> DoseStatus.SKIPPED
            ACTION_MISSED -> DoseStatus.MISSED
            ACTION_DELAYED -> DoseStatus.DELAYED
            null -> null
            else -> return null
        }
        if (status != null) return DoseRecord.completed(medicationId, at, status)
        if (!AndroidMedicationReminderScheduler.notificationsAllowed(context)) return null
        val next = MedicationSchedule.next(time, medication.frequency, maxOf(at, System.currentTimeMillis())) ?: return null
        scheduler.scheduleOccurrence(medication, time, next, revision)
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Medication reminders", NotificationManager.IMPORTANCE_HIGH))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Medication reminder")
            .setContentText("${medication.name} is scheduled for $time")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("Medication reminder").build())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        listOf("Taken" to ACTION_TAKEN, "Missed" to ACTION_MISSED, "Delayed" to ACTION_DELAYED).forEach { (label, action) ->
            val actionIntent = Intent(intent).apply {
                this.action = action
                data = intent.data?.buildUpon()?.appendPath(at.toString())?.build()
            }
            notification.addAction(0, label, PendingIntent.getBroadcast(context, 0, actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        try {
            NotificationManagerCompat.from(context).notify(AndroidMedicationReminderScheduler.key(medication), at.hashCode(), notification.build())
        } catch (_: SecurityException) {
            // Permission can be revoked between the check and posting the notification.
        }
        return null
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val EXTRA_MEDICATION_NAME = "medicationName"
        const val EXTRA_DOSE_TIME = "doseTime"
        const val EXTRA_REMINDER_ID = "reminderId"
        const val EXTRA_MEDICATION_ID = "medicationId"
        const val EXTRA_PATIENT_ID = "patientId"
        const val EXTRA_SCHEDULED_TIME = "scheduledTimeMillis"
        const val EXTRA_REVISION = "scheduleRevision"
        const val ACTION_TAKEN = "com.example.carelink.ACTION_TAKEN"
        const val ACTION_SKIPPED = "com.example.carelink.ACTION_SKIPPED"
        const val ACTION_MISSED = "com.example.carelink.ACTION_MISSED"
        const val ACTION_DELAYED = "com.example.carelink.ACTION_DELAYED"
    }
}
