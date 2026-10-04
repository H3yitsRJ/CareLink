package com.example.carelink.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.CareTask
import kotlinx.coroutines.delay
import navigation.BottomNavBar
import navigation.BottomNavDestination
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CareTasksScreen(
    tasks: List<CareTask> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    onAdd: () -> Unit = {},
    onRetry: () -> Unit = {},
    onCompletedChange: (CareTask, Boolean) -> Unit = { _, _ -> },
    onNavigate: (BottomNavDestination) -> Unit = {},
    onTaskSelected: (CareTask) -> Unit = {}
) {
    var filter by rememberSaveable { mutableStateOf("All") }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    // Refresh overdue labels while the screen remains open.
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(30_000)
        }
    }

    val visibleTasks = tasks
        .filter { task ->
            when (filter) {
                "Pending" -> !task.completed
                "Completed" -> task.completed
                else -> true
            }
        }
        .sortedWith(
            compareBy<CareTask> { it.dueDate.isBlank() }
                .thenBy { it.dueDate }
                .thenBy { it.time.ifBlank { "23:59" } }
                .thenBy { it.title }
                .thenBy { it.id }
        )

    Scaffold(
        bottomBar = { BottomNavBar(BottomNavDestination.CareTasks, onNavigate) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                 "Care tasks",
                 style = MaterialTheme.typography.headlineLarge
            )
            Button(
                onClick = onAdd,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add care task")
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Pending", "Completed").forEach { label ->
                    FilterChip(
                        selected = filter == label,
                        onClick = { filter = label },
                        label = { Text(label) }
                    )
                }
            }

            when {
                isLoading -> StateMessage("Loading care tasks")

                error != null -> {
                    StateMessage(error, true)
                    Button(onClick = onRetry) {
                            Text("Retry")
                    }
                }

                tasks.isEmpty() -> StateMessage("No care tasks yet.")

                visibleTasks.isEmpty() -> StateMessage("No tasks match this filter.")

                // Completion changes go back to the parent so the repository remains the source of truth.
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleTasks, key = { it.id }) { task ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTaskSelected(task) }
                        ) {
                            CareCard(task.title) {
                                Text(
                                    when {
                                        task.dueDate.isBlank() -> "No due date"
                                        task.time.isBlank() -> "Due ${task.dueDate} • No time set"
                                        else -> "Due ${task.dueDate} at ${task.time}"
                                    }
                                )

                                Text(
                                    text = if (task.completed) {
                                        "Status: Completed"
                                    } else {
                                        "Status: Pending"
                                    }
                                )

                                val deadline = taskDeadlineMillis(task)
                                if (
                                    !task.completed &&
                                    deadline != null &&
                                    nowMillis > deadline
                                ) {
                                    Text(
                                        text = "Overdue",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                if (task.description.isNotBlank()) {
                                    Text(task.description)
                                }

                                if (!task.appointmentId.isNullOrBlank()) {
                                    Text("Linked to appointment")
                                }

                                Row(
                                    verticalAlignment =
                                        androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.completed,
                                        onCheckedChange = {
                                            onCompletedChange(task, it)
                                        }
                                    )
                                    Text("Completed")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun taskDeadlineMillis(task: CareTask): Long? {
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(task.dueDate)) {
        return null
    }

    val time = task.time.ifBlank { "23:59" }
    if (!Regex("\\d{2}:\\d{2}").matches(time)) {
        return null
    }

    val value = "${task.dueDate} $time"
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        isLenient = false
    }
    val position = ParsePosition(0)
    val parsed = formatter.parse(value, position) ?: return null

    if (position.index != value.length) return null

    return if (task.time.isBlank()) {
        parsed.time + 59_999L
    } else {
        parsed.time
    }
}
