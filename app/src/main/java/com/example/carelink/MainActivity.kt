package com.example.carelink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.carelink.screens.CreateAccountScreen
import com.example.carelink.screens.LoginScreen
import com.example.carelink.screens.PasswordResetEmailScreen
import com.example.carelink.ui.theme.CareLinkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

private enum class AuthScreen { SignIn, CreateAccount, ResetPassword }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureFirebaseEmulators()
        enableEdgeToEdge()

        setContent {
            CareLinkTheme {
                val auth = remember { FirebaseAuth.getInstance() }
                var screen by remember { mutableStateOf(AuthScreen.SignIn) }
                var isSubmitting by remember { mutableStateOf(false) }
                var submitError by remember { mutableStateOf<String?>(null) }
                var requestSucceeded by remember { mutableStateOf(false) }

                fun navigate(destination: AuthScreen) {
                    screen = destination
                    isSubmitting = false
                    submitError = null
                    requestSucceeded = false
                }

                when (screen) {
                    AuthScreen.SignIn -> LoginScreen(
                        isSubmitting = isSubmitting,
                        submitError = submitError,
                        onSignIn = { details ->
                            isSubmitting = true
                            submitError = null
                            auth.signInWithEmailAndPassword(details.email, details.password)
                                .addOnSuccessListener { isSubmitting = false }
                                .addOnFailureListener {
                                    isSubmitting = false
                                    submitError = "We couldn't sign you in. Check your email and password."
                                }
                        },
                        onForgotPasswordClick = { navigate(AuthScreen.ResetPassword) },
                        onCreateAccountClick = { navigate(AuthScreen.CreateAccount) }
                    )

                    AuthScreen.CreateAccount -> CreateAccountScreen(
                        isSubmitting = isSubmitting,
                        submitError = submitError,
                        accountCreated = requestSucceeded,
                        onCreateAccount = { details ->
                            isSubmitting = true
                            submitError = null
                            auth.createUserWithEmailAndPassword(details.email, details.password)
                                .addOnSuccessListener {
                                    isSubmitting = false
                                    requestSucceeded = true
                                }
                                .addOnFailureListener { exception ->
                                    isSubmitting = false
                                    submitError = exception.localizedMessage ?: "Unable to create account."
                                }
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
                                .addOnSuccessListener {
                                    isSubmitting = false
                                    requestSucceeded = true
                                }
                                .addOnFailureListener { exception ->
                                    isSubmitting = false
                                    if (exception is FirebaseAuthInvalidUserException) {
                                        requestSucceeded = true
                                    } else {
                                        submitError = "We couldn't send the reset link. Check your connection and try again."
                                    }
                                }
                        },
                        onBackToSignIn = { navigate(AuthScreen.SignIn) }
                    )
                }
            }
        }
    }
}
