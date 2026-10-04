package com.example.carelink.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.example.carelink.data.CareTaskRepository
import com.example.carelink.model.CareTask
import com.example.carelink.model.Appointment
import com.example.carelink.notifications.CareTaskReminderScheduler
import navigation.BottomNavDestination

@Composable
fun CareTasksFlow(
    patientId: String,
    repository: CareTaskRepository,
    onNavigate: (BottomNavDestination) -> Unit = {},
    sourceAppointment: Appointment? = null,
    onCancelFollowUp: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val reminderScheduler = remember(context) { CareTaskReminderScheduler(context) }
    key(patientId) {
        var adding by rememberSaveable { mutableStateOf(sourceAppointment != null) }
        var selectedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
        var tasks by remember { mutableStateOf<List<CareTask>>(emptyList()) }
        val selectedTask = tasks.find { it.id == selectedTaskId }
        var loading by remember { mutableStateOf(true) }
        var saving by remember { mutableStateOf(false) }
        var followingUp by rememberSaveable { mutableStateOf(sourceAppointment != null) }
        var error by remember { mutableStateOf<String?>(null) }
        var saveError by remember { mutableStateOf<String?>(null) }
        val active = remember { mutableStateOf(true) }
        var revision by remember { mutableStateOf(0) }
        val changes = remember { mutableMapOf<String, Pair<Int, CareTask>>() }
        val deletedIds = remember { mutableSetOf<String>() }
        var updatingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
        fun refresh() {
            val requestedRevision = revision
            loading = true
            error = null

            repository.list(patientId) { result ->
                if (active.value) {
                    loading = false

                    result.onSuccess { loaded ->
                        val newer = changes.values
                            .filter { it.first > requestedRevision }
                            .map { it.second }

                        tasks = (
                            loaded.filterNot { task ->
                                task.id in deletedIds || newer.any { it.id == task.id }
                            } + newer.filterNot { it.id in deletedIds }
                        ).sortedWith(compareBy(CareTask::dueDate, CareTask::time))
                    }.onFailure { if (revision == requestedRevision) error = "We couldn't load your care tasks. Please try again." }
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
            selectedTaskId = null
            saveError = null
            if (followingUp) {
                followingUp = false
                onCancelFollowUp()
            }
        }

        BackHandler(enabled = adding) {
            if (!saving) cancel()
        }

        // Wait for a restored edit selection to finish loading.
        if (adding && selectedTaskId != null && loading) {
            StateMessage("Loading care task")
        } else if (
            adding &&
            selectedTaskId != null &&
            selectedTask == null
        ) {
            androidx.compose.foundation.layout.Column {
                StateMessage(error ?: "This care task is no longer available.", true)
                androidx.compose.material3.Button(
                    onClick = { refresh() }
                ) {
                    androidx.compose.material3.Text("Retry")
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = { cancel() }
                ) {
                    androidx.compose.material3.Text("Back to care tasks")
                }
            }
        } else if (adding) {

            AddEditCareTaskScreen(
                task = selectedTask,
                patientId = patientId,
                sourceAppointment = sourceAppointment.takeIf { followingUp },
                isSaving = saving,
                saveError = saveError,
                onSave = { task ->
                    if (!saving) {
                        saving = true
                        saveError = null

                        val saveTask: (CareTask, (Result<CareTask>) -> Unit) -> Unit =
                            if (selectedTaskId == null) {
                                repository::create
                            } else {
                                repository::update
                            }

                        saveTask(task) { result ->
                            // Update the alarm even if the user left the screen.
                            result.onSuccess { saved -> reminderScheduler.schedule(saved) }

                            if (active.value) {
                                saving = false

                                result.onSuccess { saved ->
                                    revision++
                                    changes[saved.id] = revision to saved
                                    tasks = (tasks.filterNot { it.id == saved.id } + saved).sortedWith(compareBy(CareTask::dueDate, CareTask::time))
                                    error = null
                                    followingUp = false
                                    selectedTaskId = null
                                    adding = false
                                }.onFailure {
                                    saveError = "We couldn't save the care task. Please try again."
                                }
                            }
                        }
                    }
                },
                onCancel = { cancel() },
                onDelete = selectedTask?.let { task ->
                    {
                        if (!saving) {
                            saving = true
                            saveError = null

                            repository.delete(
                                patientId,
                                task.id
                            ) { result ->
                                result.onSuccess {
                                    reminderScheduler.cancel(
                                        patientId,
                                        task.id
                                    )
                                }

                                if (active.value) {
                                    saving = false
                                    result.onSuccess {
                                        revision++
                                        deletedIds.add(task.id)
                                        changes.remove(task.id)

                                        tasks = tasks.filterNot {
                                            it.id == task.id
                                        }

                                        selectedTaskId = null
                                        followingUp = false
                                        adding = false
                                        error = null
                                    }.onFailure {
                                        saveError =
                                            "We couldn't delete the care task. Please try again."
                                    }
                                }
                            }
                        }
                    }
                }
            )

        } else {

            CareTasksScreen(
                tasks = tasks,
                isLoading = loading,
                error = error,
                onRetry = { refresh() },
                onAdd = {
                    selectedTaskId = null
                    followingUp = false
                    saveError = null
                    adding = true
                },
                onTaskSelected = { task ->
                    if (task.id !in updatingIds) {
                        selectedTaskId = task.id
                        followingUp = false
                        saveError = null
                        adding = true
                    }
                },
                onCompletedChange = { task, completed ->
                    if (task.id !in updatingIds) {
                        updatingIds = updatingIds + task.id

                        repository.setCompleted(
                            patientId,
                            task.id,
                            completed
                        ) { result ->
                            result.onSuccess {
                                reminderScheduler.schedule(
                                    task.copy(completed = completed)
                                )
                            }

                            if (active.value) {
                                updatingIds = updatingIds - task.id
                                result.onSuccess {
                                    revision++
                                    tasks = tasks.map { if (it.id == task.id) { it.copy(completed = completed) } else { it } }
                                    tasks.find { it.id == task.id }?.let { changes[it.id] = revision to it }
                                }.onFailure {
                                    error = "We couldn't update the care task. Please try again."
                                }
                            }
                        }
                    }
                },
                onNavigate = onNavigate
            )
        }
    }
}