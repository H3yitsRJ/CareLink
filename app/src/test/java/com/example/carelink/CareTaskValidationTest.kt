package com.example.carelink

import com.example.carelink.data.InMemoryCareTaskRepository
import com.example.carelink.model.CareTask
import com.example.carelink.model.validationErrors
import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class CareTaskValidationTest {
    private val task = CareTask("task1", "patient1", "Call clinic", "Ask about results", "2099-01-01", "09:00")
    private val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("2026-09-24 12:00")!!

    @Test fun requiredFieldsAndRealDatesAreValidated() {
        assertEquals(setOf("title", "dueDate", "time"), task.copy(title = " ", dueDate = "", time = "").validationErrors(now).keys)
        listOf("2027-02-29", "2026-04-31", "2026-13-01", "2026-1-01").forEach {
            assertTrue(task.copy(dueDate = it).validationErrors(now).containsKey("dueDate"))
        }
        assertTrue(task.copy(dueDate = "2028-02-29").validationErrors(now).isEmpty())
        assertTrue(task.copy(time = "24:00").validationErrors(now).containsKey("time"))
    }

    @Test fun deadlineMustBeInFutureIncludingTimeToday() {
        listOf("2026-09-23" to "23:59", "2026-09-24" to "11:59", "2026-09-24" to "12:00").forEach { (date, time) ->
            assertTrue(task.copy(dueDate = date, time = time).validationErrors(now).containsKey("dueDate"))
        }
        assertTrue(task.copy(dueDate = "2026-09-24", time = "12:01").validationErrors(now).isEmpty())
    }

    @Test fun repositoryValidatesCreatesAndListsForCorrectPatient() {
        val repository = InMemoryCareTaskRepository()
        repository.create(task.copy(dueDate = "2000-01-01")) { assertTrue(it.isFailure) }
        repository.create(task) { assertEquals(task, it.getOrThrow()) }
        repository.create(task) { assertTrue(it.isFailure) }
        repository.list("patient1") { assertEquals(listOf(task), it.getOrThrow()) }
        repository.list("patient2") { assertTrue(it.getOrThrow().isEmpty()) }
    }
}
