package com.example.carelink.data

import com.example.carelink.model.CareTask
import org.junit.Assert.*
import org.junit.Test

class CareTaskRepositoryTest {
    private val store = InMemoryCareTaskRepository()
    private val task = CareTask("task", "patient", "Call clinic", "Ask about results", "2099-01-01", "09:00", appointmentId = "appointment")

    @Test fun createRetrieveUpdateAndCompletePreserveAppointment() {
        store.create(task) { assertEquals(task, it.getOrThrow()) }
        store.list("patient") { assertEquals(listOf(task), it.getOrThrow()) }
        val edited = task.copy(title = "Call provider", description = "Updated notes")
        store.update(edited) { assertEquals(edited, it.getOrThrow()) }
        store.setCompleted("patient", task.id, true) { assertTrue(it.isSuccess) }
        store.list("patient") { assertEquals(listOf(edited.copy(completed = true)), it.getOrThrow()) }
        store.setCompleted("patient", task.id, false) { assertTrue(it.isSuccess) }
        store.list("patient") { assertEquals(listOf(edited), it.getOrThrow()) }
    }

    @Test fun duplicateMissingAndCrossPatientWritesFail() {
        store.create(task) { assertTrue(it.isSuccess) }
        store.create(task) { assertTrue(it.isFailure) }
        store.update(task.copy(id = "missing")) { assertTrue(it.isFailure) }
        store.update(task.copy(patientId = "other")) { assertTrue(it.isFailure) }
        store.setCompleted("other", task.id, true) { assertTrue(it.isFailure) }
        store.list("other") { assertTrue(it.getOrThrow().isEmpty()) }
        store.list("patient") { assertEquals(listOf(task), it.getOrThrow()) }
    }

    @Test fun invalidInputsDoNotWriteAndOverdueTasksCanBeEdited() {
        store.create(task.copy(id = "bad/id")) { assertTrue(it.isFailure) }
        store.create(task.copy(title = "")) { assertTrue(it.isFailure) }
        store.create(task.copy(appointmentId = "..")) { assertTrue(it.isFailure) }
        store.create(task) { assertTrue(it.isSuccess) }
        store.update(task.copy(dueDate = "2000-01-01")) { assertTrue(it.isSuccess) }
        store.update(task.copy(dueDate = "invalid")) { assertTrue(it.isFailure) }
        store.list("bad/id") { assertTrue(it.isFailure) }
    }
}