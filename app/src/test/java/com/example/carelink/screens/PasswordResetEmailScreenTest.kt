package com.example.carelink.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Reset-email validation is tested without making a Firebase request.
class PasswordResetEmailScreenTest {
    @Test
    fun validEmailHasNoError() {
        assertNull(validateEmailAddress(" patient@example.com "))
    }

    @Test
    fun emptyEmailIsRejected() {
        assertEquals("Enter your email address", validateEmailAddress(""))
    }

    @Test
    fun malformedEmailIsRejected() {
        assertEquals("Enter a valid email address", validateEmailAddress("patient@example"))
    }
}
