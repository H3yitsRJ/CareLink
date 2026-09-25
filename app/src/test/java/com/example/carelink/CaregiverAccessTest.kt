package com.example.carelink

import com.example.carelink.model.CarePermission
import com.example.carelink.model.CaregiverAccess
import org.junit.Assert.*
import org.junit.Test

class CaregiverAccessTest {
    private val grant = CaregiverAccess("grant1", "patient1", "caregiver1", CarePermission.entries.toSet())

    @Test fun firestoreSchemaRepresentsEverySupportedPermission() {
        assertEquals(mapOf<String, Any>(
            "patientId" to "patient1", "caregiverId" to "caregiver1",
            "permissions" to listOf("VIEW", "ADD", "EDIT", "DELETE", "RECORD_DOSE", "RECEIVE_REMINDERS"),
            "revoked" to false
        ), grant.toFirestore())
        assertEquals(grant, CaregiverAccess.fromFirestore(grant.id, grant.toFirestore()))
    }

    @Test fun eachPermissionIsIndependentAndRoundTrips() {
        CarePermission.entries.forEach { allowed ->
            val original = grant.copy(permissions = setOf(allowed))
            val restored = CaregiverAccess.fromFirestore(original.id, original.toFirestore())!!
            assertEquals(original, restored)
            CarePermission.entries.forEach { permission -> assertEquals(permission == allowed, restored.allows(permission)) }
        }
    }

    @Test fun emptyPermissionsGrantNoAccess() {
        val empty = grant.copy(permissions = emptySet())
        val restored = CaregiverAccess.fromFirestore(empty.id, empty.toFirestore())!!
        assertEquals(empty, restored)
        CarePermission.entries.forEach { assertFalse(restored.allows(it)) }
    }

    @Test fun revokedGrantRoundTripsAndDeniesEveryPermission() {
        val revoked = grant.copy(revoked = true)
        val restored = CaregiverAccess.fromFirestore(revoked.id, revoked.toFirestore())!!
        assertEquals(revoked, restored)
        CarePermission.entries.forEach { assertFalse(restored.allows(it)) }
    }

    @Test fun missingOrMalformedRevocationFailsClosed() {
        val data = grant.toFirestore()
        val variants = listOf(data - "revoked") + listOf(null, "false", 0, true).map { data + ("revoked" to it) }
        variants.forEach { document ->
            val restored = CaregiverAccess.fromFirestore(grant.id, document)!!
            assertTrue(restored.revoked)
            CarePermission.entries.forEach { assertFalse(restored.allows(it)) }
        }
    }

    @Test fun invalidRequiredFieldsRejectGrant() {
        val data = grant.toFirestore()
        assertNull(CaregiverAccess.fromFirestore(" ", data))
        listOf("patientId", "caregiverId").forEach { key ->
            assertNull(CaregiverAccess.fromFirestore(grant.id, data - key))
            listOf(null, "", " ", 123).forEach { value ->
                assertNull(CaregiverAccess.fromFirestore(grant.id, data + (key to value)))
            }
        }
        assertNull(CaregiverAccess.fromFirestore(grant.id, data - "permissions"))
        listOf(null, "VIEW", 123).forEach { value ->
            assertNull(CaregiverAccess.fromFirestore(grant.id, data + ("permissions" to value)))
        }
    }

    @Test fun unknownPermissionsDoNotCreateRightsAndDuplicatesAreRemoved() {
        val restored = CaregiverAccess.fromFirestore(grant.id, grant.toFirestore() +
            ("permissions" to listOf("VIEW", "VIEW", "ADMIN", "edit", 123, null)))!!
        assertEquals(setOf(CarePermission.VIEW), restored.permissions)
        assertFalse(restored.allows(CarePermission.EDIT))
        assertEquals(grant.id, CaregiverAccess.fromFirestore(grant.id,
            grant.toFirestore() + ("id" to "different-id"))!!.id)
    }
}
