package com.example.carelink.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.example.carelink.model.DoseRecord
import com.example.carelink.model.DoseStatus
import com.example.carelink.model.Medication
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class MedicationReminderTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val scheduler = AndroidMedicationReminderScheduler(context)
    private val medication = Medication("notification-test", "test-patient", "Test medication", "5 mg", "1 tablet", "Daily", listOf("08:00", "20:00"))

    @Before fun setup() {
        if (Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        scheduler.clear()
    }
    @After fun cleanup() { scheduler.clear() }

    @Test fun deliveryCreatesActionsAndRecordingUsesModelAndOccurrenceId() {
        assertTrue(scheduler.schedule(medication))
        val revision = scheduler.stored(medication.patientId, medication.id)!!.second
        val at = System.currentTimeMillis() - 1_000
        val intent = scheduler.alarmIntent(medication, "08:00", at, revision)
        val receiver = MedicationReminderReceiver()
        receiver.handle(context, intent, medication.patientId)
        val manager = context.getSystemService(NotificationManager::class.java)
        val deadline = SystemClock.elapsedRealtime() + 3_000
        while (manager.activeNotifications.none { it.id == at.hashCode() } && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(25)
        }
        val notification = manager.activeNotifications.single { it.id == at.hashCode() }
        assertTrue(notification.notification.extras.getCharSequence("android.text").toString().contains("Test medication"))
        assertEquals(listOf("Taken", "Missed", "Delayed"), notification.notification.actions.map { it.title.toString() })
        listOf(MedicationReminderReceiver.ACTION_TAKEN to DoseStatus.TAKEN,
            MedicationReminderReceiver.ACTION_MISSED to DoseStatus.MISSED,
            MedicationReminderReceiver.ACTION_DELAYED to DoseStatus.DELAYED).forEach { (action, status) ->
            val record = receiver.handle(context, Intent(intent).setAction(action), medication.patientId)!!
            assertEquals(status, record.status)
            assertEquals(at, record.scheduledTimeMillis)
            assertEquals(record, DoseRecord.fromFirestore(record.id, record.toFirestore()))
        }
        val tomorrow = receiver.handle(context, Intent(intent).setAction(MedicationReminderReceiver.ACTION_TAKEN)
            .putExtra(MedicationReminderReceiver.EXTRA_SCHEDULED_TIME, at + 86_400_000), medication.patientId)!!
        assertNotEquals(DoseRecord.completed(medication.id, at, DoseStatus.TAKEN).id, tomorrow.id)
    }

    @Test fun editCancelsOldAlarmsAndRemovalRejectsStaleActions() {
        scheduler.schedule(medication)
        val revision = scheduler.stored(medication.patientId, medication.id)!!.second
        val old = scheduler.alarmIntent(medication, "08:00", System.currentTimeMillis(), revision)
        val updated = medication.copy(reminderTimes = listOf("09:00"))
        assertTrue(scheduler.replace(medication, updated))
        assertNull(PendingIntent.getBroadcast(context, 0, old, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
        assertNull(MedicationReminderReceiver().handle(context, old.setAction(MedicationReminderReceiver.ACTION_TAKEN), medication.patientId))
        scheduler.cancel(updated)
        assertNull(scheduler.stored(medication.patientId, medication.id))
    }

    @Test fun accountsAreIsolated() {
        scheduler.schedule(medication)
        val revision = scheduler.stored(medication.patientId, medication.id)!!.second
        val intent = scheduler.alarmIntent(medication, "08:00", System.currentTimeMillis(), revision)
        assertNull(MedicationReminderReceiver().handle(context, intent.setAction(MedicationReminderReceiver.ACTION_TAKEN), "different-patient"))
        val other = medication.copy(patientId = "different-patient")
        assertNotEquals(intent.data, scheduler.alarmIntent(other, "08:00", 1, "test").data)
        scheduler.restore(null)
        assertNull(scheduler.stored(medication.patientId, medication.id))
    }

    @Test fun deniedPermissionDefersSchedulingUntilPermissionReturns() {
        val denied = AndroidMedicationReminderScheduler(context, permissionGranted = { false })
        assertFalse(denied.schedule(medication))
        val revision = denied.stored(medication.patientId, medication.id)!!.second
        val intent = denied.alarmIntent(medication, "08:00", 1, revision)
        assertNull(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
        scheduler.restore(medication.patientId)
        assertNotNull(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
    }
}
