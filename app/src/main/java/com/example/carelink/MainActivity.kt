package com.example.carelink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.carelink.screens.CreateAccountScreen
import com.example.carelink.screens.CreateProfileScreen
import com.example.carelink.screens.AppointmentsScreen
import com.example.carelink.screens.CareTasksScreen
import com.example.carelink.screens.DashboardScreen
import com.example.carelink.screens.DashboardSummary
import com.example.carelink.screens.LoginScreen
import com.example.carelink.screens.LogoutScreen
import com.example.carelink.screens.MedicationsScreen
import com.example.carelink.screens.PasswordResetEmailScreen
import com.example.carelink.screens.ProfileScreen
import com.example.carelink.screens.SettingsScreen
import com.example.carelink.ui.theme.CareLinkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import navigation.BottomNavDestination

// The authentication flow is small enough to model locally without adding a navigation library.
private enum class AuthScreen { SignIn, CreateAccount, ResetPassword }
private enum class AppScreen { Home, Medications, Appointments, CareTasks, Profile, Settings, Logout }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Debug and release builds provide different implementations of this function.
        configureFirebaseEmulators()
        enableEdgeToEdge()

        setContent {
            CareLinkTheme {
                val auth = remember { FirebaseAuth.getInstance() }
                val firestore = remember { FirebaseFirestore.getInstance() }
                var isAuthenticated by remember { mutableStateOf(auth.currentUser != null) }
                var screen by remember { mutableStateOf(AuthScreen.SignIn) }
                var isSubmitting by remember { mutableStateOf(false) }
                var submitError by remember { mutableStateOf<String?>(null) }
                var requestSucceeded by remember { mutableStateOf(false) }
                var hasProfile by remember { mutableStateOf<Boolean?>(null) }
                var fullName by remember { mutableStateOf("") }
                var appScreen by remember { mutableStateOf(AppScreen.Home) }

                // The remote branch added profile gating. Reload it whenever authentication changes.
                LaunchedEffect(isAuthenticated, auth.currentUser?.uid) {
                    val user = auth.currentUser
                    if (!isAuthenticated || user == null) {
                        hasProfile = false
                        fullName = ""
                    } else {
                        hasProfile = null
                        firestore.collection("users").document(user.uid).get()
                            .addOnSuccessListener { document ->
                                hasProfile = document.exists()
                                fullName = document.getString("fullName").orEmpty()
                            }
                            .addOnFailureListener {
                                hasProfile = false
                                submitError = "We couldn't load your profile."
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
                    isAuthenticated && hasProfile == null -> LoadingScreen()
                    isAuthenticated && hasProfile == false -> CreateProfileScreen(
                        isSaving = isSubmitting,
                        saveError = submitError,
                        onSaveProfile = { profile ->
                            val user = auth.currentUser ?: return@CreateProfileScreen
                            isSubmitting = true
                            submitError = null
                            val displayName = listOf(profile.firstName, profile.lastName).filter(String::isNotBlank).joinToString(" ")
                            firestore.collection("users").document(user.uid).set(
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
                            ).addOnSuccessListener {
                                isSubmitting = false
                                fullName = displayName
                                hasProfile = true
                            }.addOnFailureListener {
                                isSubmitting = false
                                submitError = "We couldn't save your profile."
                            }
                        }
                    )
                    isAuthenticated && hasProfile == true -> {
                        fun openTopLevel(destination: BottomNavDestination) {
                            appScreen = when (destination) {
                                BottomNavDestination.Home -> AppScreen.Home
                                BottomNavDestination.Medications -> AppScreen.Medications
                                BottomNavDestination.Appointments -> AppScreen.Appointments
                                BottomNavDestination.CareTasks -> AppScreen.CareTasks
                                BottomNavDestination.Profile -> AppScreen.Profile
                            }
                        }

                        when (appScreen) {
                            AppScreen.Home -> DashboardScreen(
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
                            AppScreen.Medications -> MedicationsScreen(onNavigate = ::openTopLevel)
                            AppScreen.Appointments -> AppointmentsScreen(onNavigate = ::openTopLevel)
                            AppScreen.CareTasks -> CareTasksScreen(onNavigate = ::openTopLevel)
                            AppScreen.Profile -> ProfileScreen(
                                fullName = fullName,
                                email = auth.currentUser?.email.orEmpty(),
                                onOpenSettings = { appScreen = AppScreen.Settings },
                                onNavigate = ::openTopLevel
                            )
                            AppScreen.Settings -> SettingsScreen(
                                onBack = { appScreen = AppScreen.Profile },
                                onOpenLogout = { appScreen = AppScreen.Logout }
                            )
                            AppScreen.Logout -> LogoutScreen(
                                onBack = { appScreen = AppScreen.Settings },
                                onLogout = {
                                    auth.signOut()
                                    appScreen = AppScreen.Home
                                    screen = AuthScreen.SignIn
                                    isAuthenticated = false
                                }
                            )
                        }
                    }
                    else -> when (screen) {
                        AuthScreen.SignIn -> LoginScreen(
                            isSubmitting = isSubmitting,
                            submitError = submitError,
                            onSignIn = { details ->
                                isSubmitting = true
                                submitError = null
                                auth.signInWithEmailAndPassword(details.email, details.password)
                                    .addOnSuccessListener { isSubmitting = false; isAuthenticated = true }
                                    .addOnFailureListener { isSubmitting = false; submitError = "We couldn't sign you in. Check your email and password." }
                            },
                            onForgotPassword = { navigate(AuthScreen.ResetPassword) },
                            onCreateAccount = { navigate(AuthScreen.CreateAccount) }
                        )
                        AuthScreen.CreateAccount -> CreateAccountScreen(
                            isSubmitting = isSubmitting,
                            submitError = submitError,
                            accountCreated = requestSucceeded,
                            onCreateAccount = { details ->
                                isSubmitting = true
                                submitError = null
                                auth.createUserWithEmailAndPassword(details.email, details.password)
                                    .addOnSuccessListener { isSubmitting = false; requestSucceeded = true; isAuthenticated = true }
                                    .addOnFailureListener { exception -> isSubmitting = false; submitError = exception.localizedMessage ?: "Unable to create account." }
                            },
                            onSignIn = { navigate(AuthScreen.SignIn) }
                        )
                        AuthScreen.ResetPassword -> PasswordResetEmailScreen(
                            isSubmitting = isSubmitting,
                            submitError = submitError,
                            emailSent = requestSucceeded,
                            onSendResetEmail = { email ->
                                isSubmitting = true
                                submitError = null
                                auth.sendPasswordResetEmail(email)
                                    .addOnSuccessListener { isSubmitting = false; requestSucceeded = true }
                                    .addOnFailureListener { exception ->
                                        isSubmitting = false
                                        if (exception is FirebaseAuthInvalidUserException) requestSucceeded = true
                                        else submitError = "We couldn't send the reset link. Check your connection and try again."
                                    }
                            },
                            onBackToSignIn = { navigate(AuthScreen.SignIn) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
