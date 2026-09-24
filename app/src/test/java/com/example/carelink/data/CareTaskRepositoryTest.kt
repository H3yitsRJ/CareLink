package com.example.carelink.data

import com.example.carelink.model.CareTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareTaskRepositoryTest {
    @Test fun `invalid document ids are rejected without creating records`() {
        val repository = InMemoryCareTaskRepository()
        val task = CareTask("task", "patient", "Call provider")
        listOf("", " ", ".", "..", "a/b").forEach { invalid ->
            assertTrue(repository.create(task.copy(id = invalid)).isFailure)
            assertTrue(repository.create(task.copy(patientId = invalid)).isFailure)
            assertTrue(repository.create(task.copy(appointmentId = invalid)).isFailure)
        }
        assertTrue(repository.list("patient").isEmpty())
    }

    @Test fun `malformed optional fields are rejected while absent fields keep defaults`() {
        val task = CareTask("task", "patient", "Call provider")
        assertEquals(task, CareTask.fromFirestore(task.id, mapOf("patientId" to "patient", "title" to task.title)))
        listOf("dueDate" to 123L, "completed" to "true", "appointmentId" to 42L).forEach { invalid ->
            assertEquals(null, CareTask.fromFirestore(task.id, task.toFirestore() + invalid))
        }
    }

    @Test fun `invalid duplicate missing and ownership-changing writes fail`() {
        val repository = InMemoryCareTaskRepository()
        val task = CareTask("task", "patient", "Call provider")
        assertTrue(repository.create(task.copy(id = "")).isFailure)
        assertTrue(repository.create(task.copy(title = " ")).isFailure)
        assertTrue(repository.create(task).isSuccess)
        assertTrue(repository.create(task).isFailure)
        assertTrue(repository.update(task.copy(patientId = "other")).isFailure)
        assertTrue(repository.update(task.copy(id = "missing")).isFailure)
        assertTrue(repository.setCompleted("missing", true).isFailure)
        assertTrue(repository.list("other").isEmpty())
        assertEquals(task, repository.list("patient").single())
    }
    @Test
    fun `creates updates completes and links appointment tasks`() {
        val repository = InMemoryCareTaskRepository()
        val task = CareTask("task-1", "patient-1", "Follow up", appointmentId = "appointment-1")
        assertTrue(repository.create(task).isSuccess)
        assertEquals("appointment-1", repository.list("patient-1").single().appointmentId)
        assertTrue(repository.update(task.copy(title = "Call provider")).isSuccess)
        assertTrue(repository.setCompleted(task.id, true).getOrThrow().completed)
    }
}
