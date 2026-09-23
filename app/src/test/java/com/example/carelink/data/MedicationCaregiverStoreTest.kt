package com.example.carelink.data

import com.example.carelink.model.*
import org.junit.Assert.*
import org.junit.Test

class MedicationCaregiverStoreTest {
    private val medication = Medication("med", "patient", "Example", "5 mg", "1 tablet", "Daily", listOf("08:00"))
    @Test fun concurrentChangesOrDeletionCannotBeOverwritten() {
        assertEquals("2 tablets", MedicationCaregiverStore.editFields(medication, medication, medication.copy(dose = "2 tablets"))["dose"])
        listOf(null, medication.copy(dose = "3 tablets")).forEach { current ->
            assertTrue(runCatching { MedicationCaregiverStore.editFields(medication, current, medication.copy(dose = "2 tablets")) }.isFailure)
        }
    }
    @Test fun invalidEditsCannotChangeIdentityOrRequiredValues() {
        listOf(medication.copy(id = "other"), medication.copy(patientId = "other"), medication.copy(name = ""), medication.copy(reminderTimes = listOf("25:00"))).forEach { edited ->
            assertTrue(runCatching { MedicationCaregiverStore.editFields(medication, medication, edited) }.isFailure)
        }
    }
    @Test fun grantsAreBoundToThePatientAndCaregiverAndRevocationWins() {
        val grant = mapOf("patientId" to "patient", "caregiverId" to "caregiver", "permissions" to listOf("VIEW", "EDIT"), "revoked" to false)
        assertTrue(MedicationCaregiverStore.access("patient", "caregiver", grant)!!.allows(CarePermission.EDIT))
        assertNull(MedicationCaregiverStore.access("other", "caregiver", grant))
        assertNull(MedicationCaregiverStore.access("patient", "other", grant))
        assertFalse(MedicationCaregiverStore.access("patient", "caregiver", grant + ("revoked" to true))!!.allows(CarePermission.VIEW))
        assertFalse(MedicationCaregiverStore.access("patient", "caregiver", grant - "revoked")!!.allows(CarePermission.VIEW))
    }
    @Test fun editableFieldsExcludeIdentityLifecycleAndAttribution() {
        val fields = MedicationCaregiverStore.editableFields(Medication("med", "patient", "Example", "5 mg", "1 tablet", "Daily", listOf("08:00"), updatedById = "patient"))
        assertEquals(setOf("name", "strength", "dose", "frequency", "reminderTimes", "instructions"), fields.keys)
        assertEquals(listOf("08:00"), fields["reminderTimes"])
    }
    @Test fun accountCodesCannotContainFirestorePaths() {
        assertTrue(MedicationCaregiverStore.validId("patient-code"))
        listOf("", " ", ".", "..", "users/patient", "x".repeat(129)).forEach { assertFalse(MedicationCaregiverStore.validId(it)) }
    }
}
