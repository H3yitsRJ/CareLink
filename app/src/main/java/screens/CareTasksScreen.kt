package com.example.carelink.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.CareTask
import navigation.BottomNavBar
import navigation.BottomNavDestination

@Composable
fun CareTasksScreen(tasks: List<CareTask> = emptyList(), isLoading: Boolean = false, error: String? = null, onAdd: () -> Unit = {}, onCompletedChange: (CareTask, Boolean) -> Unit = { _, _ -> }, onNavigate: (BottomNavDestination) -> Unit = {}) {
    Scaffold(bottomBar = { BottomNavBar(BottomNavDestination.CareTasks, onNavigate) }) { innerPadding ->
    Column(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Care tasks", style = MaterialTheme.typography.headlineLarge)
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add care task") }
        when {
            isLoading -> StateMessage("Loading care tasks")
            error != null -> StateMessage(error, true)
            tasks.isEmpty() -> StateMessage("No care tasks yet.")
            // Completion changes go back to the parent so the repository remains the source of truth.
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks, key = { it.id }) { task ->
                    CareCard(task.title) {
                        if (task.dueDate.isNotBlank()) Text("Due ${task.dueDate}")
                        if (task.appointmentId != null) Text("Linked to appointment")
                        Checkbox(checked = task.completed, onCheckedChange = { onCompletedChange(task, it) })
                    }
                }
            }
        }
    }
    }
}
