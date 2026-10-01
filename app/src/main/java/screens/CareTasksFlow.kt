package com.example.carelink.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.carelink.data.CareTaskRepository
import com.example.carelink.model.CareTask
import com.example.carelink.model.Appointment
import navigation.BottomNavDestination

@Composable
fun CareTasksFlow(patientId: String, repository: CareTaskRepository,
    onNavigate: (BottomNavDestination) -> Unit = {}, sourceAppointment: Appointment? = null, onCancelFollowUp: () -> Unit = {}) {
    key(patientId) {
        var adding by rememberSaveable { mutableStateOf(sourceAppointment != null) }
        var tasks by remember { mutableStateOf<List<CareTask>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var saving by remember { mutableStateOf(false) }
        var followingUp by rememberSaveable { mutableStateOf(sourceAppointment != null) }
        var error by remember { mutableStateOf<String?>(null) }
        var saveError by remember { mutableStateOf<String?>(null) }
        val active = remember { mutableStateOf(true) }
        var revision by remember { mutableStateOf(0) }
        fun refresh() {
            val requestedRevision = revision
            loading = true
            error = null
            repository.list(patientId) { result ->
                if (active.value) {
                    loading = false
                    result.onSuccess { if (revision == requestedRevision) tasks = it.sortedWith(compareBy(CareTask::dueDate, CareTask::time)) }
                        .onFailure { error = "We couldn't load your care tasks. Please try again." }
                }
            }
        }
        DisposableEffect(repository, patientId) {
            active.value = true
            refresh()
            onDispose { active.value = false }
        }
        fun cancel() {
            adding = false
            if (followingUp) onCancelFollowUp()
        }
        BackHandler(enabled = adding) { if (!saving) cancel() }
        if (adding) {
            AddEditCareTaskScreen(patientId = patientId, sourceAppointment = sourceAppointment.takeIf { followingUp }, isSaving = saving, saveError = saveError,
                onSave = { task ->
                    if (!saving) {
                        saving = true
                        saveError = null
                        repository.create(task) { result ->
                            if (active.value) {
                                saving = false
                                result.onSuccess { saved ->
                                    revision++
                                    tasks = (tasks.filterNot { it.id == saved.id } + saved)
                                        .sortedWith(compareBy(CareTask::dueDate, CareTask::time))
                                    error = null
                                    followingUp = false
                                    adding = false
                                }.onFailure { saveError = "We couldn't save the care task. Please try again." }
                            }
                        }
                    }
                }, onCancel = { cancel() })
        } else {
            CareTasksScreen(tasks = tasks, isLoading = loading, error = error,
                onRetry = { refresh() }, onAdd = { followingUp = false; saveError = null; adding = true },
                onCompletedChange = { task, completed ->
                    repository.setCompleted(patientId, task.id, completed) { result ->
                        if (active.value) result.onSuccess {
                            revision++
                            tasks = tasks.map { if (it.id == task.id) it.copy(completed = completed) else it }
                        }.onFailure { error = "We couldn't update the care task. Please try again." }
                    }
                }, onNavigate = onNavigate)
        }
    }
}
