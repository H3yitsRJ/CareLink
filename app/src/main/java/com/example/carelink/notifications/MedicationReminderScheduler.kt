package com.example.carelink.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import com.example.carelink.model.Medication
import com.example.carelink.security.SecureStorageManager
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

    private val secureStorage = SecureStorageManager(context)
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
        val medicationJson = JSONObject(medication.toFirestore()).apply {
            put("id", medication.id)
            put("revision", revision)
        }.toString()


        val stored = secureStorage.save(
            key(medication),
            medicationJson
        )

        if (!stored) {
            android.util.Log.e(
                "MedicationReminderScheduler",
                "Secure storage failed while saving medication reminder"
            )
            return false
        }

// Remove any old plaintext copy after encryption succeeds.
        val legacyRemoved = preferences.edit()
            .remove(key(medication))
            .commit()

        if (!legacyRemoved) {
            android.util.Log.e(
                "MedicationReminderScheduler",
                "Failed to remove legacy medication reminder data"
            )
            return false
        }

        val reminderScheduled = scheduleTimes(medication, revision)

        android.util.Log.d(
            "MedicationReminderScheduler",
            "Reminder scheduling result: $reminderScheduled"
        )

        return reminderScheduled

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
        secureStorage.delete(key(medication))
        preferences.edit().remove(key(medication)).commit()
    }

    fun clear() {
        all().forEach { cancel(it.first) }
        preferences.edit().clear().commit()
    }


    fun restore(patientId: String?) {
        all().forEach { (medication, revision) ->
            if (patientId != medication.patientId) {
                cancel(medication)
            } else {
                val storageKey = key(medication)

                // Migrate older plaintext reminders into secure storage.
                val legacyValue = preferences.getString(storageKey, null)

                if (legacyValue != null) {
                    if (secureStorage.save(storageKey, legacyValue)) {
                        preferences.edit().remove(storageKey).commit()
                    }
                }

                scheduleTimes(medication, revision)
            }
        }
    }



    internal fun stored(
        patientId: String,
        medicationId: String
    ): Pair<Medication, String>? {
        val storageKey = key(patientId, medicationId)

        val encryptedValue = secureStorage.get(storageKey)

        if (encryptedValue != null) {
            return decode(encryptedValue)
        }

        // Read existing reminders until migration is complete.
        val legacyValue = preferences.getString(
            storageKey,
            null
        ) ?: return null


        val medication = decode(legacyValue) ?: return null

        if (!secureStorage.save(storageKey, legacyValue)) {
            return medication
        }

        preferences.edit().remove(storageKey).commit()

        return medication

    }



    private fun all(): List<Pair<Medication, String>> {
        val secureReminders = secureStorage.getAllKeys()
            .mapNotNull { storageKey ->
                decode(secureStorage.get(storageKey))
            }

        val legacyReminders = preferences.all.values
            .mapNotNull { decode(it as? String) }

        return (secureReminders + legacyReminders)
            .distinctBy { it.first.patientId to it.first.id }
    }

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
            NotificationPermissionManager.canPostNotifications(context)
    }
}
