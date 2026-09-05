package com.example.carelink.data

import com.example.carelink.model.CareTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// This test checks the full care-task lifecycle and its appointment link.
class CareTaskRepositoryTest {
    @Test fun `creates updates completes and links appointment tasks`() {
        val repository = InMemoryCareTaskRepository()
        val task = CareTask("task-1", "patient-1", "Follow up", appointmentId = "appointment-1")
        assertTrue(repository.create(task).isSuccess)
        assertEquals("appointment-1", repository.list("patient-1").single().appointmentId)
        assertTrue(repository.update(task.copy(title = "Call provider")).isSuccess)
        assertTrue(repository.setCompleted(task.id, true).getOrThrow().completed)
    }
}
