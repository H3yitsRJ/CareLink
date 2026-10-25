package com.example.carelink

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.carelink.data.CareHistoryStore
import com.example.carelink.data.ConnectivityMonitor
import com.example.carelink.data.canReachFirebase
import com.example.carelink.screens.LocalConnectionAvailable
import com.example.carelink.screens.OfflineBanner
import com.example.carelink.screens.OFFLINE_ACTION_MESSAGE
import com.google.firebase.firestore.Source
import kotlinx.coroutines.delay
import com.example.carelink.data.DoseHistoryStore
import com.example.carelink.data.FirestoreCareRecipientDirectory
import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus
import com.example.carelink.model.Medication
import com.example.carelink.model.CareActivityType
import com.example.carelink.model.CareHistoryEntry
import com.example.carelink.notifications.AndroidMedicationReminderScheduler
import com.example.carelink.notifications.NotificationPermissionManager
import com.example.carelink.screens.AddEditMedicationScreen
import com.example.carelink.screens.AddEditAppointmentScreen
import com.example.carelink.screens.AppointmentDetailsScreen
import com.example.carelink.screens.AppointmentsScreen
import com.example.carelink.screens.CareHistoryScreen
import com.example.carelink.screens.CareTasksScreen
import com.example.carelink.screens.HealthConcernsFlow
import com.example.carelink.data.FirestoreHealthConcernRepository
import com.example.carelink.screens.CareTasksFlow
import com.example.carelink.screens.CareRecipientSelector
import com.example.carelink.data.FirestoreCareTaskRepository
import com.example.carelink.data.MedicationCaregiverStore
import com.example.carelink.data.FirestoreRefillRequestRepository
import com.example.carelink.data.RefillRequestAlreadyActiveException
import com.example.carelink.model.RefillRequest
import com.example.carelink.notifications.MedicationReminderScheduler
import com.example.carelink.screens.CareErrorState
import com.example.carelink.screens.CreateAccountScreen
import com.example.carelink.screens.CreateProfileScreen
import com.example.carelink.screens.DashboardScreen
import com.example.carelink.screens.DashboardSummary
import com.example.carelink.screens.LoginScreen
import com.example.carelink.screens.LogoutScreen
import com.example.carelink.screens.MedicationDetailsScreen
import com.example.carelink.screens.MedicationsScreen
import com.example.carelink.screens.PasswordResetEmailScreen
import com.example.carelink.screens.PatientProfileDetails
import com.example.carelink.screens.ProfileScreen
import com.example.carelink.screens.SettingsScreen
import com.example.carelink.screens.CaregiverMedicationsScreen
import com.example.carelink.screens.MedicationCaregiverAccessScreen
import com.example.carelink.screens.MedicationScheduleScreen
import com.example.carelink.ui.theme.CareLinkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import navigation.BottomNavDestination
import com.example.carelink.notifications.AppointmentReminderScheduler
import com.example.carelink.repositories.AppointmentRepository
import com.example.carelink.screens.MedicationRefillsScreen
import com.example.carelink.screens.NotificationsScreen
import com.example.carelink.screens.RefillRequestScreen
import com.example.carelink.screens.RefillRequestTrackingScreen


import com.example.carelink.security.SecureStorageManager
import com.example.carelink.data.DoseHistoryItem
import com.example.carelink.model.CareTask
import com.example.carelink.model.ConcernSeverity
import com.example.carelink.model.ConcernStatus
import com.example.carelink.model.DashboardMedicationAttention
import com.example.carelink.model.HealthConcern

private enum class AuthScreen {
    SignIn,
    CreateAccount,
    ResetPassword
}

private enum class AppScreen {
    Home,
    Notifications,
    HealthConcerns,
    Medications,
    MedicationDetails,

    RefillRequestTracking,
    MedicationRefills,

    RefillRequest,
    CareHistory,
    Appointments,
    AppointmentDetails,
    AddAppointment,
    CareTasks,
    FollowUpTask,
    Profile,
    EditProfile,
    Settings,
    CaregiverMedications,
    MedicationCaregiverAccess,
    Logout,
    AddMedication,

    MedicationScheduler
}

class MainActivity : ComponentActivity() {
    private val medicationReminderScheduler by lazy { AndroidMedicationReminderScheduler(this) }

    private val appointmentReminderScheduler by lazy { AppointmentReminderScheduler(this) }

    override fun onResume() {
        super.onResume()
        medicationReminderScheduler.restore(FirebaseAuth.getInstance().currentUser?.uid)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureFirebaseEmulators()
        enableEdgeToEdge()

        setContent {
            android.util.Log.d(
                // Use "tag:CareLinkUser" in LogCat after opening caregiver features
                "CareLinkUser",
                "Signed-in UID: ${com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid}"
            )

            CareLinkTheme {
                val auth = remember { FirebaseAuth.getInstance() }
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { _: Boolean ->
                    // Permission result handled here
                }
                val firestore = remember { FirebaseFirestore.getInstance() }
                var networkAvailable by remember { mutableStateOf(false) }
                var firebaseAvailable by remember { mutableStateOf(false) }
                val readSource = remember {
                    { if (networkAvailable && firebaseAvailable) Source.DEFAULT else Source.CACHE }
                }
                var dashboardMedications by remember {
                    mutableStateOf<List<Medication>>(emptyList())
                }

                var dashboardMedicationsLoading by remember {
                    mutableStateOf(true)
                }

                var dashboardMedicationsError by remember {
                    mutableStateOf<String?>(null)
                }

                var dashboardDoseHistory by remember {
                    mutableStateOf<List<DoseHistoryItem>>(emptyList())
                }

                var dashboardDoseHistoryLoading by remember {
                    mutableStateOf(true)
                }

                var dashboardDoseHistoryError by remember {
                    mutableStateOf<String?>(null)
                }

                LaunchedEffect(auth.currentUser?.uid) {
                    val patientId = auth.currentUser?.uid

                    dashboardDoseHistory = emptyList()
                    dashboardDoseHistoryError = null
                    dashboardDoseHistoryLoading = true

                    if (patientId == null) {
                        dashboardDoseHistoryError = "Sign in to view dose history."
                        dashboardDoseHistoryLoading = false
                    } else {
                        DoseHistoryStore(firestore, readSource).load(patientId) { result ->
                            result.onSuccess { items ->
                                dashboardDoseHistory = items
                                dashboardDoseHistoryError = null

                                android.util.Log.d(
                                    "CareLinkDashboard",
                                    "Dose history loaded: ${items.size} records"
                                )
                            }

                            result.onFailure {
                                dashboardDoseHistoryError =
                                    "Unable to load dose history."
                            }

                            dashboardDoseHistoryLoading = false
                        }
                    }
                }

                DisposableEffect(auth.currentUser?.uid) {
                    val patientId = auth.currentUser?.uid

                    dashboardMedications = emptyList()
                    dashboardMedicationsError = null
                    dashboardMedicationsLoading = true

                    val listener = if (patientId != null) {
                        firestore.collection("users")
                            .document(patientId)
                            .collection("medications")
                            .addSnapshotListener { snapshot, exception ->

                                if (exception != null) {
                                    dashboardMedicationsError =
                                        "Unable to load medications."
                                    dashboardMedicationsLoading = false
                                } else if (snapshot != null) {
                                    dashboardMedications = snapshot.documents.mapNotNull { document ->
                                        Medication.fromFirestore(
                                            document.id,
                                            document.data.orEmpty() +
                                                    ("patientId" to patientId)
                                        )
                                    }.filter { it.active }

                                    dashboardMedicationsError = null
                                    dashboardMedicationsLoading = false
                                }
                            }
                    } else {
                        dashboardMedicationsError = "Sign in to view medications."
                        dashboardMedicationsLoading = false
                        null
                    }

                    onDispose {
                        listener?.remove()
                    }
                }

                val refillRequestRepository = remember {
                    FirestoreRefillRequestRepository(firestore, readSource)
                }
                var refillRequests by remember {
                    mutableStateOf<List<RefillRequest>>(emptyList())
                }
                var refillLoading by remember {
                    mutableStateOf(false)
                }
                var refillError by remember {
                    mutableStateOf<String?>(null)
                }

                fun loadRefillRequests(patientId: String) {
                    if (patientId.isBlank()) return

                    refillLoading = true
                    refillError = null

                    refillRequestRepository.load(patientId) { result ->
                        refillLoading = false

                        result.onSuccess { requests ->
                            refillRequests = requests
                        }

                        result.onFailure {
                            refillError = "Unable to load refill requests. Check your connection and try again."
                        }
                    }
                }

                var unreadNotificationCount by remember { mutableIntStateOf(0) }
                val notificationUserId = auth.currentUser?.uid

                DisposableEffect(notificationUserId, firestore) {
                    if (notificationUserId == null) {
                        unreadNotificationCount = 0
                        onDispose { }
                    } else {
                        val listener = firestore
                            .collection("users")
                            .document(notificationUserId)
                            .collection("notifications")
                            .addSnapshotListener { snapshot, error ->
                                if (error == null && snapshot != null) {
                                    unreadNotificationCount = snapshot.documents.count {
                                        it.getBoolean("isRead") == false
                                    }
                                } else {
                                    unreadNotificationCount = 0
                                }
                            }

                        onDispose {
                            listener.remove()
                        }
                    }
                }

                val appointmentRepository = remember { AppointmentRepository(firestore, readSource) }
                val doseHistoryStore = remember { DoseHistoryStore(firestore, readSource) }
                val careHistoryStore = remember { CareHistoryStore(firestore, readSource) }
                var isSavingDose by remember { mutableStateOf(false) }
                var doseMessage by remember { mutableStateOf<String?>(null) }
                var doseError by remember { mutableStateOf<String?>(null) }
                val recipientDirectory = remember(firestore) {
                    FirestoreCareRecipientDirectory(
                        firestore
                    )
                }
                val caregiverMedicationData = remember(firestore) {
                    MedicationCaregiverStore(
                        firestore
                    )
                }
                var selectedCareRecipient by rememberSaveable(auth.currentUser?.uid) { mutableStateOf<String?>(null) }
                val healthConcernRepository = remember(firestore) { FirestoreHealthConcernRepository(firestore) }
                val careTaskRepository = remember(firestore) { FirestoreCareTaskRepository(firestore, readSource) }
                var dashboardCareTasks by remember {
                    mutableStateOf<List<CareTask>>(emptyList())
                }

                var dashboardCareTasksLoading by remember {
                    mutableStateOf(true)
                }

                var dashboardCareTasksError by remember {
                    mutableStateOf<String?>(null)
                }
                var dashboardHealthConcerns by remember {
                    mutableStateOf<List<HealthConcern>>(emptyList())
                }

                DisposableEffect(auth.currentUser?.uid) {
                    val patientId = auth.currentUser?.uid

                    val stopListening: () -> Unit = if (patientId != null) {
                        healthConcernRepository.watchList(patientId) { result ->
                            result.onSuccess { concerns ->
                                dashboardHealthConcerns = concerns
                            }
                            result.onFailure {
                                dashboardHealthConcerns = emptyList()
                            }
                        }
                    } else {
                        dashboardHealthConcerns = emptyList()
                        ({})
                    }

                    onDispose {
                        stopListening()
                    }
                }

                var isAuthenticated by remember { mutableStateOf(auth.currentUser != null) }
                val connectionAvailable = networkAvailable && (!isAuthenticated || firebaseAvailable)

                DisposableEffect(Unit) {
                    val monitor = ConnectivityMonitor(this@MainActivity) { available ->
                        runOnUiThread { networkAvailable = available }
                    }
                    monitor.start()
                    onDispose { monitor.stop() }
                }

                LaunchedEffect(networkAvailable, isAuthenticated, auth.currentUser?.uid) {
                    firebaseAvailable = false
                    val patientId = auth.currentUser?.uid
                    if (networkAvailable && isAuthenticated && patientId != null) {
                        while (true) {
                            firebaseAvailable = canReachFirebase(firestore.collection("users").document(patientId))
                            delay(15_000)
                        }
                    }
                }

                var screen by remember { mutableStateOf(AuthScreen.SignIn) }
                var isSubmitting by remember { mutableStateOf(false) }
                var submitError by remember { mutableStateOf<String?>(null) }
                var requestSucceeded by remember { mutableStateOf(false) }
                var hasProfile by remember { mutableStateOf<Boolean?>(null) }
                var fullName by remember { mutableStateOf("") }
                var firstName by remember { mutableStateOf("") }
                var lastName by remember { mutableStateOf("") }
                var preferredName by remember { mutableStateOf("") }
                var dateOfBirth by remember { mutableStateOf("") }
                var phoneNumber by remember { mutableStateOf("") }
                var addressLine1 by remember { mutableStateOf("") }
                var addressLine2 by remember { mutableStateOf("") }
                var city by remember { mutableStateOf("") }
                var state by remember { mutableStateOf("") }
                var zipCode by remember { mutableStateOf("") }
                var appScreen by remember { mutableStateOf(AppScreen.Home) }
                var isSavingMedication by remember { mutableStateOf(false) }
                var medicationSaveError by remember { mutableStateOf<String?>(null) }
                var selectedMedication by remember { mutableStateOf< com.example.carelink.model.Medication? >(null) }
                var medicationOpenedFromHistory by remember { mutableStateOf(false) }
                var medicationSuccessMessage by remember { mutableStateOf<String?>(null) }
                var medicationPermissionExplanation by remember { mutableStateOf<Medication?>(null) }
                val notificationPreferences = remember {
                    getSharedPreferences("notification_permission", MODE_PRIVATE)
                }
                var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
                var appointmentsLoading by remember { mutableStateOf(false) }
                var appointmentLoadError by remember { mutableStateOf<String?>(null) }
                var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
                var isSavingAppointment by remember { mutableStateOf(false) }
                var appointmentSaveError by remember { mutableStateOf<String?>(null) }
                var appointmentSuccessMessage by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(auth.currentUser?.uid, appScreen, connectionAvailable) {
                    val patientId = auth.currentUser?.uid

                    if (patientId != null && appScreen == AppScreen.Home) {
                        dashboardCareTasksLoading = true
                        dashboardCareTasksError = null

                        careTaskRepository.list(patientId) { result ->
                            result.onSuccess { tasks ->
                                dashboardCareTasks = tasks
                                dashboardCareTasksLoading = false
                            }

                            result.onFailure { exception ->
                                dashboardCareTasksError =
                                    exception.message ?: "Unable to load care tasks."
                                dashboardCareTasksLoading = false
                            }
                        }
                    } else {
                        dashboardCareTasks = emptyList()
                        dashboardCareTasksLoading = false
                        dashboardCareTasksError = null
                    }
                }

                DisposableEffect(isAuthenticated, auth.currentUser?.uid) {
                    val patientId = auth.currentUser?.uid
                    if (!isAuthenticated || patientId == null) return@DisposableEffect onDispose { }
                    var active = true
                    val listener = firestore.collection("users").document(patientId).collection("medications")
                        .addSnapshotListener { snapshot, _ ->
                            if (active && auth.currentUser?.uid == patientId) snapshot?.documentChanges?.forEach { change ->
                                com.example.carelink.model.Medication.fromFirestore(change.document.id, change.document.data)
                                    ?.let { medication ->
                                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) medicationReminderScheduler.cancel(medication)
                                        else medicationReminderScheduler.schedule(medication)
                                    }
                            }
                        }
                    onDispose { active = false; listener.remove() }
                }

                fun loadAppointments() {
                    val user = auth.currentUser

                    if (user == null) {
                        appointments = emptyList()
                        appointmentsLoading = false
                        appointmentLoadError = null
                        return
                    }

                    appointmentsLoading = true
                    appointmentLoadError = null

                    appointmentRepository.getAppointments(
                        userId = user.uid,
                        onSuccess = { loadedAppointments ->
                            appointments = loadedAppointments

                            loadedAppointments.forEach { appointment ->
                                if(appointment.status == AppointmentStatus.SCHEDULED) {
                                    appointmentReminderScheduler.schedule(appointment)
                                } else {
                                    appointmentReminderScheduler.cancel(appointment)
                                }
                            }

                            appointmentsLoading = false
                        },
                        onError = { error->
                            appointmentsLoading = false
                            appointmentLoadError = error
                        }
                    )

                }

                LaunchedEffect(
                    isAuthenticated,
                    auth.currentUser?.uid,
                    connectionAvailable
                ) {
                    val user = auth.currentUser

                    if (!isAuthenticated || user == null) {
                        hasProfile = null
                        fullName = ""
                        appointments = emptyList()
                        selectedAppointment = null
                        appointmentLoadError = null
                        appointmentSuccessMessage = null
                    } else {
                        medicationReminderScheduler.restore(user.uid)
                        loadAppointments()
                        firestore
                            .collection("users")
                            .document(user.uid)
                            .get(if (connectionAvailable) Source.DEFAULT else Source.CACHE)
                            .addOnSuccessListener { document ->
                                // An empty cache cannot establish that a patient needs a new profile.
                                if (!document.exists() && !connectionAvailable) {
                                    submitError = "Connect to the internet to load your profile."
                                    return@addOnSuccessListener
                                }
                                hasProfile = document.exists()
                                submitError = null
                                fullName = document.getString("fullName").orEmpty()
                                firstName = document.getString("firstName").orEmpty()
                                lastName = document.getString("lastName").orEmpty()
                                preferredName = document.getString("preferredName").orEmpty()
                                dateOfBirth = document.getString("dateOfBirth").orEmpty()
                                phoneNumber = document.getString("phoneNumber").orEmpty()
                                addressLine1 = document.getString("addressLine1").orEmpty()
                                addressLine2 = document.getString("addressLine2").orEmpty()
                                city = document.getString("city").orEmpty()
                                state = document.getString("state").orEmpty()
                                zipCode = document.getString("zipCode").orEmpty()
                            }
                            .addOnFailureListener {
                                submitError =
                                    "We couldn't load your profile."
                            }
                    }
                }

                fun navigate(destination: AuthScreen) {
                    screen = destination
                    isSubmitting = false
                    submitError = null
                    requestSucceeded = false
                }

                CompositionLocalProvider(LocalConnectionAvailable provides connectionAvailable) {

                    Column(Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                    ) {
                        OfflineBanner(!connectionAvailable)

                        Box(Modifier.weight(1f)) {

                            when {
                                isAuthenticated && hasProfile == null -> {
                                    if (submitError != null) {
                                        Column(Modifier.safeDrawingPadding()) {
                                            CareErrorState(message = submitError!!)
                                            TextButton(
                                                onClick = {
                                                    medicationReminderScheduler.clear();
                                                    auth.signOut();
                                                    isAuthenticated = false
                                                }
                                            ) {
                                                Text("Back to sign in")
                                            }
                                        }
                                    } else LoadingScreen()
                                }

                                isAuthenticated && hasProfile == false -> {
                                    CreateProfileScreen(
                                        isSaving = isSubmitting,
                                        saveError = submitError,
                                        onSaveProfile = { profile ->
                                            if (!connectionAvailable) {
                                                submitError = OFFLINE_ACTION_MESSAGE
                                                return@CreateProfileScreen
                                            }
                                            val user =
                                                auth.currentUser
                                                    ?: return@CreateProfileScreen

                                            isSubmitting = true
                                            submitError = null

                                            val displayName = listOf(
                                                profile.firstName,
                                                profile.lastName
                                            )
                                                .filter(String::isNotBlank)
                                                .joinToString(" ")

                                            firestore
                                                .collection("users")
                                                .document(user.uid)
                                                .set(
                                                    mapOf(
                                                        "fullName" to displayName,
                                                        "firstName" to profile.firstName,
                                                        "lastName" to profile.lastName,
                                                        "preferredName" to profile.preferredName,
                                                        "dateOfBirth" to profile.dateOfBirth,
                                                        "phoneNumber" to profile.phoneNumber,
                                                        "addressLine1" to profile.addressLine1,
                                                        "addressLine2" to profile.addressLine2,
                                                        "city" to profile.city,
                                                        "state" to profile.state,
                                                        "zipCode" to profile.zipCode,
                                                        "email" to user.email.orEmpty()
                                                    )
                                                )
                                                .addOnSuccessListener {
                                                    isSubmitting = false
                                                    fullName = displayName
                                                    hasProfile = true
                                                }
                                                .addOnFailureListener {
                                                    isSubmitting = false
                                                    submitError =
                                                        "We couldn't save your profile."
                                                }
                                        }
                                    )
                                }

                                isAuthenticated && hasProfile == true -> {
                                    fun openTopLevel(
                                        destination: BottomNavDestination
                                    ) {
                                        medicationOpenedFromHistory = false

                                        appScreen = when (destination) {
                                            BottomNavDestination.Home -> AppScreen.Home
                                            BottomNavDestination.Medications -> AppScreen.Medications
                                            BottomNavDestination.Appointments -> AppScreen.Appointments
                                            BottomNavDestination.CareTasks -> AppScreen.CareTasks
                                            BottomNavDestination.Profile -> AppScreen.Profile
                                        }
                                    }


                                    when (appScreen) {
                                        AppScreen.RefillRequest -> {
                                            var medicationsLoading by remember { mutableStateOf(true) }
                                            var medicationLoadError by remember { mutableStateOf<String?>(null) }
                                            var medicationRetry by remember { mutableStateOf(0) }
                                            var submittingRefill by remember { mutableStateOf(false) }
                                            var submittedRefill by remember { mutableStateOf(false) }
                                            var refillSaveError by remember { mutableStateOf<String?>(null) }

                                            var refillMedications by remember {
                                                mutableStateOf<List<Medication>>(emptyList())
                                            }
                                            LaunchedEffect(auth.currentUser?.uid, connectionAvailable, medicationRetry) {
                                                medicationsLoading = true
                                                medicationLoadError = null
                                                val user = auth.currentUser

                                                if (user != null) {
                                                    FirebaseFirestore.getInstance()
                                                        .collection("users")
                                                        .document(user.uid)
                                                        .collection("medications")
                                                        .get(readSource())
                                                        .addOnSuccessListener { result ->
                                                            refillMedications = result.documents.mapNotNull { document ->
                                                                Medication.fromFirestore(
                                                                    document.id,
                                                                    document.data.orEmpty() + ("patientId" to user.uid)
                                                                )
                                                            }
                                                            medicationsLoading = false
                                                        }
                                                        .addOnFailureListener {
                                                            medicationsLoading = false
                                                            medicationLoadError = "We couldn't load your medications. Check your connection and try again."
                                                        }
                                                } else {
                                                    medicationsLoading = false
                                                    medicationLoadError = "Sign in to view your medications."
                                                }
                                            }

                                            RefillRequestScreen(
                                                eligibleMedications = refillMedications,
                                                patientId = auth.currentUser?.uid.orEmpty(),
                                                requestedById = auth.currentUser?.uid.orEmpty(),
                                                activeRequests = refillRequests,
                                                isSaving = submittingRefill,
                                                saved = submittedRefill,
                                                saveError = refillSaveError,
                                                medicationsLoading = medicationsLoading,
                                                medicationLoadError = medicationLoadError,
                                                onRetryMedications = { medicationRetry++ },
                                                onOpenMedications = { appScreen = AppScreen.Medications },
                                                onCancel = { appScreen = if (submittedRefill) AppScreen.RefillRequestTracking else AppScreen.MedicationRefills },
                                                onSave = { request ->
                                                    if (!connectionAvailable || submittingRefill || submittedRefill) return@RefillRequestScreen
                                                    submittingRefill = true
                                                    refillSaveError = null
                                                    refillRequestRepository.create(request) { result ->
                                                        submittingRefill = false
                                                        result.onSuccess {
                                                            submittedRefill = true
                                                        }

                                                        result.onFailure { error ->
                                                            refillSaveError = if (generateSequence(error) { it.cause }.any { it is RefillRequestAlreadyActiveException })
                                                                "A refill request is already active for this medication."
                                                            else "Couldn't submit the refill request. Check your connection and try again."
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        AppScreen.MedicationRefills -> {
                                            MedicationRefillsScreen(
                                                onRequestRefill = {
                                                    appScreen = AppScreen.RefillRequest
                                                },
                                                onTrackRefills = {
                                                    appScreen = AppScreen.RefillRequestTracking
                                                },
                                                onBack = { appScreen = AppScreen.Medications }
                                            )
                                        }
                                        AppScreen.RefillRequestTracking -> {
                                            val patientId = auth.currentUser?.uid.orEmpty()
                                            var trackingMedications by remember {
                                                mutableStateOf<List<Medication>>(emptyList())
                                            }
                                            LaunchedEffect(patientId, connectionAvailable) {
                                                if (patientId.isNotBlank()) {
                                                    FirebaseFirestore.getInstance()
                                                        .collection("users")
                                                        .document(patientId)
                                                        .collection("medications")
                                                        .get(readSource())
                                                        .addOnSuccessListener { result ->
                                                            trackingMedications = result.documents.mapNotNull { document ->
                                                                Medication.fromFirestore(
                                                                    document.id,
                                                                    document.data.orEmpty() + ("patientId" to patientId)
                                                                )
                                                            }
                                                        }
                                                }
                                            }

                                            LaunchedEffect(patientId, connectionAvailable) {
                                                if (patientId.isNotBlank()) {
                                                    loadRefillRequests(patientId)
                                                }
                                            }

                                            RefillRequestTrackingScreen(
                                                requests = refillRequests,
                                                medicationNames = trackingMedications.associate {
                                                    it.id to it.name
                                                },
                                                isLoading = refillLoading,
                                                error = refillError,
                                                onRefresh = {
                                                    loadRefillRequests(patientId)
                                                },
                                                onBack = { appScreen = AppScreen.MedicationRefills }
                                            )
                                        }
                                        AppScreen.Notifications -> {
                                            NotificationsScreen(onBack = { appScreen = AppScreen.Home })
                                        }
                                        AppScreen.Home -> {

                                            val overdueMedicationCount =
                                                if (
                                                    !dashboardMedicationsLoading &&
                                                    !dashboardDoseHistoryLoading &&
                                                    dashboardMedicationsError == null &&
                                                    dashboardDoseHistoryError == null
                                                ) {
                                                    DashboardMedicationAttention.overdueCount(
                                                        medications = dashboardMedications,
                                                        doseRecords = dashboardDoseHistory.map { it.record }
                                                    )
                                                } else {
                                                    null
                                                }
                                            android.util.Log.d(
                                                "CareLinkDashboard",
                                                "Overdue medication count: $overdueMedicationCount"
                                            )

                                            val highSeverityConcernCount = dashboardHealthConcerns.count { concern ->
                                                concern.severity == ConcernSeverity.HIGH &&
                                                        concern.status == ConcernStatus.ACTIVE
                                            }

                                            val overdueCareTaskCount =
                                                if (
                                                    !dashboardCareTasksLoading &&
                                                    dashboardCareTasksError == null
                                                ) {
                                                    val now = System.currentTimeMillis()

                                                    dashboardCareTasks.count { task ->
                                                        val deadline = if (
                                                            task.dueDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
                                                        ) {
                                                            val time = task.time.ifBlank { "23:59" }
                                                            val formatter = java.text.SimpleDateFormat(
                                                                "yyyy-MM-dd HH:mm",
                                                                java.util.Locale.US
                                                            ).apply {
                                                                isLenient = false
                                                            }

                                                            val parsed = formatter.parse(
                                                                "${task.dueDate} $time",
                                                                java.text.ParsePosition(0)
                                                            )

                                                            parsed?.time?.let {
                                                                if (task.time.isBlank()) it + 59_999L else it
                                                            }
                                                        } else {
                                                            null
                                                        }

                                                        !task.completed &&
                                                                deadline != null &&
                                                                now > deadline
                                                    }
                                                } else {
                                                    null
                                                }
                                            DashboardScreen(
                                                fullName = fullName,
                                                unreadNotificationCount = unreadNotificationCount,
                                                summary = DashboardSummary(
                                                    medication = when {
                                                        dashboardMedicationsLoading -> "Loading medications..."
                                                        dashboardMedicationsError != null -> dashboardMedicationsError!!
                                                        dashboardMedications.isEmpty() -> ""
                                                        else -> {
                                                            val activeCount = dashboardMedications.size
                                                            val overdueCount = overdueMedicationCount ?: 0

                                                            if (overdueCount > 0) {
                                                                "$activeCount active medication(s)\n" +
                                                                        "Attention: $overdueCount dose(s) may be overdue"
                                                            } else {
                                                                "$activeCount active medication(s)"
                                                            }
                                                        }
                                                    },
                                                    appointment = "View upcoming appointments",
                                                    healthConcern = if (highSeverityConcernCount > 0) {
                                                        "Review active health concerns\n" +
                                                                "Attention: $highSeverityConcernCount high-severity health concern(s)"
                                                    } else {
                                                        "Review active health concerns"
                                                    },
                                                    careTask = when {
                                                        dashboardCareTasksLoading -> "Loading care tasks..."

                                                        dashboardCareTasksError != null ->
                                                            dashboardCareTasksError!!

                                                        overdueCareTaskCount != null && overdueCareTaskCount > 0 ->
                                                            "Check open care tasks\n" +
                                                                    "Attention: $overdueCareTaskCount care task(s) overdue"

                                                        else -> "Check open care tasks"
                                                    }
                                                ),
                                                onOpen = { destination ->
                                                    appScreen = when (destination) {
                                                        "medications" -> AppScreen.Medications
                                                        "add-medication" -> AppScreen.AddMedication
                                                        "appointments" -> AppScreen.Appointments
                                                        "health-concerns" -> AppScreen.HealthConcerns
                                                        "care-tasks" -> AppScreen.CareTasks
                                                        "notifications" -> AppScreen.Notifications
                                                        else -> AppScreen.Home
                                                    }
                                                },
                                                onNavigate = ::openTopLevel
                                            )
                                        }

                                        AppScreen.HealthConcerns -> {
                                            val actorId = auth.currentUser?.uid

                                            if (actorId != null) {
                                                var caregiverMode by rememberSaveable(actorId) {
                                                    mutableStateOf(selectedCareRecipient != null)
                                                }

                                                if (caregiverMode) {
                                                    CareRecipientSelector(
                                                        actorId = actorId,
                                                        selectedPatientId = selectedCareRecipient,
                                                        onSelect = {
                                                            selectedCareRecipient = it
                                                        },
                                                        directory = recipientDirectory,
                                                        medicationData = caregiverMedicationData,
                                                        onBack = {
                                                            caregiverMode = false
                                                        },
                                                        content = { patientId ->
                                                            com.example.carelink.screens.CaregiverHealthConcernsRoute(
                                                                patientId = patientId,
                                                                actorId = actorId,
                                                                repository = healthConcernRepository,
                                                                accessData = caregiverMedicationData,
                                                                onBack = {
                                                                    caregiverMode = false
                                                                }
                                                            )
                                                        }
                                                    )
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .safeDrawingPadding()
                                                    ) {
                                                        TextButton(
                                                            onClick = {
                                                                caregiverMode = true
                                                            }
                                                        ) {
                                                            Text("Care recipient health concerns")
                                                        }

                                                        androidx.compose.foundation.layout.Box(
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            HealthConcernsFlow(
                                                                patientId = actorId,
                                                                repository = healthConcernRepository,
                                                                onBack = {
                                                                    appScreen = AppScreen.Home
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        AppScreen.Medications -> {
                                            MedicationsScreen(
                                                onCaregiverMedications = {
                                                    appScreen = AppScreen.CaregiverMedications },
                                                onOpenCareHistory = {
                                                    appScreen = AppScreen.CareHistory
                                                },
                                                onOpenRefills = {
                                                    appScreen = AppScreen.MedicationRefills
                                                },
                                                onAddMedication = {
                                                    selectedMedication = null
                                                    medicationSuccessMessage = null
                                                    doseMessage = null
                                                    doseError = null
                                                    appScreen = AppScreen.AddMedication
                                                },
                                                onMedicationSelected = {
                                                        medication ->
                                                    selectedMedication = medication
                                                    medicationOpenedFromHistory = false
                                                    medicationSuccessMessage = null
                                                    appScreen = AppScreen.MedicationDetails
                                                },
                                                onMedicationScheduler = {
                                                    appScreen = AppScreen.MedicationScheduler
                                                },
                                                successMessage = medicationSuccessMessage,
                                                onNavigate = ::openTopLevel
                                            )
                                        }
                                        AppScreen.MedicationDetails -> {
                                            MedicationDetailsScreen(
                                                medication = selectedMedication,
                                                errorMessage = medicationSaveError,
                                                isSavingDose = isSavingDose,
                                                doseMessage = doseMessage,
                                                doseError = doseError,
                                                onRecordDose = { medication, scheduledTime, status ->
                                                    doseMessage = null
                                                    if (!connectionAvailable) {
                                                        doseError = OFFLINE_ACTION_MESSAGE
                                                        return@MedicationDetailsScreen
                                                    }
                                                    val user = auth.currentUser

                                                    if (user == null) {
                                                        doseMessage = null
                                                        doseError = "Sign in to record a dose."
                                                    } else if (!isSavingDose) {
                                                        isSavingDose = true
                                                        doseMessage = null
                                                        doseError = null

                                                        doseHistoryStore.record(
                                                            patientId = user.uid,
                                                            medication = medication,
                                                            scheduledTimeMillis = scheduledTime,
                                                            status = status,
                                                            onComplete = { result ->
                                                                isSavingDose = false

                                                                result.fold(
                                                                    onSuccess = {
                                                                        val statusLabel = status.name.lowercase()
                                                                        doseMessage = "Dose recorded as $statusLabel."
                                                                        doseError = null
                                                                    },
                                                                    onFailure = {
                                                                        doseMessage = null
                                                                        doseError =
                                                                            "We couldn't record the dose. Please try again."
                                                                    }
                                                                )
                                                            }
                                                        )
                                                    }
                                                },
                                                onEdit = { medication ->
                                                    selectedMedication = medication
                                                    medicationSaveError = null
                                                    appScreen = AppScreen.AddMedication
                                                },
                                                onRemove = { medication ->
                                                    if (!connectionAvailable) {
                                                        medicationSaveError = OFFLINE_ACTION_MESSAGE
                                                        return@MedicationDetailsScreen
                                                    }
                                                    val user = auth.currentUser

                                                    if (user != null) {
                                                        firestore
                                                            .collection("users")
                                                            .document(user.uid)
                                                            .collection("medications")
                                                            .document(medication.id)
                                                            .delete()
                                                            .addOnSuccessListener {
                                                                medicationReminderScheduler.cancel(medication)
                                                                selectedMedication = null
                                                                medicationSaveError = null
                                                                medicationSuccessMessage = "Medication removed successfully."
                                                                appScreen = AppScreen.Medications
                                                            }
                                                            .addOnFailureListener {
                                                                medicationSaveError = "We couldn't remove the medication. Please try again."
                                                            }
                                                    }
                                                },
                                                onBack = {
                                                    selectedMedication = null
                                                    medicationSuccessMessage = null

                                                    appScreen = if (medicationOpenedFromHistory) {
                                                        AppScreen.CareHistory
                                                    } else {
                                                        AppScreen.Medications
                                                    }

                                                    medicationOpenedFromHistory = false
                                                },
                                                onNavigate = ::openTopLevel
                                            )
                                        }
                                        AppScreen.CareHistory -> {
                                            val patientId = auth.currentUser?.uid.orEmpty()

                                            var openingMedicationId by remember(patientId) {
                                                mutableStateOf<String?>(null)
                                            }
                                            var medicationLinkError by remember(patientId) {
                                                mutableStateOf<String?>(null)
                                            }
                                            var historyRetry by remember(patientId) {
                                                mutableIntStateOf(0)
                                            }

                                            val historyPager = remember(patientId, historyRetry) {
                                                if (patientId.isNotBlank()) {
                                                    com.example.carelink.data.CareHistoryPager(
                                                        firestore = firestore,
                                                        patientId = patientId
                                                    )
                                                } else {
                                                    null
                                                }
                                            }

                                            var historyEntries by remember(historyPager) {
                                                mutableStateOf<List<CareHistoryEntry>>(emptyList())
                                            }
                                            var historyLoading by remember(historyPager) {
                                                mutableStateOf(true)
                                            }
                                            var historyError by remember(historyPager) {
                                                mutableStateOf<String?>(null)
                                            }
                                            var historyHasMore by remember(historyPager) {
                                                mutableStateOf(false)
                                            }
                                            var historyLoadingMore by remember(historyPager) {
                                                mutableStateOf(false)
                                            }
                                            var historyMoreError by remember(historyPager) {
                                                mutableStateOf<String?>(null)
                                            }
                                            var historyPageRequest by remember(historyPager) {
                                                mutableIntStateOf(0)
                                            }

                                            DisposableEffect(historyPager, historyPageRequest) {
                                                var active = true
                                                val firstPage = historyEntries.isEmpty()

                                                if (
                                                    historyPager == null ||
                                                    auth.currentUser?.uid != patientId
                                                ) {
                                                    historyEntries = emptyList()
                                                    historyLoading = false
                                                    historyLoadingMore = false
                                                    historyHasMore = false
                                                    historyError = "Sign in to view your care history."
                                                } else {
                                                    historyLoading = firstPage
                                                    historyLoadingMore = !firstPage
                                                    historyError = null
                                                    historyMoreError = null

                                                    historyPager.loadNext { result ->
                                                        if (active && auth.currentUser?.uid == patientId) {
                                                            historyLoading = false
                                                            historyLoadingMore = false

                                                            result.onSuccess { page ->
                                                                historyEntries = CareHistoryEntry.newestFirst(
                                                                    (historyEntries + page.entries)
                                                                        .distinctBy { it.id }
                                                                )

                                                                historyHasMore = page.hasMore
                                                            }.onFailure { exception ->
                                                                android.util.Log.e(
                                                                    "CareHistoryPaging",
                                                                    "Failed to load care history",
                                                                    exception
                                                                )

                                                                if (firstPage) {
                                                                    historyError =
                                                                        "We couldn't load your care history. Please try again."
                                                                } else {
                                                                    historyMoreError =
                                                                        "We couldn't load older history. Your loaded events are still available."
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                onDispose {
                                                    active = false
                                                }
                                            }

                                            DisposableEffect(patientId, openingMedicationId) {
                                                val medicationId = openingMedicationId
                                                var active = true

                                                if (medicationId != null) {
                                                    if (
                                                        patientId.isBlank() ||
                                                        auth.currentUser?.uid != patientId
                                                    ) {
                                                        medicationLinkError =
                                                            "Sign in to open this medication."

                                                        openingMedicationId = null
                                                    } else {
                                                        firestore.collection("users")
                                                            .document(patientId)
                                                            .collection("medications")
                                                            .document(medicationId)
                                                            .get(com.google.firebase.firestore.Source.SERVER)
                                                            .addOnSuccessListener { document ->
                                                                if (active && auth.currentUser?.uid == patientId) {
                                                                    val medication = Medication.fromFirestore(
                                                                        document.id,
                                                                        document.data.orEmpty()
                                                                    )

                                                                    openingMedicationId = null

                                                                    if (
                                                                        medication == null ||
                                                                        medication.patientId != patientId
                                                                    ) {
                                                                        medicationLinkError =
                                                                            "This medication is no longer available."
                                                                    } else {
                                                                        selectedMedication = medication
                                                                        medicationSaveError = null
                                                                        medicationSuccessMessage = null
                                                                        doseMessage = null
                                                                        doseError = null
                                                                        medicationOpenedFromHistory = true
                                                                        appScreen = AppScreen.MedicationDetails
                                                                    }
                                                                }
                                                            }
                                                            .addOnFailureListener {
                                                                if (active && auth.currentUser?.uid == patientId) {
                                                                    openingMedicationId = null

                                                                    medicationLinkError =
                                                                        "We couldn't open this medication. Check your connection and try again."
                                                                }
                                                            }
                                                    }
                                                }

                                                onDispose {
                                                    active = false
                                                }
                                            }

                                            CareHistoryScreen(
                                                entries = historyEntries,
                                                isLoading = historyLoading,
                                                loadError = historyError,
                                                onRetry = {
                                                    historyRetry++
                                                },
                                                onBack = {
                                                    appScreen = AppScreen.Medications
                                                },
                                                onOpenMedication = { entry ->
                                                    val medicationId = entry.relatedRecordId

                                                    if (
                                                        entry.patientId == patientId &&
                                                        entry.type ==
                                                        com.example.carelink.model.CareActivityType.MEDICATION &&
                                                        !medicationId.isNullOrBlank() &&
                                                        '/' !in medicationId &&
                                                        medicationId != "." &&
                                                        medicationId != ".." &&
                                                        openingMedicationId == null
                                                    ) {
                                                        medicationLinkError = null
                                                        openingMedicationId = medicationId
                                                    }
                                                },
                                                onOpenCaregiverAccess = { entry ->
                                                    if (
                                                        entry.patientId == patientId &&
                                                        auth.currentUser?.uid == patientId &&
                                                        entry.type ==
                                                        com.example.carelink.model.CareActivityType.CAREGIVER_ACCESS &&
                                                        !entry.relatedRecordId.isNullOrBlank()
                                                    ) {
                                                        appScreen = AppScreen.MedicationCaregiverAccess
                                                    }
                                                },
                                                hasMore = historyHasMore,
                                                isLoadingMore = historyLoadingMore,
                                                loadMoreError = historyMoreError,
                                                onLoadMore = {
                                                    if (
                                                        !historyLoading &&
                                                        !historyLoadingMore &&
                                                        historyHasMore
                                                    ) {
                                                        historyPageRequest++
                                                    }
                                                }
                                            )

                                            if (medicationLinkError != null) {
                                                androidx.compose.material3.AlertDialog(
                                                    onDismissRequest = {
                                                        medicationLinkError = null
                                                    },
                                                    title = {
                                                        Text("Medication unavailable")
                                                    },
                                                    text = {
                                                        CareErrorState(message = medicationLinkError.orEmpty())
                                                    },
                                                    confirmButton = {
                                                        TextButton(
                                                            onClick = {
                                                                medicationLinkError = null
                                                            }
                                                        ) {
                                                            Text("OK")
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        AppScreen.AddMedication -> {
                                            AddEditMedicationScreen(
                                                medication = selectedMedication,
                                                isSaving = isSavingMedication,
                                                saveError = medicationSaveError,
                                                onSave = { medication ->
                                                    if (!connectionAvailable) {
                                                        medicationSaveError = OFFLINE_ACTION_MESSAGE
                                                        return@AddEditMedicationScreen
                                                    }
                                                    val user = auth.currentUser

                                                    if (user != null) {
                                                        isSavingMedication = true
                                                        medicationSaveError = null

                                                        val medicationData =
                                                            hashMapOf(
                                                                "id" to medication.id,
                                                                "patientId" to user.uid,
                                                                "name" to medication.name,
                                                                "strength" to medication.strength,
                                                                "dose" to medication.dose,
                                                                "frequency" to medication.frequency,
                                                                "reminderTimes" to medication.reminderTimes,
                                                                "instructions" to medication.instructions,
                                                                "active" to medication.active,
                                                                "updatedById" to user.uid,
                                                                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                                            )

                                                        val historyRef = firestore
                                                            .collection("users")
                                                            .document(user.uid)
                                                            .collection("careHistory")
                                                            .document()

                                                        val historyEntry = CareHistoryEntry(
                                                            id = historyRef.id,
                                                            patientId = user.uid,
                                                            occurredAtMillis = System.currentTimeMillis(),
                                                            type = CareActivityType.MEDICATION,
                                                            summary = if (selectedMedication == null) {
                                                                "Medication added: ${medication.name}"
                                                            } else {
                                                                "Medication updated: ${medication.name}"
                                                            },
                                                            actorId = user.uid,
                                                            relatedRecordId = medication.id
                                                        )


                                                        val medicationRef = firestore.collection("users")
                                                            .document(user.uid).collection("medications").document(medication.id)
                                                        firestore.batch()
                                                            .set(medicationRef, medicationData)
                                                            .set(historyRef, historyEntry.toFirestore())
                                                            .commit()
                                                            .addOnSuccessListener {
                                                                val saved = medication.copy(patientId = user.uid, updatedById = user.uid)
                                                                val remindersScheduled = selectedMedication?.let {
                                                                    medicationReminderScheduler.replace(it, saved)
                                                                } ?: medicationReminderScheduler.schedule(saved)
                                                                if (
                                                                    !remindersScheduled &&
                                                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                                    !NotificationPermissionManager.canPostNotifications(this@MainActivity) &&
                                                                    !notificationPreferences.getBoolean("explanationShown", false)
                                                                ) {
                                                                    notificationPreferences.edit().putBoolean("explanationShown", true).apply()
                                                                    medicationPermissionExplanation = saved
                                                                }

                                                                isSavingMedication = false
                                                                selectedMedication = null
                                                                medicationSuccessMessage = if (remindersScheduled) "Medication saved successfully."
                                                                else "Medication saved. Enable CareLink notifications in Android Settings to receive reminders."
                                                                appScreen = AppScreen.Medications
                                                            }
                                                            .addOnFailureListener {
                                                                isSavingMedication = false
                                                                medicationSaveError = "We couldn't save the medication."
                                                            }
                                                    }
                                                },
                                                onCancel = {
                                                    medicationSaveError = null

                                                    appScreen =
                                                        if (
                                                            selectedMedication != null
                                                        ) {
                                                            AppScreen.MedicationDetails
                                                        } else {
                                                            AppScreen.Medications
                                                        }
                                                },
                                                onDelete = { medication ->
                                                    if (!connectionAvailable) {
                                                        medicationSaveError = OFFLINE_ACTION_MESSAGE
                                                        return@AddEditMedicationScreen
                                                    }
                                                    val user = auth.currentUser

                                                    if (user != null) {
                                                        firestore
                                                            .collection("users")
                                                            .document(user.uid)
                                                            .collection("medications")
                                                            .document(medication.id)
                                                            .delete()
                                                            .addOnSuccessListener {
                                                                medicationReminderScheduler.cancel(medication)
                                                                selectedMedication = null
                                                                medicationSaveError = null
                                                                medicationSuccessMessage = "Medication removed successfully."
                                                                appScreen = AppScreen.Medications
                                                            }
                                                            .addOnFailureListener {
                                                                medicationSaveError = "We couldn't remove the medication. Please try again."
                                                            }
                                                    }
                                                }
                                            )
                                        }
                                        AppScreen.MedicationScheduler -> {
                                            MedicationScheduleScreen(
                                                onBack = { appScreen = AppScreen.Medications },
                                                onNavigate = ::openTopLevel,
                                                onEditMedication = {
                                                    selectedMedication = it
                                                    medicationSaveError = null
                                                    appScreen = AppScreen.AddMedication
                                                },
                                                onAddMedication = {
                                                    selectedMedication = null
                                                    medicationSaveError = null
                                                    appScreen = AppScreen.AddMedication
                                                }
                                            )
                                        }
                                        AppScreen.Appointments -> {
                                            val actorId = auth.currentUser?.uid

                                            if (actorId != null) {
                                                var caregiverMode by rememberSaveable(actorId) {
                                                    mutableStateOf(selectedCareRecipient != null)
                                                }

                                                if (caregiverMode) {
                                                    CareRecipientSelector(
                                                        actorId = actorId,
                                                        selectedPatientId = selectedCareRecipient,
                                                        onSelect = {
                                                            selectedCareRecipient = it
                                                        },
                                                        directory = recipientDirectory,
                                                        medicationData = caregiverMedicationData,
                                                        onBack = {
                                                            caregiverMode = false
                                                        },
                                                        content = { patientId ->
                                                            com.example.carelink.screens.CaregiverAppointmentsRoute(
                                                                patientId = patientId,
                                                                actorId = actorId,
                                                                repository = healthConcernRepository,
                                                                accessData = caregiverMedicationData,
                                                                onBack = {
                                                                    caregiverMode = false
                                                                },
                                                                onNavigate = { destination ->
                                                                    if (destination != BottomNavDestination.Appointments) {
                                                                        openTopLevel(destination)
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    )
                                                } else {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .safeDrawingPadding()
                                                    ) {
                                                        TextButton(
                                                            onClick = {
                                                                caregiverMode = true
                                                            }
                                                        ) {
                                                            Text("Care recipient appointments")
                                                        }

                                                        androidx.compose.foundation.layout.Box(
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            AppointmentsScreen(
                                                                appointments = appointments,
                                                                isLoading = appointmentsLoading,
                                                                errorMessage = appointmentLoadError,
                                                                successMessage = appointmentSuccessMessage,
                                                                onAddAppointment = {
                                                                    selectedAppointment = null
                                                                    appointmentSaveError = null
                                                                    appointmentSuccessMessage = null
                                                                    appScreen = AppScreen.AddAppointment
                                                                },
                                                                onAppointmentSelected = { appointment ->
                                                                    selectedAppointment = appointment
                                                                    appointmentSaveError = null
                                                                    appointmentSuccessMessage = null
                                                                    appScreen = AppScreen.AppointmentDetails
                                                                },
                                                                onEditAppointment = { appointment ->
                                                                    selectedAppointment = appointment
                                                                    appointmentSaveError = null
                                                                    appointmentSuccessMessage = null
                                                                    appScreen = AppScreen.AddAppointment
                                                                },
                                                                onRetry = {
                                                                    loadAppointments()
                                                                },
                                                                onNavigate = ::openTopLevel,
                                                                canManage = true
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        AppScreen.AppointmentDetails -> {
                                            AppointmentDetailsScreen(
                                                appointment = selectedAppointment,
                                                successMessage = appointmentSuccessMessage,
                                                onEdit = {
                                                    appointmentSaveError = null
                                                    appScreen = AppScreen.AddAppointment
                                                },
                                                onGenerateFollowUp = { appScreen = AppScreen.FollowUpTask },
                                                onCancelAppointment = { appointment ->
                                                    if (!connectionAvailable) {
                                                        appointmentSuccessMessage = OFFLINE_ACTION_MESSAGE
                                                        return@AppointmentDetailsScreen
                                                    }
                                                    val user = auth.currentUser

                                                    if (user != null) {
                                                        appointmentRepository.cancelAppointment(
                                                            userId = user.uid,
                                                            appointment = appointment,
                                                            onSuccess = {
                                                                val cancelledAppointment = appointment.copy(
                                                                    status = AppointmentStatus.CANCELLED
                                                                )

                                                                appointmentReminderScheduler.cancel(
                                                                    cancelledAppointment
                                                                )

                                                                selectedAppointment = cancelledAppointment

                                                                appointmentSuccessMessage = "Appointment cancelled successfully."

                                                                loadAppointments()
                                                            },
                                                            onError = {
                                                                appointmentSuccessMessage = "Sorry, we couldn't cancel the appointment. Please try again later."
                                                            }
                                                        )
                                                    }
                                                },
                                                onBack = {
                                                    selectedAppointment = null
                                                    appointmentSuccessMessage = null
                                                    appScreen = AppScreen.Appointments
                                                }
                                            )
                                        }
                                        AppScreen.AddAppointment -> {
                                            AddEditAppointmentScreen(
                                                appointment = selectedAppointment,
                                                isSaving = isSavingAppointment,
                                                saveError = appointmentSaveError,
                                                onSave = { appointment ->
                                                    if (!connectionAvailable) {
                                                        appointmentSaveError = OFFLINE_ACTION_MESSAGE
                                                        return@AddEditAppointmentScreen
                                                    }
                                                    val user = auth.currentUser

                                                    if (user != null) {
                                                        isSavingAppointment = true
                                                        appointmentSaveError = null

                                                        appointmentRepository.saveAppointment(
                                                            userId = user.uid,
                                                            appointment = appointment,
                                                            onSuccess = {
                                                                appointmentReminderScheduler.schedule(appointment)

                                                                isSavingAppointment = false
                                                                selectedAppointment = null
                                                                appointmentSaveError = null

                                                                appointmentSuccessMessage = "Appointment saved successfully."

                                                                loadAppointments()

                                                                appScreen = AppScreen.Appointments
                                                            },
                                                            onError = {
                                                                isSavingAppointment = false
                                                                appointmentSaveError = "Could not save appointment. Please try again."
                                                            }
                                                        )
                                                    } else {
                                                        appointmentSaveError =
                                                            "You must be signed in to save an appointment."
                                                    }
                                                },
                                                onCancel = {
                                                    appointmentSaveError = null

                                                    appScreen =
                                                        if (selectedAppointment != null) {
                                                            AppScreen.AppointmentDetails
                                                        } else {
                                                            AppScreen.Appointments
                                                        }
                                                }
                                            )
                                        }
                                        AppScreen.CareTasks,
                                        AppScreen.FollowUpTask -> {
                                            val patientId = auth.currentUser?.uid
                                            if (patientId != null) {
                                                CareTasksFlow(
                                                    patientId = patientId,
                                                    repository = careTaskRepository,
                                                    sourceAppointment = if (appScreen == AppScreen.FollowUpTask) selectedAppointment else null,
                                                    onCancelFollowUp = { appScreen = AppScreen.AppointmentDetails },
                                                    onNavigate = ::openTopLevel
                                                )
                                            }
                                        }
                                        AppScreen.Profile -> {
                                            ProfileScreen(
                                                fullName = fullName,
                                                email = auth.currentUser
                                                    ?.email
                                                    .orEmpty(),
                                                onEditProfile = {
                                                    appScreen =
                                                        AppScreen.EditProfile
                                                },
                                                onOpenSettings = {
                                                    appScreen =
                                                        AppScreen.Settings
                                                },
                                                onNavigate = ::openTopLevel
                                            )
                                        }
                                        AppScreen.EditProfile -> {
                                            CreateProfileScreen(
                                                isSaving = isSubmitting,
                                                saveError = submitError,
                                                initialProfile =
                                                    PatientProfileDetails(
                                                        firstName = firstName,
                                                        lastName = lastName,
                                                        preferredName = preferredName,
                                                        dateOfBirth = dateOfBirth,
                                                        phoneNumber = phoneNumber,
                                                        addressLine1 = addressLine1,
                                                        addressLine2 = addressLine2,
                                                        city = city,
                                                        state = state,
                                                        zipCode = zipCode
                                                    ),
                                                onSaveProfile = { profile ->
                                                    if (!connectionAvailable) {
                                                        submitError = OFFLINE_ACTION_MESSAGE
                                                        return@CreateProfileScreen
                                                    }
                                                    val user =
                                                        auth.currentUser
                                                            ?: return@CreateProfileScreen

                                                    val displayName = listOf(
                                                        profile.firstName,
                                                        profile.lastName
                                                    )
                                                        .filter { it.isNotBlank() }
                                                        .joinToString(" ")

                                                    isSubmitting = true
                                                    submitError = null
                                                    firestore
                                                        .collection("users")
                                                        .document(user.uid)
                                                        .update(
                                                            mapOf(
                                                                "fullName" to displayName,
                                                                "firstName" to profile.firstName,
                                                                "lastName" to profile.lastName,
                                                                "preferredName" to profile.preferredName,
                                                                "dateOfBirth" to profile.dateOfBirth,
                                                                "phoneNumber" to profile.phoneNumber,
                                                                "addressLine1" to profile.addressLine1,
                                                                "addressLine2" to profile.addressLine2,
                                                                "city" to profile.city,
                                                                "state" to profile.state,
                                                                "zipCode" to profile.zipCode
                                                            )
                                                        )
                                                        .addOnSuccessListener {
                                                            isSubmitting = false
                                                            fullName = displayName
                                                            firstName = profile.firstName
                                                            lastName = profile.lastName
                                                            preferredName = profile.preferredName
                                                            dateOfBirth = profile.dateOfBirth
                                                            phoneNumber = profile.phoneNumber
                                                            addressLine1 = profile.addressLine1
                                                            addressLine2 = profile.addressLine2
                                                            city = profile.city
                                                            state = profile.state
                                                            zipCode = profile.zipCode

                                                            appScreen = AppScreen.Profile
                                                        }
                                                        .addOnFailureListener {
                                                            isSubmitting = false
                                                            submitError = "We couldn't update your profile."
                                                        }
                                                },
                                                onCancel = {
                                                    appScreen = AppScreen.Profile
                                                }
                                            )
                                        }
                                        AppScreen.Settings -> {
                                            SettingsScreen(
                                                onCaregiverAccess = { appScreen = AppScreen.MedicationCaregiverAccess },
                                                onBack = {
                                                    appScreen = AppScreen.Profile
                                                },
                                                onOpenLogout = {
                                                    appScreen = AppScreen.Logout
                                                }
                                            )
                                        }
                                        AppScreen.CaregiverMedications -> CaregiverMedicationsScreen(
                                            actorId = auth.currentUser!!.uid, onBack = { appScreen = AppScreen.Medications })
                                        AppScreen.MedicationCaregiverAccess -> MedicationCaregiverAccessScreen(
                                            patientId = auth.currentUser!!.uid, patientName = fullName,
                                            onBack = { appScreen = AppScreen.Settings })
                                        AppScreen.Logout -> {
                                            var logoutError by remember { mutableStateOf<String?>(null) }
                                            LogoutScreen(
                                                errorMessage = logoutError,
                                                onBack = {
                                                    appScreen = AppScreen.Settings
                                                },

                                                onLogout = {
                                                    logoutError = null
                                                    val secureStorage = SecureStorageManager(
                                                        this@MainActivity
                                                    )

                                                    if (secureStorage.clearAll()) {
                                                        medicationReminderScheduler.clear()
                                                        auth.signOut()
                                                        appScreen = AppScreen.Home
                                                        screen = AuthScreen.SignIn
                                                        isAuthenticated = false
                                                    } else {
                                                        logoutError = secureStorage.lastError + " You're still signed in. Tap Log out to try again."
                                                        android.util.Log.e(
                                                            "CareLinkLogout",
                                                            "Secure storage cleanup failed; logout cancelled"
                                                        )
                                                    }
                                                }

                                            )
                                        }
                                    }
                                }

                                else -> {
                                    when (screen) {
                                        AuthScreen.SignIn -> {
                                            LoginScreen(
                                                isSubmitting = isSubmitting,
                                                submitError = submitError,
                                                onSignIn = { details ->
                                                    if (!connectionAvailable) {
                                                        submitError = "Connect to the internet to sign in."
                                                        return@LoginScreen
                                                    }
                                                    isSubmitting = true
                                                    submitError = null

                                                    auth
                                                        .signInWithEmailAndPassword(
                                                            details.email,
                                                            details.password
                                                        )
                                                        .addOnSuccessListener {
                                                            isSubmitting = false
                                                            hasProfile = null
                                                            isAuthenticated = true
                                                        }
                                                        .addOnFailureListener {
                                                            isSubmitting = false
                                                            submitError = "We couldn't sign you in. Check your email and password."
                                                        }
                                                },
                                                onForgotPassword = {
                                                    navigate(
                                                        AuthScreen.ResetPassword
                                                    )
                                                },
                                                onCreateAccount = {
                                                    navigate(
                                                        AuthScreen.CreateAccount
                                                    )
                                                }
                                            )
                                        }
                                        AuthScreen.CreateAccount -> {
                                            CreateAccountScreen(
                                                isSubmitting = isSubmitting,
                                                submitError = submitError,
                                                accountCreated = requestSucceeded,
                                                onCreateAccount = { details ->
                                                    if (!connectionAvailable) {
                                                        submitError = "Connect to the internet to create an account."
                                                        return@CreateAccountScreen
                                                    }
                                                    isSubmitting = true
                                                    submitError = null

                                                    auth
                                                        .createUserWithEmailAndPassword(
                                                            details.email,
                                                            details.password
                                                        )
                                                        .addOnSuccessListener {
                                                            isSubmitting = false
                                                            requestSucceeded = true
                                                            isAuthenticated = true
                                                        }
                                                        .addOnFailureListener {
                                                            isSubmitting = false
                                                            submitError =
                                                                "We couldn't create your account. Check your details and connection, then try again."
                                                        }
                                                },
                                                onSignIn = {
                                                    navigate(AuthScreen.SignIn)
                                                }
                                            )
                                        }
                                        AuthScreen.ResetPassword -> {
                                            PasswordResetEmailScreen(
                                                isSubmitting = isSubmitting,
                                                submitError = submitError,
                                                emailSent = requestSucceeded,
                                                onSendResetEmail = { email ->
                                                    if (!connectionAvailable) {
                                                        submitError = "Connect to the internet to reset your password."
                                                        return@PasswordResetEmailScreen
                                                    }
                                                    isSubmitting = true
                                                    submitError = null

                                                    auth
                                                        .sendPasswordResetEmail(email)
                                                        .addOnSuccessListener {
                                                            isSubmitting = false
                                                            requestSucceeded = true
                                                        }
                                                        .addOnFailureListener {
                                                                exception ->
                                                            isSubmitting = false

                                                            if (
                                                                exception is
                                                                        FirebaseAuthInvalidUserException
                                                            ) {
                                                                requestSucceeded = true
                                                            } else {
                                                                submitError = "We couldn't send the reset link. Check your connection and try again."
                                                            }
                                                        }
                                                },
                                                onBackToSignIn = {
                                                    navigate(AuthScreen.SignIn)
                                                }
                                            )
                                        }

                                    }
                                }

                            }


                        }
                    }
                }

                if (medicationPermissionExplanation != null) {
                    AlertDialog(
                        onDismissRequest = {
                            medicationPermissionExplanation = null
                        },
                        title = {
                            Text("Enable medication reminders?")
                        },
                        text = {
                            Text(
                                "CareLink needs notification permission to alert you when it is time to take your medication. You can continue using CareLink without reminders."
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    medicationPermissionExplanation = null
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }
                            ) {
                                Text("Continue")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    medicationPermissionExplanation = null
                                }
                            ) {
                                Text("Not now")
                            }
                        }
                    )
                }



            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
