package com.example.carelink.model

import org.junit.Assert.*
import org.junit.Test

class CaregiverInvitationTest {
    private val invitation = CaregiverInvitation("invitation", "sender", "caregiver@example.com", "patient", InvitationStatus.PENDING, 2_000L)

    @Test fun everyStatusRoundTripsWithoutChangingIdentity() {
        InvitationStatus.entries.forEach { status ->
            val value = invitation.copy(status = status)
            assertEquals(value, CaregiverInvitation.fromFirestore(value.id, value.toFirestore()))
        }
    }

    @Test fun expirationIncludesTheExactBoundaryForEveryStatus() {
        InvitationStatus.entries.forEach { status ->
            val value = invitation.copy(status = status)
            assertFalse(value.isExpired(1_999L))
            assertTrue(value.isExpired(2_000L))
            assertTrue(value.isExpired(2_001L))
        }
    }

    @Test fun missingOrMalformedRemoteFieldsAreRejected() {
        val fields = invitation.toFirestore()
        fields.keys.forEach { assertNull(CaregiverInvitation.fromFirestore(invitation.id, fields - it)) }
        listOf("status" to "unknown", "recipientEmail" to "invalid", "senderId" to "", "patientId" to "x/y",
            "expiresAtMillis" to -1L, "expiresAtMillis" to 2_000.5).forEach { invalid ->
            assertNull(CaregiverInvitation.fromFirestore(invitation.id, fields + invalid))
        }
        assertNull(CaregiverInvitation.fromFirestore("", fields))
        listOf(".", "..").forEach { invalid ->
            assertNull(CaregiverInvitation.fromFirestore(invalid, fields))
            assertNull(CaregiverInvitation.fromFirestore(invitation.id, fields + ("senderId" to invalid)))
            assertNull(CaregiverInvitation.fromFirestore(invitation.id, fields + ("patientId" to invalid)))
        }
    }
}
