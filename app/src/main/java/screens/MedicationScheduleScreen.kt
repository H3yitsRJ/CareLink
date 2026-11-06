package com.example.carelink.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Medication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import navigation.BottomNavBar
import navigation.BottomNavDestination

@Composable
fun MedicationScheduleScreen(
    onBack: () -> Unit = {},
    onNavigate: (BottomNavDestination) -> Unit = {},
    onEditMedication: (Medication) -> Unit = {},
    onAddMedication: () -> Unit = {}
) {
    val patientId = FirebaseAuth.getInstance().currentUser?.uid
    val connected = LocalConnectionAvailable.current
    var medications by remember(patientId) { mutableStateOf<List<Medication>>(emptyList()) }
    var loading by remember(patientId) { mutableStateOf(true) }
    var error by remember(patientId) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    DisposableEffect(patientId, connected, retry) {
        var active = true
        loading = true
        error = null
        if (patientId == null) {
            loading = false
            error = "Sign in to view your medications."
        } else {
            FirebaseFirestore.getInstance().collection("users").document(patientId)
                .collection("medications").get(if (connected) Source.DEFAULT else Source.CACHE)
                .addOnSuccessListener { result ->
                    if (active) {
                        medications = result.documents.mapNotNull {
                            Medication.fromFirestore(it.id, it.data.orEmpty() + ("patientId" to patientId))
                        }.filter { it.active }
                        loading = false
                    }
                }.addOnFailureListener {
                    if (active) { loading = false; error = "We couldn't load your medications. Please try again." }
                }
        }
        onDispose { active = false }
    }
    MedicationScheduleContent(medications, loading, error, { retry++ }, onBack, onNavigate, onEditMedication, onAddMedication)
}

@Composable
internal fun MedicationScheduleContent(
    medications: List<Medication>, loading: Boolean = false, error: String? = null,
    onRetry: () -> Unit = {}, onBack: () -> Unit = {},
    onNavigate: (BottomNavDestination) -> Unit = {},
    onEditMedication: (Medication) -> Unit = {}, onAddMedication: () -> Unit = {}
) {
    BackHandler(onBack = onBack)
    Scaffold(bottomBar = { BottomNavBar(BottomNavDestination.Medications, onNavigate) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Medication schedule", style = MaterialTheme.typography.headlineMedium)
                Text("Select a medication to edit its frequency and reminder times.")
            }
            when {
                loading -> item { LoadingState("Loading medications") }
                error != null -> item { StateMessage(error, true); TextButton(onClick = onRetry) { Text("Retry") } }
                medications.isEmpty() -> item {
                    StateMessage("No active medications are available.")
                    Button(onClick = onAddMedication) { Text("Add medication") }
                }
                else -> items(medications, key = { it.id }) { medication ->
                    MedicationCard(medication, onClick = { onEditMedication(medication) })
                }
            }
        }
    }
}
