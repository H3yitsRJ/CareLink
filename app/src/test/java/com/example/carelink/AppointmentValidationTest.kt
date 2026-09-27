package com.example.carelink

import com.example.carelink.screens.appointmentDateError
import com.example.carelink.screens.isValidTime
import org.junit.Assert.*
import org.junit.Test

class AppointmentValidationTest {
    @Test fun rejectsPastDatesButAcceptsTodayAndFutureDates() {
        assertEquals("Choose today or a future date", appointmentDateError("2026-09-22", "2026-09-23"))
        assertNull(appointmentDateError("2026-09-23", "2026-09-23"))
        assertNull(appointmentDateError(" 2027-01-01 ", "2026-12-31"))
        assertNotNull(appointmentDateError("2025-12-31", "2026-01-01"))
    }

    @Test fun rejectsMalformedAndImpossibleDates() {
        listOf("", "2026-2-01", "2026-02-29", "2026-04-31", "2026-13-01", "2026-01-00").forEach {
            assertEquals("Use a valid date in YYYY-MM-DD format", appointmentDateError(it, "2026-01-01"))
        }
        assertNull(appointmentDateError("2028-02-29", "2026-01-01"))
    }

    @Test fun requiresValid24HourTime() {
        listOf("", "24:00", "12:60", "9:30", "10:30 AM").forEach { assertFalse(isValidTime(it)) }
        listOf("00:00", "09:30", "23:59").forEach { assertTrue(isValidTime(it)) }
    }
}
