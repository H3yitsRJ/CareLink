package com.example.carelink.screens

import com.example.carelink.model.Medication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationTest {

    @Test
    fun medicationConvertsToFirestoreMap() {
        val medication = Medication(
            id = "med123",
            patientId = "patient123",
            name = "Metformin",
            strength = "500 mg",
            dose = "1 tablet",
            frequency = "Twice daily",
            reminderTimes = listOf("08:00", "18:00"),
            instructions = "Take with food",
            active = true
        )

        val data = medication.toFirestore()

        assertEquals("patient123", data["patientId"])
        assertEquals("Metformin", data["name"])
        assertEquals("500 mg", data["strength"])
        assertEquals("1 tablet", data["dose"])
        assertEquals("Twice daily", data["frequency"])
        assertEquals(listOf("08:00", "18:00"), data["reminderTimes"])
        assertEquals("Take with food", data["instructions"])
        assertEquals(true, data["active"])
    }

    @Test
    fun firestoreMapConvertsBackToMedication() {
        val data = mapOf<String, Any?>(
            "patientId" to "patient123",
            "name" to "Metformin",
            "strength" to "500 mg",
            "dose" to "1 tablet",
            "frequency" to "Twice daily",
            "reminderTimes" to listOf("08:00", "18:00"),
            "instructions" to "Take with food",
            "active" to true
        )

        val medication = Medication.fromFirestore("med123", data)

        assertNotNull(medication)
        assertEquals("med123", medication?.id)
        assertEquals("patient123", medication?.patientId)
        assertEquals("Metformin", medication?.name)
        assertEquals("500 mg", medication?.strength)
        assertEquals("1 tablet", medication?.dose)
        assertEquals("Twice daily", medication?.frequency)
        assertEquals(listOf("08:00", "18:00"), medication?.reminderTimes)
        assertEquals("Take with food", medication?.instructions)
        assertEquals(true, medication?.active)
    }

    @Test
    fun missingOptionalFieldsUseSafeDefaults() {
        val data = mapOf<String, Any?>(
            "patientId" to "patient123",
            "name" to "Metformin"
        )

        val medication = Medication.fromFirestore("med123", data)

        assertNotNull(medication)
        assertEquals("", medication?.strength)
        assertEquals("", medication?.dose)
        assertEquals("", medication?.frequency)
        assertTrue(medication?.reminderTimes?.isEmpty() == true)
        assertEquals("", medication?.instructions)
        assertEquals(true, medication?.active)
    }

    @Test
    fun invalidOptionalFieldsDoNotCrash() {
        val data = mapOf<String, Any?>(
            "patientId" to "patient123",
            "name" to "Metformin",
            "strength" to 500,
            "dose" to true,
            "frequency" to 123,
            "reminderTimes" to listOf("08:00", 123, true),
            "instructions" to false,
            "active" to "yes"
        )

        val medication = Medication.fromFirestore("med123", data)

        assertNotNull(medication)
        assertEquals("", medication?.strength)
        assertEquals("", medication?.dose)
        assertEquals("", medication?.frequency)
        assertEquals(listOf("08:00"), medication?.reminderTimes)
        assertEquals("", medication?.instructions)
        assertEquals(true, medication?.active)
    }

    @Test
    fun missingRequiredFieldReturnsNull() {
        val data = mapOf<String, Any?>(
            "name" to "Metformin"
        )

        val medication = Medication.fromFirestore("med123", data)

        assertNull(medication)
    }

    @Test
    fun validMedicationHasNoValidationErrors() {
        val medication = Medication(
            id = "med123",
            patientId = "patient123",
            name = "Metformin",
            strength = "500 mg",
            dose = "1 tablet",
            frequency = "Twice daily",
            reminderTimes = listOf("08:00", "18:00")
        )

        val errors = medication.validate()

        assertTrue(errors.isEmpty())
    }

    @Test
    fun invalidMedicationReturnsValidationErrors() {
        val medication = Medication(
            id = "med123",
            patientId = "patient123",
            name = "",
            strength = "",
            dose = "",
            frequency = "",
            reminderTimes = emptyList()
        )

        val errors = medication.validate()

        assertEquals("Enter a medication name", errors["name"])
        assertEquals("Enter the medication strength", errors["strength"])
        assertEquals("Enter the dose", errors["dose"])
        assertEquals("Choose a frequency", errors["frequency"])
        assertEquals("Add at least one reminder time", errors["reminderTimes"])
    }

    @Test
    fun invalidReminderTimeReturnsValidationError() {
        val medication = Medication(
            id = "med123",
            patientId = "patient123",
            name = "Metformin",
            strength = "500 mg",
            dose = "1 tablet",
            frequency = "Daily",
            reminderTimes = listOf("25:99")
        )

        val errors = medication.validate()

        assertEquals(
            "Use a 24-hour time such as 08:30",
            errors["reminderTimes"]
        )
    }
}