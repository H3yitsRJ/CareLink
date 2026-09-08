package com.example.carelink

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationTest {

    @Test
    fun registration_withNewUser_succeeds() {
        val auth = FakeAuthRepository()

        val result = auth.register(
            email = "timothy@example.com",
            password = "password123"
        )

        assertTrue(result)
        assertTrue(auth.isLoggedIn())
    }

    @Test
    fun registration_withExistingUser_fails() {
        val auth = FakeAuthRepository()

        auth.register(
            email = "timothy@example.com",
            password = "password123"
        )

        val result = auth.register(
            email = "timothy@example.com",
            password = "differentPassword"
        )

        assertFalse(result)
    }

    @Test
    fun login_withCorrectCredentials_succeeds() {
        val auth = FakeAuthRepository()

        auth.register(
            email = "timothy@example.com",
            password = "password123"
        )

        auth.logout()

        val result = auth.login(
            email = "timothy@example.com",
            password = "password123"
        )

        assertTrue(result)
        assertTrue(auth.isLoggedIn())
    }

    @Test
    fun login_withWrongPassword_fails() {
        val auth = FakeAuthRepository()

        auth.register(
            email = "timothy@example.com",
            password = "password123"
        )

        auth.logout()

        val result = auth.login(
            email = "timothy@example.com",
            password = "wrongPassword"
        )

        assertFalse(result)
        assertFalse(auth.isLoggedIn())
    }

    @Test
    fun login_withUnknownUser_fails() {
        val auth = FakeAuthRepository()

        val result = auth.login(
            email = "unknown@example.com",
            password = "password123"
        )

        assertFalse(result)
        assertFalse(auth.isLoggedIn())
    }

    @Test
    fun logout_clearsCurrentUser() {
        val auth = FakeAuthRepository()

        auth.register(
            email = "timothy@example.com",
            password = "password123"
        )

        assertTrue(auth.isLoggedIn())

        auth.logout()

        assertFalse(auth.isLoggedIn())
        assertNull(auth.currentUserEmail)
    }

    @Test
    fun authenticationState_afterLogin_isRestored() {
        val auth = FakeAuthRepository()

        auth.register(
            email = "timothy@example.com",
            password = "password123"
        )

        assertTrue(auth.isLoggedIn())
        assertTrue(auth.currentUserEmail == "timothy@example.com")
    }
}