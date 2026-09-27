package com.example.carelink.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CareTaskTest {

    @Test
    fun newTaskIsPendingWithOptionalFieldsEmpty() {
        val task = CareTask(id = "task-1", patientId = "patient-1", title = "Call doctor")

        assertEquals(CareTaskStatus.PENDING, task.status)
        assertEquals("", task.description)
        assertEquals("", task.dueDate)
        assertEquals("", task.dueTime)
        assertNull(task.appointmentId)
    }

    @Test
    fun taskRoundTripsThroughFirestore() {
        val original = CareTask(
            id = "task-1",
            patientId = "patient-1",
            title = "Arrange a ride",
            dueDate = "2026-10-01",
            completed = true,
            appointmentId = "appointment-1",
            description = "Call the caregiver",
            time = "09:30"
        )

        val stored = original.toFirestore()
        val restored = CareTask.fromFirestore(original.id, stored)

        assertEquals("COMPLETED", stored["status"])
        assertEquals(original, restored)
    }

    @Test
    fun olderFirestoreRecordUsesCompletedField() {
        val restored = CareTask.fromFirestore(
            "task-1",
            mapOf(
                "patientId" to "patient-1",
                "title" to "Call doctor",
                "completed" to true
            )
        )

        assertTrue(restored!!.completed)
        assertEquals(CareTaskStatus.COMPLETED, restored.status)
    }

    @Test
    fun changingCompletionChangesStatus() {
        val task = CareTask(id = "task-1", patientId = "patient-1", title = "Call doctor")

        val completedTask = task.copy(completed = true)

        assertFalse(task.completed)
        assertEquals(CareTaskStatus.PENDING, task.status)
        assertTrue(completedTask.completed)
        assertEquals(CareTaskStatus.COMPLETED, completedTask.status)
    }

    @Test
    fun missingRequiredFirestoreFieldsAreRejected() {
        assertNull(CareTask.fromFirestore("task-1", mapOf("title" to "Call doctor")))
        assertNull(CareTask.fromFirestore("task-1", mapOf("patientId" to "patient-1")))
    }
}
