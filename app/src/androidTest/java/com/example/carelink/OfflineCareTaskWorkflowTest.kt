package com.example.carelink

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.carelink.data.CareTaskRepository
import com.example.carelink.data.InMemoryCareTaskRepository
import com.example.carelink.model.CareTask
import com.example.carelink.screens.CareTasksFlow
import com.example.carelink.screens.LocalConnectionAvailable
import com.example.carelink.screens.OFFLINE_ACTION_MESSAGE
import com.example.carelink.screens.OfflineBanner
import com.example.carelink.ui.theme.CareLinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class OfflineCareTaskWorkflowTest {
    @get:Rule val compose = createComposeRule()
    private val repository = InMemoryCareTaskRepository()
    private val connection = mutableStateOf(false)
    private val banner = "CareLink can't connect. Saved information is available. Changes that need a connection are paused."

    private fun open(store: CareTaskRepository) {
        compose.setContent {
            CareLinkTheme {
                CompositionLocalProvider(LocalConnectionAvailable provides connection.value) {
                    Column(Modifier.fillMaxSize()) {
                        OfflineBanner(!connection.value)
                        Box(Modifier.weight(1f)) { CareTasksFlow("patient1", store) }
                    }
                }
            }
        }
    }

    @Test fun offlineCachedTaskStaysReadableAndCompletionWaitsForConnection() {
        val cached = CareTask("cached", "patient1", "Call clinic", "", "2099-01-01", "09:00")
        repository.create(cached) { assertTrue(it.isSuccess) }
        var completionRequests = 0
        var listRequests = 0
        val store = object : CareTaskRepository by repository {
            override fun list(patientId: String, completed: (Result<List<CareTask>>) -> Unit) {
                listRequests++
                repository.list(patientId, completed)
            }
            override fun setCompleted(patientId: String, id: String, completed: Boolean, result: (Result<Unit>) -> Unit) {
                completionRequests++
                repository.setCompleted(patientId, id, completed, result)
            }
        }
        open(store)
        compose.onNodeWithText(banner).assertExists()
        compose.onNodeWithText("Call clinic").assertExists()
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText(OFFLINE_ACTION_MESSAGE).assertExists()
        compose.onNodeWithText("Call clinic").assertExists()
        compose.onNode(isToggleable()).assertIsOff()
        var requestsBeforeReconnect = 0
        compose.runOnIdle {
            assertEquals(0, completionRequests)
            requestsBeforeReconnect = listRequests
            // Simulate a server-side change available to the next refreshed list.
            repository.create(cached.copy(id = "new", title = "Pick up medication")) { assertTrue(it.isSuccess) }
            connection.value = true
        }
        compose.onNodeWithText(banner).assertDoesNotExist()
        compose.onNodeWithText("Pick up medication").assertExists()
        compose.runOnIdle { assertTrue(listRequests > requestsBeforeReconnect) }
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.runOnIdle { assertEquals(1, completionRequests) }
    }

    @Test fun offlineSavePreservesDraftAndReconnectAllowsSave() {
        var createRequests = 0
        val store = object : CareTaskRepository by repository {
            override fun create(task: CareTask, completed: (Result<CareTask>) -> Unit) {
                createRequests++
                repository.create(task, completed)
            }
        }
        open(store)
        compose.onNodeWithText("Add care task").performClick()
        compose.onNodeWithText("Task title").performScrollTo().performTextReplacement("Call clinic")
        compose.onNodeWithText("Description (optional)").performScrollTo().performTextReplacement("Ask about results")
        compose.onNodeWithText("Due date").performScrollTo().performTextReplacement("2099-01-01")
        compose.onNodeWithText("Time").performScrollTo().performTextReplacement("09:00")
        compose.onNodeWithText("Save care task").performScrollTo().performClick()
        compose.onNodeWithText(OFFLINE_ACTION_MESSAGE).assertExists()
        compose.runOnIdle { assertEquals(0, createRequests); connection.value = true }
        compose.onNodeWithText(banner).assertDoesNotExist()
        compose.onNodeWithText("Call clinic").assertExists()
        compose.onNodeWithText("Ask about results").assertExists()
        compose.onNodeWithText("Save care task").performScrollTo().performClick()
        compose.onNodeWithText("Care tasks").assertExists()
        compose.onNodeWithText("Call clinic").assertExists()
        compose.runOnIdle { assertEquals(1, createRequests) }
    }
}
