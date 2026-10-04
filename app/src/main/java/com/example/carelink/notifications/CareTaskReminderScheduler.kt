package com.example.carelink.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.carelink.model.CareTask
import org.json.JSONObject
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class CareTaskReminderScheduler(context: Context) {
    private val context = context.applicationContext
    private val alarms =
        this.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferences =
        this.context.getSharedPreferences("care_task_reminders", Context.MODE_PRIVATE)

    fun schedule(task: CareTask): Boolean {
        if (task.patientId.isBlank() || task.id.isBlank()) return false

        // Replace any previous reminder, including when the deadline is removed.
        cancel(task.patientId, task.id)

        if (task.completed) return false
        val deadline = deadlineMillis(task) ?: return false
        if (deadline <= System.currentTimeMillis()) return false
        if (!NotificationPermissionManager.canPostNotifications(context)) {
            return false
        }

        val revision = UUID.randomUUID().toString()
        val record = JSONObject().apply {
            put("title", task.title)
            put("dueDate", task.dueDate)
            put("time", task.time)
            put("deadline", deadline)
            put("revision", revision)
        }

        if (!preferences.edit()
                .putString(key(task.patientId, task.id), record.toString())
                .commit()
        ) {
            return false
        }

        return try {
            val intent = reminderIntent(task.patientId, task.id).apply {
                putExtra(EXTRA_REVISION, revision)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarms.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                deadline,
                pending
            )
            true
        } catch (_: RuntimeException) {
            cancel(task.patientId, task.id)
            false
        }
    }

    fun cancel(patientId: String, taskId: String) {
        if (patientId.isBlank() || taskId.isBlank()) return

        // Remove the record first so an already-delivered alarm is rejected.
        preferences.edit().remove(key(patientId, taskId)).commit()

        val pending = PendingIntent.getBroadcast(
            context,
            0,
            reminderIntent(patientId, taskId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pending?.let {
            alarms.cancel(it)
            it.cancel()
        }

        val manager = context.getSystemService(
            android.app.NotificationManager::class.java
        )
        manager.cancel(key(patientId, taskId), 0)
    }

    internal fun stored(patientId: String, taskId: String): JSONObject? =
        runCatching {
            val value = preferences.getString(key(patientId, taskId), null)
                ?: return null
            JSONObject(value)
        }.getOrNull()

    internal fun consume(patientId: String, taskId: String) {
        preferences.edit().remove(key(patientId, taskId)).commit()
    }

    private fun reminderIntent(patientId: String, taskId: String) =
        Intent(context, CareTaskReminderReceiver::class.java).apply {
            data = Uri.Builder()
                .scheme("carelink")
                .authority("care-task-reminder")
                .appendPath(patientId)
                .appendPath(taskId)
                .build()
            putExtra(EXTRA_PATIENT_ID, patientId)
            putExtra(EXTRA_TASK_ID, taskId)
        }

    companion object {
        const val EXTRA_PATIENT_ID = "patientId"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_REVISION = "revision"

        internal fun key(patientId: String, taskId: String) =
            "$patientId/$taskId"

        internal fun deadlineMillis(task: CareTask): Long? {
            if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(task.dueDate) ||
                !Regex("\\d{2}:\\d{2}").matches(task.time)
            ) {
                return null
            }

            val value = "${task.dueDate} ${task.time}"
            val formatter =
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                    isLenient = false
                }
            val position = ParsePosition(0)
            val parsed = formatter.parse(value, position) ?: return null

            return parsed.time.takeIf { position.index == value.length }
        }
    }
}