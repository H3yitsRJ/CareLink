package com.example.carelink.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppointmentEditorTest {
    @Test
    fun `accepts a complete appointment`() {
        assertNull(validateAppointment("Annual visit", "2026-10-01", "09:30"))
    }

    @Test
    fun `rejects missing title and malformed time`() {
        assertEquals("Enter an appointment title", validateAppointment("", "2026-10-01", "09:30"))
        assertEquals("Use a 24-hour time such as 09:30", validateAppointment("Annual visit", "2026-10-01", "9:30"))
    }
}
