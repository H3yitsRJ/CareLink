package com.example.carelink.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.carelink.data.HealthConcernRepository
import com.example.carelink.model.*
import androidx.compose.ui.unit.dp
import navigation.BottomNavDestination

@Composable
fun HealthConcernsFlow(
    patientId: String,
    repository: HealthConcernRepository,
    onBack: () -> Unit = {},
    canEdit: Boolean = true
) {
    key(patientId) {
        var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
        var isAdding by rememberSaveable { mutableStateOf(false) }
        var successMessage by rememberSaveable { mutableStateOf<String?>(null) }

        LaunchedEffect(canEdit) {
            if (!canEdit) {
                isAdding = false
                successMessage = null
            }
        }

        BackHandler {
            when {
                isAdding -> isAdding = false
                selectedId != null -> selectedId = null
                else -> onBack()
            }
        }

        if (isAdding && canEdit) {
            AddHealthConcernScreen(
                patientId = patientId,
                repository = repository,
                onSaved = {
                    isAdding = false
                    successMessage = "Health concern saved successfully."
                },
                onCancel = { isAdding = false }
            )

        } else {
            val id = selectedId
            if (id != null) key(id) {
                ConcernDetailRoute(
                    patientId,
                    id,
                    repository,
                    onBack = { selectedId = null },
                    canEdit = canEdit
                )

            } else {
                var concerns by remember { mutableStateOf<List<HealthConcern>>(emptyList()) }
                var loading by remember { mutableStateOf(true) }
                var error by remember { mutableStateOf<String?>(null) }
                var retry by remember { mutableIntStateOf(0) }

                DisposableEffect(
                    patientId,
                    repository,
                    retry
                ) {
                    var active = true
                    loading = true
                    error = null
                    val stop = repository.watchList(patientId) { result ->
                        if (active) {
                            loading = false
                            result.onSuccess { concerns = it }
                                .onFailure { error = "We couldn't load your health concerns." }
                        }
                    }
                    onDispose { active = false; stop() }
                }
                Scaffold(Modifier.safeDrawingPadding(),
                    bottomBar = { TextButton(onClick = onBack) { Text("Back") } }
                ) { padding ->
                    Column(Modifier.padding(padding)) {
                        successMessage?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (error != null) TextButton(onClick = { retry++ }) { Text("Retry") }
                        HealthConcernsScreen(
                            concerns = concerns,
                            isLoading = loading,
                            error = error,
                            onAdd = if (canEdit) {
                                {
                                    successMessage = null
                                    isAdding = true
                                }
                            } else {
                                null
                            },
                            onSelect = { selectedId = it.id })
                    }
                }
            }
        }
    }
}

@Composable
private fun ConcernDetailRoute(
    patientId: String, id: String,
    repository: HealthConcernRepository,
    onBack: () -> Unit,
    canEdit: Boolean
) {
    val connectionAvailable = LocalConnectionAvailable.current
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
    val currentCanEdit by rememberUpdatedState(canEdit)

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
        if (!currentCanEdit) {
            saveError = "You have view-only access and cannot change this health concern."
            return
        }

        if (!connectionAvailable) {
            saveError = OFFLINE_ACTION_MESSAGE
            return
        }
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
        onRetry = { retry++ },
        onBack = onBack,
        canEdit = canEdit)
}

@Composable
fun CaregiverHealthConcernsRoute(
    patientId: String,
    actorId: String,
    repository: HealthConcernRepository,
    accessData: com.example.carelink.data.CaregiverMedicationData,
    onBack: () -> Unit
) {
    key(patientId, actorId, accessData) {
        var access by remember {
            mutableStateOf<CaregiverAccess?>(null)
        }
        var loading by remember { mutableStateOf(true) }
        var verified by remember { mutableStateOf(false) }
        var retry by remember { mutableIntStateOf(0) }

        DisposableEffect(patientId, actorId, accessData, retry) {
            var active = true
            loading = true
            verified = false
            access = null

            val stop = accessData.watchAccess(
                patientId,
                actorId
            ) { latest, _, online ->
                if (active) {
                    loading = false
                    verified = online

                    access = latest?.takeIf {
                        online &&
                                it.patientId == patientId &&
                                it.caregiverId == actorId
                    }
                }
            }

            onDispose {
                active = false
                stop()
            }
        }

        val canView =
            verified && access?.allows(CarePermission.VIEW) == true

        val canEdit =
            canView && access?.allows(CarePermission.EDIT) == true

        if (canView) {
            HealthConcernsFlow(
                patientId = patientId,
                repository = repository,
                onBack = onBack,
                canEdit = canEdit
            )
        } else {
            BackHandler(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Care recipient health concerns",
                    style = MaterialTheme.typography.headlineMedium
                )

                when {
                    loading -> LoadingState("Checking caregiver access...")

                    !verified -> {
                        Text(
                            "We couldn't verify your access. " +
                                    "Check your connection and retry."
                        )
                    }

                    else -> {
                        Text(
                            "You do not have access to this patient's " +
                                    "health concerns, or your access was revoked."
                        )
                    }
                }

                if (!loading) {
                    Button(onClick = { retry++ }) {
                        Text("Retry")
                    }
                }

                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
            }
        }
    }
}

@Composable
fun CaregiverAppointmentsRoute(
    patientId: String,
    actorId: String,
    repository: HealthConcernRepository,
    accessData: com.example.carelink.data.CaregiverMedicationData,
    onBack: () -> Unit,
    onNavigate: (BottomNavDestination) -> Unit
) {
    key(patientId, actorId, repository, accessData) {
        var access by remember {
            mutableStateOf<CaregiverAccess?>(null)
        }
        var verified by remember { mutableStateOf(false) }
        var checking by remember { mutableStateOf(true) }
        var retry by remember { mutableIntStateOf(0) }

        DisposableEffect(patientId, actorId, accessData, retry) {
            var active = true
            checking = true
            verified = false
            access = null

            val stop = accessData.watchAccess(
                patientId,
                actorId
            ) { latest, _, online ->
                if (active) {
                    checking = false
                    verified = online
                    access = latest?.takeIf {
                        online &&
                                it.patientId == patientId &&
                                it.caregiverId == actorId
                    }
                }
            }

            onDispose {
                active = false
                stop()
            }
        }

        val canView = verified && access?.allows(CarePermission.VIEW) == true

        if (!canView) {
            BackHandler(onBack = onBack)

            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Care recipient appointments",
                    style = MaterialTheme.typography.headlineMedium
                )

                if (checking) {
                    LoadingState("Checking caregiver access...")
                } else {
                    Text(
                        if (!verified) {
                            "We couldn't verify your access. Check your connection and retry."
                        } else {
                            "Appointment access is unavailable or has been revoked."
                        }
                    )
                }

                if (!checking) {
                    Button(onClick = { retry++ }) {
                        Text("Retry")
                    }
                }

                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
            }
        } else {
            var appointments by remember {
                mutableStateOf<List<Appointment>>(emptyList())
            }
            var selectedId by rememberSaveable {
                mutableStateOf<String?>(null)
            }
            var loading by remember { mutableStateOf(true) }
            var error by remember { mutableStateOf<String?>(null) }
            var reload by remember { mutableIntStateOf(0) }

            DisposableEffect(patientId, repository, reload) {
                var active = true
                loading = true
                error = null
                appointments = emptyList()

                val stop = repository.watchAppointments(patientId) { result ->
                    if (active) {
                        loading = false
                        result.onSuccess {
                            appointments = it
                            error = null
                        }.onFailure {
                            appointments = emptyList()
                            selectedId = null
                            error = "We couldn't load this patient's appointments."
                        }
                    }
                }

                onDispose {
                    active = false
                    stop()
                }
            }

            if (selectedId != null) {
                AppointmentDetailsScreen(
                    appointment = appointments.find { it.id == selectedId },
                    isLoading = loading,
                    canManage = false,
                    onBack = { selectedId = null }
                )
            } else {
                AppointmentsScreen(
                    appointments = appointments,
                    isLoading = loading,
                    errorMessage = error,
                    onAppointmentSelected = { selectedId = it.id },
                    onRetry = { reload++ },
                    onNavigate = onNavigate,
                    onBack = onBack,
                    canManage = false
                )
            }
        }
    }
}
