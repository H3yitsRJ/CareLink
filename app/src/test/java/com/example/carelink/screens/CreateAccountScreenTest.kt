package com.example.carelink.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateAccountScreenTest {
    @Test
    fun validRegistrationHasNoErrors() {
        val errors = validateRegistration("jane@example.com", "carelink123", "carelink123")
        assertFalse(errors.hasErrors)
    }

    @Test
    fun emptyRegistrationShowsRequiredFieldErrors() {
        val errors = validateRegistration("", "", "")
        assertTrue(errors.hasErrors)
        assertEquals("Enter your email address", errors.email)
        assertEquals("Enter a password", errors.password)
        assertEquals("Enter your password again", errors.confirmPassword)
    }

    @Test
    fun invalidEmailShortPasswordAndMismatchAreRejected() {
        val errors = validateRegistration("not-an-email", "short", "different")
        assertEquals("Enter a valid email address", errors.email)
        assertEquals("Use at least 8 characters", errors.password)
        assertEquals("Passwords do not match", errors.confirmPassword)
    }
}
