package com.example.carelink.data

import org.junit.Assert.*
import org.junit.Test

class CareRecipientDirectoryTest {
    private val grant = mapOf<String, Any?>("patientId" to "patient", "caregiverId" to "caregiver",
        "patientName" to "Pat", "permissions" to listOf("VIEW"), "revoked" to false)
    private fun parse(data: Map<String, Any?> = grant, path: String = "users/patient/medicationCaregivers/caregiver") =
        FirestoreCareRecipientDirectory.recipient("caregiver", path, data)
    @Test fun authorizedRecipientIsNamedAndMissingNameFallsBackToId() {
        assertEquals(CareRecipient("patient", "Pat"), parse())
        assertEquals("patient", parse(grant - "patientName")!!.name)
    }
    @Test fun revokedMissingViewAndUnverifiedGrantsAreExcluded() {
        assertNull(parse(grant + ("revoked" to true)))
        assertNull(parse(grant - "revoked"))
        assertNull(parse(grant + ("permissions" to listOf("EDIT"))))
        assertNull(parse(grant + ("permissions" to listOf("UNKNOWN"))))
    }
    @Test fun identityAndDocumentPathMustMatch() {
        assertNull(parse(grant + ("caregiverId" to "stranger")))
        assertNull(parse(grant + ("patientId" to "other")))
        assertNull(parse(path = "users/patient/medicationCaregivers/stranger"))
        assertNull(parse(path = "fake/patient/medicationCaregivers/caregiver"))
        assertNull(parse(path = "users/patient/fake/medicationCaregivers/caregiver"))
    }
}
