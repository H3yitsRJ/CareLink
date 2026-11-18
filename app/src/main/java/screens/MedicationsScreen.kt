package com.example.carelink.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carelink.model.Medication
import com.example.carelink.ui.theme.CareLinkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import navigation.BottomNavBar
import navigation.BottomNavDestination
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    MedicationsContent(
        medications,
        loading,
        loadError,
        { retry++ },
        onAddMedication,
        onMedicationSelected,
        onMedicationScheduler,
        successMessage,
        onCaregiverMedications,
        onOpenCareHistory,
        onOpenRefills,
        onNavigate
    )

}

@Composable
internal fun MedicationsContent(
    medications: List<Medication>,
    loading: Boolean = false,
    loadError: String? = null,
    onRetry: () -> Unit = {},
    onAddMedication: () -> Unit = {},
    onMedicationSelected: (Medication) -> Unit = {},
    onMedicationScheduler: () -> Unit = {},
    successMessage: String? = null,
    onCaregiverMedications: () -> Unit = {},
    onOpenCareHistory: () -> Unit = {},
    onOpenRefills: () -> Unit = {},
    onNavigate: (BottomNavDestination) -> Unit = {}
) {
    var showMedicationTools by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                BottomNavDestination.Medications,
                onNavigate
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Medications",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "Manage your medications and schedules.",
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )

                successMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAddMedication,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2F5D60),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Add medication")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            TextButton(onClick = { showMedicationTools = true }) {
                                Text("Medication tools")
                            }

                            DropdownMenu(
                                expanded = showMedicationTools,
                                onDismissRequest = { showMedicationTools = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Caregiver medications") },
                                    onClick = {
                                        showMedicationTools = false
                                        onCaregiverMedications()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Care history") },
                                    onClick = {
                                        showMedicationTools = false
                                        onOpenCareHistory()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Medication refills") },
                                    onClick = {
                                        showMedicationTools = false
                                        onOpenRefills()
                                    }
                                )
                            }
                        }

                        IconButton(onClick = onMedicationScheduler) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Medication scheduler",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.titleLarge
                )

                HorizontalDivider()
            }

            when {
                loading -> item {
                    LoadingState("Loading medications")
                }

                loadError != null -> item {
                    CareErrorState(
                        message = loadError,
                        onRetry = onRetry
                    )
                }

                medications.isEmpty() -> item {
                    StateMessage(
                        "No medications to display. Add a medication to start tracking doses and schedules."
                    )
                }

                else -> items(
                    count = medications.size,
                    key = { medications[it].id }
                ) { index ->
                    MedicationCard(
                        medications[index],
                        onClick = { onMedicationSelected(medications[index]) }
                    )
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
    val clockFormat = LocalClockFormat.current
    val reminder = medication.reminderTimes.firstOrNull()

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Medication,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = listOf(medication.strength, medication.dose)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = medication.frequency,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (reminder != null) {
                    Text(
                        text = "Reminder: ${clockFormat.displayTime(reminder)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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
