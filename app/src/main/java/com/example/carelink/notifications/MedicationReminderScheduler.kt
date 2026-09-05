package com.example.carelink.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.carelink.model.Medication
import java.util.Calendar

// The interface lets tests or previews replace Android's AlarmManager.
interface MedicationReminderScheduler {
    fun schedule(medication: Medication): Boolean
    fun cancel(medication: Medication)
    fun replace(previous: Medication, updated: Medication): Boolean {
        cancel(previous)
        return schedule(updated)
    }
}

class AndroidMedicationReminderScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)
) : MedicationReminderScheduler {
    override fun schedule(medication: Medication): Boolean {
        // Android 13 and later must not receive notifications until the patient grants permission.
        if (!notificationsAllowed()) return false
        medication.reminderTimes.forEach { time ->
            val triggerAt = nextOccurrence(time) ?: return@forEach
            // An inexact alarm avoids requiring the special exact-alarm system permission.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent(medication, time)
            )
        }
        return true
    }

    override fun cancel(medication: Medication) {
        medication.reminderTimes.forEach { time -> alarmManager.cancel(pendingIntent(medication, time)) }
    }

    private fun notificationsAllowed(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(medication: Medication, time: String): PendingIntent {
        val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_NAME, medication.name)
            putExtra(MedicationReminderReceiver.EXTRA_DOSE_TIME, time)
            putExtra(MedicationReminderReceiver.EXTRA_REMINDER_ID, reminderId(medication.id, time))
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId(medication.id, time),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        // Combining the medication and time prevents one scheduled dose from replacing another.
        internal fun reminderId(medicationId: String, time: String): Int = "$medicationId|$time".hashCode()

        internal fun nextOccurrence(time: String, nowMillis: Long = System.currentTimeMillis()): Long? {
            val parts = time.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null
            // If today's time has passed, schedule the next occurrence tomorrow.
            return Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= nowMillis) add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
        }
    }
}
