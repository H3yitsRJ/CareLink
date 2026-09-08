package com.example.carelink

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun registration_withValidFields_returnsTrue() {
        val result = AuthValidator.isRegistrationValid(
            name = "Timothy Jackson",
            email = "timothy@example.com",
            password = "password123"
        )

        assertTrue(result)
    }

    @Test
    fun registration_withEmptyName_returnsFalse() {
        val result = AuthValidator.isRegistrationValid(
            name = "",
            email = "timothy@example.com",
            password = "password123"
        )

        assertFalse(result)
    }

    @Test
    fun registration_withEmptyEmail_returnsFalse() {
        val result = AuthValidator.isRegistrationValid(
            name = "Timothy Jackson",
            email = "",
            password = "password123"
        )

        assertFalse(result)
    }

    @Test
    fun registration_withEmptyPassword_returnsFalse() {
        val result = AuthValidator.isRegistrationValid(
            name = "Timothy Jackson",
            email = "timothy@example.com",
            password = ""
        )

        assertFalse(result)
    }

    @Test
    fun registration_withShortPassword_returnsFalse() {
        val result = AuthValidator.isRegistrationValid(
            name = "Timothy Jackson",
            email = "timothy@example.com",
            password = "123"
        )

        assertFalse(result)
    }

    @Test
    fun login_withValidFields_returnsTrue() {
        val result = AuthValidator.isLoginValid(
            email = "timothy@example.com",
            password = "password123"
        )

        assertTrue(result)
    }

    @Test
    fun login_withInvalidEmail_returnsFalse() {
        val result = AuthValidator.isLoginValid(
            email = "notanemail",
            password = "password123"
        )

        assertFalse(result)
    }

    @Test
    fun login_withEmptyPassword_returnsFalse() {
        val result = AuthValidator.isLoginValid(
            email = "timothy@example.com",
            password = ""
        )

        assertFalse(result)
    }

    @Test
    fun passwordReset_withValidEmail_returnsTrue() {
        val result = AuthValidator.isPasswordResetValid(
            "timothy@example.com"
        )

        assertTrue(result)
    }

    @Test
    fun passwordReset_withInvalidEmail_returnsFalse() {
        val result = AuthValidator.isPasswordResetValid(
            "notanemail"
        )

        assertFalse(result)
    }

    @Test
    fun passwordReset_withEmptyEmail_returnsFalse() {
        val result = AuthValidator.isPasswordResetValid("")

        assertFalse(result)
    }
}