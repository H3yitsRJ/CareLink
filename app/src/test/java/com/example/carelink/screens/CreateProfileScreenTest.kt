package com.example.carelink.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateProfileScreenTest {
    @Test
    fun validProfileHasNoErrors() {
        assertFalse(validateProfile("Jane", "Smith").hasErrors)
    }

    @Test
    fun emptyProfileShowsRequiredFieldErrors() {
        val errors = validateProfile("", "")
        assertTrue(errors.hasErrors)
        assertEquals("Enter your first name", errors.firstName)
        assertEquals("Enter your last name", errors.lastName)
    }

    @Test
    fun blankOptionalFieldsAreAccepted() {
        assertFalse(
            validateProfile(
                firstName = "Jane",
                lastName = "Smith",
                dateOfBirth = "",
                phoneNumber = "",
                state = "",
                zipCode = ""
            ).hasErrors
        )
    }

    @Test
    fun enteredOptionalFieldsMustUseSupportedFormats() {
        val errors = validateProfile(
            firstName = "Jane",
            lastName = "Smith",
            dateOfBirth = "13/40/20",
            phoneNumber = "555-123",
            state = "Ohio",
            zipCode = "123"
        )

        assertEquals("Use MM/DD/YYYY", errors.dateOfBirth)
        assertEquals("Enter a 10-digit phone number", errors.phoneNumber)
        assertEquals("Use the 2-letter state code", errors.state)
        assertEquals("Enter a 5-digit ZIP code", errors.zipCode)
    }
}
