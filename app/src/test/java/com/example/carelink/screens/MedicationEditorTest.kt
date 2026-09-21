package com.example.carelink.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationEditorTest {
    @Test fun frequencyMustMatchTheNumberOfReminderTimes() {
        assertTrue(validateMedicationEditor("Test", "5 mg", "1 tablet", "Twice daily", "08:00").hasErrors)
        assertFalse(validateMedicationEditor("Test", "5 mg", "1 tablet", "Twice daily", "08:00, 20:00").hasErrors)
        assertTrue(validateMedicationEditor("Test", "5 mg", "1 tablet", "unknown", "08:00").hasErrors)
    }
    @Test
    fun `accepts complete medication values`() {
        assertFalse(validateMedicationEditor("Metformin", "500 mg", "1 tablet", "Daily", "08:30").hasErrors)
    }

    @Test
    fun `rejects missing required values and invalid time`() {
        val errors = validateMedicationEditor("", "", "", "", "8:30")
        assertTrue(errors.hasErrors)
        assertTrue(errors.name != null)
        assertTrue(errors.reminderTime != null)
    }
}
