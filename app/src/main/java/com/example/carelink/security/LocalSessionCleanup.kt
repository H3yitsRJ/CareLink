package com.example.carelink.security

import com.example.carelink.notifications.AndroidMedicationReminderScheduler

internal fun clearLocalSession(
    reminders: AndroidMedicationReminderScheduler,
    storage: SecureStorageManager
): Boolean {
    // Reminder metadata must remain readable until alarms and visible notifications are cancelled.
    reminders.clear()
    return storage.clearAll()
}
