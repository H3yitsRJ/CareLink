package com.example.carelink

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.data.CareTaskRepository
import com.example.carelink.data.InMemoryCareTaskRepository
import com.example.carelink.model.CareTask
import com.example.carelink.screens.CareTasksFlow
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CareTaskWorkflowTest {
    @get:Rule val compose = createComposeRule()
    private val repository = InMemoryCareTaskRepository()
    private fun open(store: CareTaskRepository = repository) {
        compose.setContent { CareLinkTheme { CareTasksFlow("patient1", store) } }
        compose.onNodeWithText("Add care task").performClick()
    }
    private fun fill() {
        compose.onNodeWithText("Task title").performScrollTo().performTextReplacement("Call clinic")
        compose.onNodeWithText("Description (optional)").performScrollTo().performTextReplacement("Ask about results")
        compose.onNodeWithText("Due date").performScrollTo().performTextReplacement("2099-01-01")
        compose.onNodeWithText("Time").performScrollTo().performTextReplacement("09:00")
    }
    private fun save() = compose.onNodeWithText("Save care task").performScrollTo().performClick()

    @Test fun validTaskSavesThroughRepositoryAndAppearsInList() {
        open(); fill(); save()
        compose.onNodeWithText("Care tasks").assertExists()
        compose.onNodeWithText("Call clinic").assertExists()
        compose.onNodeWithText("Ask about results").assertExists()
        compose.onNodeWithText("Due 2099-01-01 at 09:00").assertExists()
        compose.runOnIdle {
            repository.list("patient1") {
                val task = it.getOrThrow().single()
                assertEquals("Call clinic", task.title)
                assertEquals("Ask about results", task.description)
                assertEquals("09:00", task.time)
            }
        }
    }

    @Test fun requiredAndPastDatesBlockSavingUntilCorrected() {
        open(); save()
        compose.onNodeWithText("Enter a task title").assertExists()
        compose.onNodeWithText("Use a valid date in YYYY-MM-DD format").assertExists()
        compose.onNodeWithText("Use 24-hour time in HH:MM format").assertExists()
        fill()
        compose.onNodeWithText("Due date").performScrollTo().performTextReplacement("2000-01-01")
        save()
        compose.onNodeWithText("Choose a future due date and time").assertExists()
        compose.runOnIdle { repository.list("patient1") { assertTrue(it.getOrThrow().isEmpty()) } }
        compose.onNodeWithText("Due date").performScrollTo().performTextReplacement("2099-01-01")
        save()
        compose.onNodeWithText("Call clinic").assertExists()
        compose.onNodeWithText("Save care task").assertDoesNotExist()
    }

    @Test fun cancelDiscardsDraftWithoutCreatingTask() {
        open(); fill()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithText("No care tasks yet.").assertExists()
        compose.runOnIdle { repository.list("patient1") { assertTrue(it.getOrThrow().isEmpty()) } }
        compose.onNodeWithText("Add care task").performClick()
        compose.onNodeWithText("Call clinic").assertDoesNotExist()
    }

    @Test fun failedSavePreservesDraftAndAllowsRetry() {
        var fail = true
        val store = object : CareTaskRepository by repository {
            override fun create(task: CareTask, completed: (Result<CareTask>) -> Unit) {
                if (fail) completed(Result.failure(IllegalStateException("Unavailable")))
                else repository.create(task, completed)
            }
        }
        open(store); fill(); save()
        compose.onNodeWithText("We couldn't save the care task. Please try again.").assertExists()
        compose.onNodeWithText("Call clinic").assertExists()
        compose.runOnIdle { fail = false }
        save()
        compose.onNodeWithText("Care tasks").assertExists()
        compose.runOnIdle { repository.list("patient1") { assertEquals(1, it.getOrThrow().size) } }
    }
}
