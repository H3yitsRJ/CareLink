package com.example.carelink.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Editor validation is separated from Compose so these checks stay fast.
class MedicationEditorTest {
    @Test fun `accepts complete medication values`() {
        assertFalse(validateMedicationEditor("Metformin", "500 mg", "1 tablet", "Daily", "08:30").hasErrors)
    }

    @Test fun `rejects missing required values and invalid time`() {
        val errors = validateMedicationEditor("", "", "", "", "8:30")
        assertTrue(errors.hasErrors)
        assertTrue(errors.name != null)
        assertTrue(errors.reminderTime != null)
    }
}
