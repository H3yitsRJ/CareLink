package com.example.carelink.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.carelink.R
import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.atomic.AtomicBoolean

class AppointmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val patientId = intent.getStringExtra(EXTRA_PATIENT_ID) ?: return
        val appointmentId = intent.getStringExtra(EXTRA_APPOINTMENT_ID) ?: return

        // Never display another patient's appointment on this device.
        if (FirebaseAuth.getInstance().currentUser?.uid != patientId) return

        val pending = goAsync()
        val handler = Handler(Looper.getMainLooper())
        val finished = AtomicBoolean(false)
        val finish = Runnable {
            if (finished.compareAndSet(false, true)) pending.finish()
        }
        handler.postDelayed(finish, 8_000)

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(patientId)
            .collection("appointments")
            .document(appointmentId)
            .get()
            .addOnCompleteListener { task ->
                try {
                    val document = if (task.isSuccessful) {
                        task.result.takeIf { it.exists() }
                    } else {
                        null
                    }
                    val appointment = document?.let {
                        Appointment.fromFirestore(it.id, it.data.orEmpty())
                    }

                    if (
                        appointment?.patientId == patientId &&
                        appointment.status == AppointmentStatus.SCHEDULED &&
                        NotificationPermissionManager.canPostNotifications(context)
                    ) {
                        showNotification(context, appointment)
                    }
                } finally {
                    handler.removeCallbacks(finish)
                    finish.run()
                }
            }
    }

    private fun showNotification(context: Context, appointment: Appointment) {
        val manager = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Appointment reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Upcoming appointment")
            .setContentText(
                "${appointment.title} is at ${appointment.time}"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                appointment.id.hashCode(),
                notification
            )
        } catch (_: SecurityException) {
            // Notification permission can change after the check.
        }
    }

    companion object {
        const val EXTRA_PATIENT_ID = "appointmentReminderPatientId"
        const val EXTRA_APPOINTMENT_ID = "appointmentReminderAppointmentId"
        private const val CHANNEL_ID = "appointment_reminders"
    }
}