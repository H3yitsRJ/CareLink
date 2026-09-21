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
import com.example.carelink.model.Medication
import org.json.JSONObject
import java.util.UUID

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
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java),
    private val permissionGranted: () -> Boolean = { notificationsAllowed(context) }
) : MedicationReminderScheduler {
    private val preferences = context.getSharedPreferences("medication_reminders", Context.MODE_PRIVATE)

    override fun schedule(medication: Medication): Boolean {
        if (medication.patientId.isBlank() || medication.id.isBlank() || medication.validate().isNotEmpty() ||
            MedicationSchedule.days(medication.frequency) == null) return false
        if (MedicationSchedule.requiredTimes(medication.frequency)?.let { it != medication.reminderTimes.distinct().size } == true) return false
        stored(medication.patientId, medication.id)?.let { (previous, revision) ->
            if (previous == medication && medication.active) return scheduleTimes(medication, revision)
        }
        cancel(medication)
        if (!medication.active) return true
        val revision = UUID.randomUUID().toString()
        val stored = preferences.edit().putString(key(medication), JSONObject(medication.toFirestore()).apply {
            put("id", medication.id); put("revision", revision)
        }.toString()).commit()
        if (!stored) return false
        return scheduleTimes(medication, revision)
    }

    private fun scheduleTimes(medication: Medication, revision: String): Boolean {
        if (!permissionGranted()) return false
        return runCatching {
            medication.reminderTimes.forEach { time ->
                val next = MedicationSchedule.next(time, medication.frequency, System.currentTimeMillis()) ?: return false
                scheduleOccurrence(medication, time, next, revision)
            }
            true
        }.getOrDefault(false)
    }

    internal fun scheduleOccurrence(medication: Medication, time: String, at: Long, revision: String) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at,
            PendingIntent.getBroadcast(context, 0, alarmIntent(medication, time, at, revision),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
    }

    internal fun alarmIntent(medication: Medication, time: String, at: Long, revision: String) =
        Intent(context, MedicationReminderReceiver::class.java).apply {
            data = Uri.Builder().scheme("carelink").authority("medication")
                .appendPath(medication.patientId).appendPath(medication.id).appendPath(time).build()
            putExtra(MedicationReminderReceiver.EXTRA_PATIENT_ID, medication.patientId)
            putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_ID, medication.id)
            putExtra(MedicationReminderReceiver.EXTRA_DOSE_TIME, time)
            putExtra(MedicationReminderReceiver.EXTRA_SCHEDULED_TIME, at)
            putExtra(MedicationReminderReceiver.EXTRA_REVISION, revision)
        }

    override fun cancel(medication: Medication) {
        val stored = stored(medication.patientId, medication.id)?.first
        (stored?.reminderTimes.orEmpty() + medication.reminderTimes).distinct().forEach { time ->
            val pending = PendingIntent.getBroadcast(context, 0, alarmIntent(medication, time, 0, ""),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            pending?.let { alarmManager.cancel(it); it.cancel() }
            // Cancel alarms made by older versions as well.
            val legacy = PendingIntent.getBroadcast(context, reminderId(medication.id, time),
                Intent(context, MedicationReminderReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            legacy?.let { alarmManager.cancel(it); it.cancel() }
            NotificationManagerCompat.from(context).cancel(reminderId(medication.id, time))
        }
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        manager.activeNotifications.filter { it.tag == key(medication) }.forEach { manager.cancel(it.tag, it.id) }
        preferences.edit().remove(key(medication)).commit()
    }

    fun clear() {
        all().forEach { cancel(it.first) }
        preferences.edit().clear().commit()
    }

    fun restore(patientId: String?) {
        all().forEach { (medication, revision) ->
            if (patientId != medication.patientId) cancel(medication)
            else scheduleTimes(medication, revision)
        }
    }

    internal fun stored(patientId: String, medicationId: String): Pair<Medication, String>? =
        decode(preferences.getString(key(patientId, medicationId), null))

    private fun all() = preferences.all.values.mapNotNull { decode(it as? String) }
    private fun decode(json: String?): Pair<Medication, String>? = runCatching {
        val value = JSONObject(json ?: return null)
        val times = value.getJSONArray("reminderTimes")
        Medication(value.getString("id"), value.getString("patientId"), value.getString("name"),
            value.getString("strength"), value.getString("dose"), value.getString("frequency"),
            (0 until times.length()).map(times::getString), value.optString("instructions"), value.getBoolean("active"),
            value.optString("updatedById").takeIf { it.isNotEmpty() }) to value.getString("revision")
    }.getOrNull()

    companion object {
        internal fun key(medication: Medication) = key(medication.patientId, medication.id)
        internal fun key(patientId: String, medicationId: String) = "$patientId/$medicationId"
        internal fun reminderId(medicationId: String, time: String): Int = "$medicationId|$time".hashCode()
        internal fun nextOccurrence(time: String, nowMillis: Long = System.currentTimeMillis()): Long? =
            MedicationSchedule.next(time, "Daily", nowMillis)
        internal fun notificationsAllowed(context: Context): Boolean =
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
