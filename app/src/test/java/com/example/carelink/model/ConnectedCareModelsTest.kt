package com.example.carelink.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectedCareModelsTest {
    @Test
    fun `care task round trips and retains appointment link`() {
        val task = CareTask("task-1", "patient-1", "Schedule follow-up", "2026-09-10", appointmentId = "appointment-1")
        assertEquals(task, CareTask.fromFirestore(task.id, task.toFirestore()))
    }

    @Test
    fun `invitation identifies expiration and round trips`() {
        val invitation = CaregiverInvitation("invite-1", "owner-1", "caregiver@example.com", "patient-1", InvitationStatus.PENDING, 2_000L)
        assertFalse(invitation.isExpired(1_999L))
        assertTrue(invitation.isExpired(2_000L))
        assertEquals(invitation, CaregiverInvitation.fromFirestore(invitation.id, invitation.toFirestore()))
    }

    @Test
    fun `history filter validates dates and applies types`() {
        val medication = CareHistoryEntry("1", "patient-1", 100L, CareActivityType.MEDICATION, "Dose recorded")
        val appointment = CareHistoryEntry("2", "patient-1", 200L, CareActivityType.APPOINTMENT, "Appointment created")
        assertEquals(listOf(appointment), CareHistoryFilter(150L, 250L, setOf(CareActivityType.APPOINTMENT)).apply(listOf(medication, appointment)))
        assertTrue(CareHistoryFilter(250L, 150L).validate() != null)
    }

    @Test
    fun `caregiver access round trips and revoked access denies permissions`() {
        val access = CaregiverAccess(
            "access-1", "patient-1", "caregiver-1",
            setOf(CarePermission.VIEW, CarePermission.EDIT)
        )
        assertEquals(access, CaregiverAccess.fromFirestore(access.id, access.toFirestore()))
        assertTrue(access.allows(CarePermission.EDIT))
        assertFalse(access.copy(revoked = true).allows(CarePermission.VIEW))
    }
}
