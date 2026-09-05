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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import screens.LoginScreen
import screens.LogoutScreen
import screens.SettingsScreen

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

                /*
                 * If Firebase does not currently have a
                 * signed-in user, start on the Login screen.
                 */
                var showLoginScreen by remember {
                    mutableStateOf(
                        auth.currentUser == null
                    )
                }

                /*
                 * Temporary navigation for the authenticated
                 * portion of CareLink.
                 * Later this can be replaced
                 */
                var authenticatedScreen by remember {
                    mutableStateOf("home")
                }

                // TODO: Remove this temporary signed-in message once the Dashboard screen
                // is complete. Replace it with navigation to the CareLink Dashboard.

                // -----------------------------------------
                // AUTHENTICATED SCREENS
                // -----------------------------------------

               if (isAuthenticated) {
                   when (authenticatedScreen) {
                       // -----------------------------
                       // TEMPORARY HOME SCREEN
                       // -----------------------------
                       "home" -> {
                           Column(
                               modifier = Modifier
                                   .fillMaxSize()
                                   .safeDrawingPadding()
                                   .padding(24.dp),
                               verticalArrangement = Arrangement.Center,
                               horizontalAlignment = Alignment.CenterHorizontally
                           ) {
                               Text(
                                   text = "User is signed in"
                               )
                               Button(
                                   onClick = {
                                       authenticatedScreen = "settings"
                                   }
                               ) {
                                   Text("Settings")
                               }
                           }
                       }
                       // -----------------------------
                       // SETTINGS SCREEN
                       // -----------------------------
                       "settings" -> {
                           SettingsScreen(
                               onLogoutClick = {
                                   authenticatedScreen = "logout"
                               }
                           )
                       }
                       // -----------------------------
                       // LOGOUT SCREEN
                       // -----------------------------
                       "logout" -> {
                           LogoutScreen(
                               /*
                                 * Cancel does NOT sign
                                 * the user out.
                                 *
                                 * It simply returns to
                                 * Settings.
                                 */
                               onCancel = {
                                   authenticatedScreen = "settings"
                               },

                               /*
                                 * This is where the real
                                 * Firebase logout occurs.
                                 */
                               onConfirmLogout = {
                                   auth.signOut()
                                   isAuthenticated = false
                                   showLoginScreen = true

                                   /*
                                     * Reset the authenticated
                                     * screen so the next login
                                     * starts at Home.
                                     */
                                   authenticatedScreen = "home"
                               }
                           )
                       }
                   }
                   // -----------------------------------------
                   // LOGIN SCREEN
                   // -----------------------------------------
               } else if (showLoginScreen) {
                   LoginScreen(
                       onSignInClick = {
                                email,
                                password ->
                           submitError = null

                           auth.signInWithEmailAndPassword(
                               email,
                               password
                           )
                               .addOnSuccessListener {
                                   isAuthenticated = true

                                   /*
                                     * Make sure every new
                                     * login begins at Home.
                                     */
                                   authenticatedScreen = "home"
                               }
                               .addOnFailureListener {
                                        exception ->
                                   submitError =
                                       exception
                                           .localizedMessage
                                           ?: "Unable to sign in."
                               }
                       },
                       onCreateAccountClick = {
                           showLoginScreen = false
                           submitError = null
                       }
                   )
                   // -----------------------------------------
                   // CREATE ACCOUNT SCREEN
                   // -----------------------------------------
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
                                   authenticatedScreen = "home"
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
                           showLoginScreen = true
                           submitError = null
                       }
                   )
               }
            }
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier
) {
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
