package com.example.carelink.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.carelink.data.HealthConcernRepository
import com.example.carelink.model.*

@Composable
fun HealthConcernsFlow(patientId: String, repository: HealthConcernRepository, onBack: () -> Unit = {}) {
    key(patientId) {
        var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
        BackHandler { if (selectedId != null) selectedId = null else onBack() }
        val id = selectedId
        if (id != null) key(id) {
            ConcernDetailRoute(patientId, id, repository, onBack = { selectedId = null })
        } else {
            var concerns by remember { mutableStateOf<List<HealthConcern>>(emptyList()) }
            var loading by remember { mutableStateOf(true) }
            var error by remember { mutableStateOf<String?>(null) }
            var retry by remember { mutableIntStateOf(0) }
            DisposableEffect(patientId, repository, retry) {
                var active = true
                loading = true
                error = null
                val stop = repository.watchList(patientId) { result ->
                    if (active) {
                        loading = false
                        result.onSuccess { concerns = it }.onFailure { error = "We couldn't load your health concerns." }
                    }
                }
                onDispose { active = false; stop() }
            }
            Scaffold(Modifier.safeDrawingPadding(), bottomBar = {
                TextButton(onClick = onBack) { Text("Back to home") }
            }) { padding ->
                Column(Modifier.padding(padding)) {
                    if (error != null) TextButton(onClick = { retry++ }) { Text("Retry") }
                    HealthConcernsScreen(concerns = concerns, isLoading = loading, error = error,
                        onSelect = { selectedId = it.id })
                }
            }
        }
    }
}

@Composable
private fun ConcernDetailRoute(patientId: String, id: String, repository: HealthConcernRepository, onBack: () -> Unit) {
    var concern by remember { mutableStateOf<HealthConcern?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var appointmentsLoading by remember { mutableStateOf(true) }
    var appointmentsError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val alive = remember { mutableStateOf(true) }
    DisposableEffect(Unit) { alive.value = true; onDispose { alive.value = false } }
    DisposableEffect(patientId, id, repository, retry) {
        var active = true
        loading = true
        error = null
        appointmentsLoading = true
        appointmentsError = null
        val stopConcern = repository.watchConcern(patientId, id) { result ->
            if (active) {
                loading = false
                result.onSuccess { concern = it }.onFailure { error = "We couldn't load this health concern." }
            }
        }
        val stopAppointments = repository.watchAppointments(patientId) { result ->
            if (active) {
                appointmentsLoading = false
                result.onSuccess { appointments = it }.onFailure { appointmentsError = "We couldn't load appointments." }
            }
        }
        onDispose { active = false; stopConcern(); stopAppointments() }
    }
    fun update(operation: ((Result<Unit>) -> Unit) -> Unit) {
        if (saving) return
        saving = true
        saveError = null
        operation { result ->
            if (alive.value) {
                saving = false
                result.onFailure { saveError = "We couldn't save your changes. Please try again." }
            }
        }
    }
    HealthConcernDetailsScreen(concern, loading, error, appointments, appointmentsLoading, appointmentsError,
        saving, saveError,
        onStatusChange = { status -> update { repository.setStatus(patientId, id, status, it) } },
        onLinkAppointment = { appointmentId -> update { repository.linkAppointment(patientId, id, appointmentId, it) } },
        onRetry = { retry++ }, onBack = onBack)
}
