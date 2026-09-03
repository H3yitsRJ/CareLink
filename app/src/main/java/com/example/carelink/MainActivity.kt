package com.example.carelink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.carelink.ui.theme.CareLinkTheme
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import com.google.firebase.firestore.FirebaseFirestore
import com.example.carelink.screens.CreateAccountScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import screens.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = FirebaseFirestore.getInstance()

        val testData = hashMapOf(
            "message" to "CareLink Firebase connected"
        )

        db.collection("test")
            .add(testData)
            .addOnSuccessListener {
                Log.d("FirebaseTest", "Firestore write successful")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseTest", "Firestore write failed", e)
            }
        enableEdgeToEdge()
        setContent {
            CareLinkTheme {

                val auth = FirebaseAuth.getInstance()
                var isAuthenticated by remember {
                    mutableStateOf(auth.currentUser != null)
                }

                var isSubmitting by remember {
                    mutableStateOf(false)
                }

                var submitError by remember {
                    mutableStateOf<String?>(null)
                }

                var accountCreated by remember {
                    mutableStateOf(false)
                }

                var showLoginScreen by remember {
                    mutableStateOf(false)
                }

                // TODO: Remove this temporary signed-in message once the Dashboard screen
// is complete. Replace it with navigation to the CareLink Dashboard.
                if (isAuthenticated) {
                    Text(
                        text = "User is signed in",
                        modifier = Modifier
                            .safeDrawingPadding()
                            .padding(24.dp)
                    )
                } else if (showLoginScreen) {
                    LoginScreen(
                        onSignInClick = { email, password ->
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    isAuthenticated = true
                                }
                                .addOnFailureListener { exception ->
                                    submitError =
                                        exception.localizedMessage
                                            ?: "Unable to sign in."
                                }
                        },
                        onCreateAccountClick = {
                            showLoginScreen = false
                        }
                    )
                } else {
                    CreateAccountScreen(
                        isSubmitting = isSubmitting,
                        submitError = submitError,
                        accountCreated = accountCreated,

                        onCreateAccount = { details ->

                            isSubmitting = true
                            submitError = null

                            auth.createUserWithEmailAndPassword(
                                details.email,
                                details.password
                            )
                                .addOnSuccessListener {
                                    isSubmitting = false
                                    accountCreated = true
                                    isAuthenticated = true
                                }
                                .addOnFailureListener { exception ->
                                    isSubmitting = false
                                    submitError =
                                        exception.localizedMessage
                                            ?: "Unable to create account."
                                }
                        },
                        onSignIn = { showLoginScreen = true }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CareLinkTheme {
        Greeting("Android")
    }
}
