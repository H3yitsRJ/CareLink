package com.example.carelink

object AuthValidator {

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") &&
                email.substringAfter("@").contains(".")
    }

    fun isRegistrationValid(
        name: String,
        email: String,
        password: String
    ): Boolean {
        return name.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                isValidEmail(email) &&
                password.length >= 6
    }

    fun isLoginValid(
        email: String,
        password: String
    ): Boolean {
        return email.isNotBlank() &&
                password.isNotBlank() &&
                isValidEmail(email)
    }

    fun isPasswordResetValid(email: String): Boolean {
        return email.isNotBlank() &&
                isValidEmail(email)
    }
}