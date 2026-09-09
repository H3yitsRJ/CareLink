package com.example.carelink.screens

import com.example.carelink.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileTest {

    @Test
    fun profileConvertsToFirestoreMap() {
        val profile = UserProfile(
            userId = "user123",
            fullName = "Jane Doe",
            email = "jane@example.com",
            dateOfBirth = "1990-05-10",
            phoneNumber = "555-123-4567",
            primaryPhysician = "Dr. Smith",
            preferredPharmacy = "Care Pharmacy"
        )

        val data = profile.toFirestore()

        assertEquals("user123", data["userId"])
        assertEquals("Jane Doe", data["fullName"])
        assertEquals("jane@example.com", data["email"])
        assertEquals("1990-05-10", data["dateOfBirth"])
        assertEquals("555-123-4567", data["phoneNumber"])
        assertEquals("Dr. Smith", data["primaryPhysician"])
        assertEquals("Care Pharmacy", data["preferredPharmacy"])
    }

    @Test
    fun firestoreMapConvertsBackToProfile() {
        val data = mapOf<String, Any?>(
            "userId" to "user123",
            "fullName" to "Jane Doe",
            "email" to "jane@example.com",
            "dateOfBirth" to "1990-05-10",
            "phoneNumber" to "555-123-4567",
            "primaryPhysician" to "Dr. Smith",
            "preferredPharmacy" to "Care Pharmacy"
        )

        val profile = UserProfile.fromFirestore(data)

        assertNotNull(profile)
        assertEquals("user123", profile?.userId)
        assertEquals("Jane Doe", profile?.fullName)
        assertEquals("jane@example.com", profile?.email)
        assertEquals("1990-05-10", profile?.dateOfBirth)
        assertEquals("555-123-4567", profile?.phoneNumber)
        assertEquals("Dr. Smith", profile?.primaryPhysician)
        assertEquals("Care Pharmacy", profile?.preferredPharmacy)
    }

    @Test
    fun missingOptionalFieldsDoNotCrash() {
        val data = mapOf<String, Any?>(
            "userId" to "user123",
            "fullName" to "Jane Doe",
            "email" to "jane@example.com"
        )

        val profile = UserProfile.fromFirestore(data)

        assertNotNull(profile)
        assertNull(profile?.dateOfBirth)
        assertNull(profile?.phoneNumber)
        assertNull(profile?.primaryPhysician)
        assertNull(profile?.preferredPharmacy)
    }

    @Test
    fun invalidOptionalFieldsDoNotCrash() {
        val data = mapOf<String, Any?>(
            "userId" to "user123",
            "fullName" to "Jane Doe",
            "email" to "jane@example.com",
            "dateOfBirth" to 12345,
            "phoneNumber" to true
        )

        val profile = UserProfile.fromFirestore(data)

        assertNotNull(profile)
        assertNull(profile?.dateOfBirth)
        assertNull(profile?.phoneNumber)
    }

    @Test
    fun missingRequiredFieldReturnsNull() {
        val data = mapOf<String, Any?>(
            "fullName" to "Jane Doe",
            "email" to "jane@example.com"
        )

        val profile = UserProfile.fromFirestore(data)

        assertNull(profile)
    }
}