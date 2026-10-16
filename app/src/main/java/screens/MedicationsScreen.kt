package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carelink.ui.theme.CareLinkTheme
import navigation.BottomNavBar
import navigation.BottomNavDestination
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.carelink.model.Medication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MedicationsScreen(
    onAddMedication: () -> Unit = {},
    onMedicationSelected: (Medication) -> Unit = {},
    onMedicationScheduler: () -> Unit = {},
    successMessage: String? = null,
    onNavigate: (BottomNavDestination) -> Unit = {},
    onCaregiverMedications: () -> Unit = {},
    onOpenCareHistory: () -> Unit = {},
    onOpenRefills: () -> Unit = {}
) {

    var medications by remember {
        mutableStateOf<List<Medication>>(emptyList())
    }

    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableStateOf(0) }

    val connectionAvailable = LocalConnectionAvailable.current
    LaunchedEffect(retry, connectionAvailable) {
        loading = true
        loadError = null
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .collection("medications")
                .get(if (connectionAvailable) com.google.firebase.firestore.Source.DEFAULT else com.google.firebase.firestore.Source.CACHE)
                .addOnSuccessListener { result ->

                    medications = result.documents.mapNotNull { document ->
                        Medication.fromFirestore(document.id, document.data.orEmpty() + ("patientId" to user.uid))
                    }
                    loading = false
                }
                .addOnFailureListener {
                    loading = false
                    loadError = "We couldn't load your medications."
                }
        } else {
            loading = false
            loadError = "Sign in to view your medications."
        }
    }

    MedicationsContent(medications, loading, loadError, { retry++ }, onAddMedication,
        onMedicationSelected, onMedicationScheduler, successMessage, onCaregiverMedications,
        onOpenCareHistory, onOpenRefills, onNavigate)
}

@Composable
internal fun MedicationsContent(
    medications: List<Medication>, loading: Boolean = false, loadError: String? = null,
    onRetry: () -> Unit = {}, onAddMedication: () -> Unit = {},
    onMedicationSelected: (Medication) -> Unit = {}, onMedicationScheduler: () -> Unit = {},
    successMessage: String? = null, onCaregiverMedications: () -> Unit = {},
    onOpenCareHistory: () -> Unit = {}, onOpenRefills: () -> Unit = {},
    onNavigate: (BottomNavDestination) -> Unit = {}
) {
    Scaffold(bottomBar = { BottomNavBar(BottomNavDestination.Medications, onNavigate) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            item {
                Text("Medications", style = MaterialTheme.typography.headlineLarge)
                successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                TextButton(onClick = onCaregiverMedications) { Text("Caregiver medications") }
                TextButton(onClick = onOpenCareHistory) { Text("Care history") }
                TextButton(onClick = onOpenRefills) { Text("Medication refills") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onAddMedication, modifier = Modifier.weight(1f).height(63.dp)) { Text("Add Medication") }
                    Button(onClick = onMedicationScheduler, modifier = Modifier.size(63.dp), contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.CalendarMonth, "Medication Scheduler", Modifier.size(35.dp))
                    }
                }
                Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
            }
            when {
                loading -> item { StateMessage("Loading medications") }
                loadError != null -> item { StateMessage(loadError, true); Button(onClick = onRetry) { Text("Retry") } }
                medications.isEmpty() -> item { StateMessage("No medications to display. Add a medication to start tracking doses and schedules.") }
                else -> items(count = medications.size, key = { medications[it].id }) { index ->
                    MedicationCard(medications[index], onClick = { onMedicationSelected(medications[index]) })
                }
            }
        }
    }
}

@Composable
fun MedicationCard(
    medication: Medication,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = medication.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Strength: ${medication.strength}",
                fontSize = 16.sp
            )

            Text(
                text = "Dose: ${medication.dose}",
                fontSize = 16.sp
            )

            Text(
                text = "Frequency: ${medication.frequency}",
                fontSize = 16.sp
            )

            if (medication.reminderTimes.isNotEmpty()) {
                Text(
                    text = "Reminder: ${medication.reminderTimes.first()}",
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun MedicationsScreenPreview() {
    CareLinkTheme {
        MedicationsScreen()
    }
}
