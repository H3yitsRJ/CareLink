package com.example.carelink

import com.example.carelink.screens.appointmentDateError
import com.example.carelink.screens.ClockFormat
import com.example.carelink.screens.displayFieldDate
import com.example.carelink.screens.displayFieldTime
import com.example.carelink.screens.storedFieldDate
import com.example.carelink.screens.storedFieldTime
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

    @Test fun dateFieldUsesMonthDayYearAndPreservesStoredDate() {
        assertEquals("02-29-2028", displayFieldDate("2028-02-29"))
        assertEquals("2028-02-29", storedFieldDate("02-29-2028"))
        listOf("", "2028-02-29", "02-29-2026", "04-31-2028").forEach {
            assertNull(storedFieldDate(it))
        }
    }

    @Test fun timeFieldUsesAmPmAndPreservesStoredTime() {
        mapOf("00:00" to "12:00 AM", "12:00" to "12:00 PM", "23:59" to "11:59 PM").forEach { (stored, display) ->
            assertEquals(display, displayFieldTime(stored))
            assertEquals(stored, storedFieldTime(display))
        }
        assertEquals("09:30", storedFieldTime(" 9:30 am "))
        listOf("", "24:00", "12:60 AM", "9:30", "13:30 PM").forEach {
            assertNull(storedFieldTime(it))
        }
    }

    @Test fun twentyFourHourClockPreservesStoredTimeAndValidatesInput() {
        val clock = ClockFormat.TwentyFourHour
        assertEquals("00:00", clock.displayTime("00:00"))
        assertEquals("13:30", clock.storedTime(" 13:30 "))
        assertNull(clock.storedTime("1:30 PM"))
        assertNull(clock.storedTime("24:00"))
    }
}
