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
import com.google.firebase.firestore.FirebaseFirestore
import com.example.carelink.screens.CreateAccountScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth

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

                var isSubmitting by remember {
                    mutableStateOf(false)
                }

                var submitError by remember {
                    mutableStateOf<String?>(null)
                }

                var accountCreated by remember {
                    mutableStateOf(false)
                }

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
                            }
                            .addOnFailureListener { exception ->
                                isSubmitting = false
                                submitError =
                                    exception.localizedMessage
                                        ?: "Unable to create account."
                            }
                    }
                )
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
