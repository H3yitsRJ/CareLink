package com.example.carelink.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.carelink.data.CareTaskRepository
import com.example.carelink.model.CareTask
import navigation.BottomNavDestination

@Composable
fun CareTasksFlow(patientId: String, repository: CareTaskRepository,
    onNavigate: (BottomNavDestination) -> Unit = {}) {
    key(patientId) {
        var adding by rememberSaveable { mutableStateOf(false) }
        var tasks by remember { mutableStateOf<List<CareTask>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var saving by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var saveError by remember { mutableStateOf<String?>(null) }
        val active = remember { mutableStateOf(true) }
        fun refresh() {
            loading = true
            error = null
            repository.list(patientId) { result ->
                if (active.value) {
                    loading = false
                    result.onSuccess { tasks = it.sortedWith(compareBy(CareTask::dueDate, CareTask::time)) }
                        .onFailure { error = "We couldn't load your care tasks. Please try again." }
                }
            }
        }
        DisposableEffect(repository, patientId) {
            active.value = true
            refresh()
            onDispose { active.value = false }
        }
        BackHandler(enabled = adding) { if (!saving) adding = false }
        if (adding) {
            AddEditCareTaskScreen(patientId = patientId, isSaving = saving, saveError = saveError,
                onSave = { task ->
                    if (!saving) {
                        saving = true
                        saveError = null
                        repository.create(task) { result ->
                            if (active.value) {
                                saving = false
                                result.onSuccess { saved ->
                                    tasks = (tasks.filterNot { it.id == saved.id } + saved)
                                        .sortedWith(compareBy(CareTask::dueDate, CareTask::time))
                                    error = null
                                    adding = false
                                }.onFailure { saveError = "We couldn't save the care task. Please try again." }
                            }
                        }
                    }
                }, onCancel = { adding = false })
        } else {
            CareTasksScreen(tasks = tasks, isLoading = loading, error = error,
                onRetry = { refresh() }, onAdd = { saveError = null; adding = true },
                onCompletedChange = { task, completed ->
                    repository.setCompleted(patientId, task.id, completed) { result ->
                        if (active.value) result.onSuccess {
                            tasks = tasks.map { if (it.id == task.id) it.copy(completed = completed) else it }
                        }.onFailure { error = "We couldn't update the care task. Please try again." }
                    }
                }, onNavigate = onNavigate)
        }
    }
}
