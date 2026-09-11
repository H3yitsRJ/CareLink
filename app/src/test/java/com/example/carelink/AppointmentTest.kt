package com.example.carelink

import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AppointmentTest {

    @Test
    fun appointmentConvertsToFirestore() {
        val appointment = Appointment(
            id = "appointment1",
            patientId = "patient1",
            title = "Annual Checkup",
            date = "2026-09-15",
            time = "10:30 AM",
            provider = "Dr. Smith",
            location = "Medical Center",
            notes = "Bring medication list",
            status = AppointmentStatus.SCHEDULED
        )

        val data = appointment.toFirestore()

        assertEquals("patient1", data["patientId"])
        assertEquals("Annual Checkup", data["title"])
        assertEquals("2026-09-15", data["date"])
        assertEquals("10:30 AM", data["time"])
        assertEquals("Dr. Smith", data["provider"])
        assertEquals("Medical Center", data["location"])
        assertEquals("Bring medication list", data["notes"])
        assertEquals("SCHEDULED", data["status"])
    }

    @Test
    fun appointmentConvertsFromFirestore() {
        val data = mapOf<String, Any?>(
            "patientId" to "patient1",
            "title" to "Annual Checkup",
            "date" to "2026-09-15",
            "time" to "10:30 AM",
            "provider" to "Dr. Smith",
            "location" to "Medical Center",
            "notes" to "Bring medication list",
            "status" to "SCHEDULED"
        )

        val appointment = Appointment.fromFirestore(
            id = "appointment1",
            data = data
        )

        assertEquals("appointment1", appointment.id)
        assertEquals("patient1", appointment.patientId)
        assertEquals("Annual Checkup", appointment.title)
        assertEquals("2026-09-15", appointment.date)
        assertEquals("10:30 AM", appointment.time)
        assertEquals(AppointmentStatus.SCHEDULED, appointment.status)
    }

    @Test
    fun invalidOptionalValuesDoNotCrash() {
        val data = mapOf<String, Any?>(
            "patientId" to "patient1",
            "title" to "Annual Checkup",
            "date" to "2026-09-15",
            "time" to "10:30 AM",
            "provider" to null,
            "location" to null,
            "notes" to null,
            "status" to "INVALID_STATUS"
        )

        val appointment = Appointment.fromFirestore(
            id = "appointment1",
            data = data
        )

        assertEquals("", appointment.provider)
        assertEquals("", appointment.location)
        assertEquals("", appointment.notes)
        assertEquals(AppointmentStatus.SCHEDULED, appointment.status)
    }
}