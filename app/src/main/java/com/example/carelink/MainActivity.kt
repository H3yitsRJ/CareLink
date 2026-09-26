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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.carelink.data.DoseHistoryStore
import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus
import com.example.carelink.model.DoseStatus
import com.example.carelink.model.Medication
import com.example.carelink.notifications.AndroidMedicationReminderScheduler
import com.example.carelink.notifications.NotificationPermissionManager
import com.example.carelink.screens.AddEditMedicationScreen
import com.example.carelink.screens.AddEditAppointmentScreen
import com.example.carelink.screens.AppointmentDetailsScreen
import com.example.carelink.screens.AppointmentsScreen
import com.example.carelink.screens.CareHistoryScreen
import com.example.carelink.screens.CareTasksScreen
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

private enum class AuthScreen {
    SignIn,
    CreateAccount,
    ResetPassword
}

private enum class AppScreen {
    Home,
    Medications,
    MedicationDetails,
    CareHistory,
    Appointments,
    AppointmentDetails,
    AddAppointment,
    CareTasks,
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

    override fun onResume() {
        super.onResume()
        medicationReminderScheduler.restore(FirebaseAuth.getInstance().currentUser?.uid)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureFirebaseEmulators()
        enableEdgeToEdge()

        setContent {
            CareLinkTheme {
                val auth = remember { FirebaseAuth.getInstance() }
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { medicationReminderScheduler.restore(auth.currentUser?.uid) }

                val firestore = remember { FirebaseFirestore.getInstance() }
                val doseHistoryStore = remember { DoseHistoryStore(firestore) }
                var isSavingDose by remember { mutableStateOf(false) }
                var doseMessage by remember { mutableStateOf<String?>(null) }
                var doseError by remember { mutableStateOf<String?>(null) }
                var isAuthenticated by remember { mutableStateOf(auth.currentUser != null) }
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
                var medicationSuccessMessage by remember { mutableStateOf<String?>(null) }
                var medicationPermissionExplanation by remember { mutableStateOf<Medication?>(null) }
                var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
                var appointmentsLoading by remember { mutableStateOf(false) }
                var appointmentLoadError by remember { mutableStateOf<String?>(null) }
                var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
                var isSavingAppointment by remember { mutableStateOf(false) }
                var appointmentSaveError by remember { mutableStateOf<String?>(null) }
                var appointmentSuccessMessage by remember { mutableStateOf<String?>(null) }

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

                    firestore
                        .collection("users")
                        .document(user.uid)
                        .collection("appointments")
                        .get()
                        .addOnSuccessListener { snapshot ->
                            appointments = snapshot.documents.map { document ->
                                Appointment.fromFirestore(
                                    id = document.id,
                                    data = document.data.orEmpty()
                                )
                            }

                            appointmentsLoading = false
                        }
                        .addOnFailureListener {
                            appointmentsLoading = false
                            appointmentLoadError =
                                "We couldn't load your appointments. Please try again."
                        }
                }

                LaunchedEffect(
                    isAuthenticated,
                    auth.currentUser?.uid
                ) {
                    val user = auth.currentUser

                    if (!isAuthenticated || user == null) {
                        hasProfile = false
                        fullName = ""
                        appointments = emptyList()
                        selectedAppointment = null
                        appointmentLoadError = null
                        appointmentSuccessMessage = null
                    } else {
                        medicationReminderScheduler.restore(user.uid)
                        loadAppointments()
                        hasProfile = null

                        firestore
                            .collection("users")
                            .document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                hasProfile = document.exists()
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
                                hasProfile = false
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

                when {
                    isAuthenticated && hasProfile == null -> {
                        LoadingScreen()
                    }

                    isAuthenticated && hasProfile == false -> {
                        CreateProfileScreen(
                            isSaving = isSubmitting,
                            saveError = submitError,
                            onSaveProfile = { profile ->
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
                            appScreen = when (destination) {
                                BottomNavDestination.Home -> AppScreen.Home
                                BottomNavDestination.Medications -> AppScreen.Medications
                                BottomNavDestination.Appointments -> AppScreen.Appointments
                                BottomNavDestination.CareTasks -> AppScreen.CareTasks
                                BottomNavDestination.Profile -> AppScreen.Profile
                            }
                        }

                        when (appScreen) {

                            AppScreen.Home -> {
                                DashboardScreen(
                                    fullName = fullName,
                                    summary = DashboardSummary(
                                        medication = "Review today's medication schedule",
                                        appointment = "View upcoming appointments",
                                        healthConcern = "Review active health concerns",
                                        careTask = "Check open care tasks"
                                    ),
                                    onOpen = { destination ->
                                        appScreen = when (destination) {
                                            "medications" -> AppScreen.Medications
                                            "appointments" -> AppScreen.Appointments
                                            "care-tasks" -> AppScreen.CareTasks
                                            else -> AppScreen.Home
                                        }
                                    },
                                    onNavigate = ::openTopLevel
                                )
                            }

                            AppScreen.Medications -> {
                                MedicationsScreen(
                                    onCaregiverMedications = { appScreen = AppScreen.CaregiverMedications },
                                    onAddMedication = {
                                        selectedMedication = null
                                        medicationSuccessMessage = null
                                        appScreen = AppScreen.AddMedication
                                    },
                                    onMedicationSelected = {
                                        medication ->
                                            selectedMedication = medication
                                            medicationSuccessMessage = null
                                            appScreen = AppScreen.MedicationDetails
                                    },
                                    onOpenCareHistory = {
                                        appScreen = AppScreen.CareHistory
                                    },
                                    onMedicationScheduler = {
                                        appScreen = AppScreen.MedicationScheduler
                                    },
                                    successMessage =
                                        medicationSuccessMessage,
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
                                    onRecordDose = { medication, scheduledTimeMillis, status ->
                                        val user = auth.currentUser
                                        if (user == null) {
                                            doseError = "Sign in to record a dose."
                                        } else {
                                            isSavingDose = true
                                            doseError = null
                                            doseMessage = null
                                            doseHistoryStore.record(
                                                patientId = user.uid,
                                                medication = medication,
                                                scheduledTimeMillis = scheduledTimeMillis,
                                                status = status
                                            ) { result ->
                                                isSavingDose = false
                                                result.onSuccess {
                                                    doseMessage = "Dose recorded as ${status.name.lowercase()}."
                                                }.onFailure {
                                                    doseError = "We couldn't save this dose. Please try again."
                                                }
                                            }
                                        }
                                    },
                                    onEdit = { medication ->
                                        selectedMedication = medication
                                        medicationSaveError = null
                                        appScreen = AppScreen.AddMedication
                                    },
                                    onRemove = { medication ->
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
                                        appScreen = AppScreen.Medications
                                    },
                                    onNavigate = ::openTopLevel
                                )
                            }

                            AppScreen.AddMedication -> {
                                AddEditMedicationScreen(
                                    medication = selectedMedication,
                                    isSaving = isSavingMedication,
                                    saveError = medicationSaveError,
                                    onSave = { medication ->
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

                                            firestore
                                                .collection("users")
                                                .document(user.uid)
                                                .collection("medications")
                                                .document(medication.id)
                                                .set(medicationData)
                                                .addOnSuccessListener {
                                                    val saved = medication.copy(patientId = user.uid, updatedById = user.uid)
                                                    val remindersScheduled = selectedMedication?.let {
                                                        medicationReminderScheduler.replace(it, saved)
                                                    } ?: medicationReminderScheduler.schedule(saved)
                                                    if (
                                                        !remindersScheduled &&
                                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                        !NotificationPermissionManager.canPostNotifications(this)
                                                    ) {
                                                        medicationPermissionExplanation = saved
                                                    }

                                                    isSavingMedication = false
                                                    selectedMedication = null
                                                    medicationSuccessMessage = if (remindersScheduled) "Medication saved successfully."
                                                        else "Medication saved. Enable notifications in Settings to receive reminders."
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
                                MedicationScheduleScreen()
                            }

                            AppScreen.Appointments -> {
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
                                    onNavigate = ::openTopLevel
                                )
                            }

                            AppScreen.AppointmentDetails -> {
                                AppointmentDetailsScreen(
                                    appointment = selectedAppointment,
                                    successMessage = appointmentSuccessMessage,
                                    onEdit = {
                                        appointmentSaveError = null
                                        appScreen = AppScreen.AddAppointment
                                    },
                                    onCancelAppointment = { appointment ->
                                        val user = auth.currentUser

                                        if (user != null) {
                                            val cancelledAppointment = appointment.copy(
                                                status = AppointmentStatus.CANCELLED
                                            )

                                            firestore
                                                .collection("users")
                                                .document(user.uid)
                                                .collection("appointments")
                                                .document(appointment.id)
                                                .set(cancelledAppointment.toFirestore())
                                                .addOnSuccessListener {
                                                    selectedAppointment = cancelledAppointment
                                                    appointmentSuccessMessage =
                                                        "Appointment cancelled successfully."
                                                    loadAppointments()
                                                }
                                                .addOnFailureListener {
                                                    appointmentSuccessMessage =
                                                        "We couldn't cancel the appointment."
                                                }
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
                                        val user = auth.currentUser

                                        if (user != null) {
                                            isSavingAppointment = true
                                            appointmentSaveError = null

                                            val appointmentCollection = firestore
                                                .collection("users")
                                                .document(user.uid)
                                                .collection("appointments")

                                            val appointmentId = appointment.id.ifBlank {
                                                appointmentCollection.document().id
                                            }

                                            val appointmentToSave = appointment.copy(
                                                id = appointmentId,
                                                patientId = user.uid
                                            )

                                            appointmentCollection
                                                .document(appointmentId)
                                                .set(appointmentToSave.toFirestore())
                                                .addOnSuccessListener {
                                                    isSavingAppointment = false
                                                    selectedAppointment = null
                                                    appointmentSaveError = null
                                                    appointmentSuccessMessage =
                                                        "Appointment saved successfully."

                                                    loadAppointments()
                                                    appScreen = AppScreen.Appointments
                                                }
                                                .addOnFailureListener {
                                                    isSavingAppointment = false
                                                    appointmentSaveError =
                                                        "We couldn't save the appointment. Please try again."
                                                }
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

                            AppScreen.CareTasks -> {
                                CareTasksScreen(
                                    onNavigate = ::openTopLevel
                                )
                            }

                            AppScreen.CareHistory -> {
                                CareHistoryScreen(
                                    onBack = {
                                        appScreen = AppScreen.Medications
                                    },
                                    onNavigate = ::openTopLevel
                                )
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
                                        val user =
                                            auth.currentUser
                                                ?: return@CreateProfileScreen

                                        val displayName = listOf(
                                            profile.firstName,
                                            profile.lastName
                                        )
                                            .filter { it.isNotBlank() }
                                            .joinToString(" ")

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
                                LogoutScreen(
                                    onBack = {
                                        appScreen = AppScreen.Settings
                                    },
                                    onLogout = {
                                        medicationReminderScheduler.clear()
                                        auth.signOut()
                                        appScreen = AppScreen.Home
                                        screen = AuthScreen.SignIn
                                        isAuthenticated = false
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
                                        isSubmitting = true
                                        submitError = null

                                        auth
                                            .signInWithEmailAndPassword(
                                                details.email,
                                                details.password
                                            )
                                            .addOnSuccessListener {
                                                isSubmitting = false
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
                                                    exception ->
                                                isSubmitting = false
                                                submitError =
                                                    exception
                                                        .localizedMessage
                                                        ?: "Unable to create account."
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
