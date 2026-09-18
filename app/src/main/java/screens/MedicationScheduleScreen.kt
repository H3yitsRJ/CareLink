package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.carelink.model.Medication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationScheduleScreen() {
    var medications by remember {
        mutableStateOf<List<Medication>>(emptyList())
    }

    var selectedMedication by remember {
        mutableStateOf<Medication?>(null)
    }

    var showScheduleDialog by remember {
        mutableStateOf(false)
    }

    var reminderTime by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val selectedDays = remember {
        mutableStateListOf<String>()
    }

    val formattedDate = SimpleDateFormat(
        "EEEE, MMMM d",
        Locale.getDefault()
    ).format(Date())

    Scaffold(
        bottomBar = {
            BottomNavBar(selectedDestination =
                BottomNavDestination.Medications)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(32.dp)
        ) {
            Text(
                text = "Schedule",
                fontSize = 43.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select a medication to add a schedule"
            )

            Spacer(modifier = Modifier.height(43.dp))

            Text(
                text = formattedDate,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(18.dp))

            if(medications.isEmpty()){

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp),
                    contentAlignment = Alignment.Center
                ){
                    Text(
                        text = "No medications to display. \n\nAdd a medication to create a schedule.",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }

            }else{

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {

                    items(count = medications.size) { index ->

                        MedicationCard(
                            medication = medications[index],
                            onClick = {
                                selectedMedication = medications[index]
                                showScheduleDialog = true
                            }
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )
                    }
                }
            }

        }

        if(showScheduleDialog && selectedMedication != null) {

            val weekDays = listOf(
                "Mon",
                "Tue",
                "Wed",
                "Thu",
                "Fri",
                "Sat",
                "Sun"
            )

            AlertDialog(
                onDismissRequest = {
                    showScheduleDialog = false
                },
                title = {
                    Text("Schedule Medication")
                },
                text = {
                    Column {
                        Text(
                            "Create a schedule for ${selectedMedication!!.name}"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Select Days")

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow {

                            weekDays.forEach { day ->

                                FilterChip(
                                    selected = day in selectedDays,
                                    onClick = {
                                        if(day in selectedDays) {
                                            selectedDays.remove(day)
                                        } else {
                                            selectedDays.add(day)
                                        }
                                    },
                                    label = {
                                        Text(day)
                                    }
                                )

                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = reminderTime,
                            onValueChange = {
                                reminderTime = it
                            },
                            label = {
                                Text("Reminder Time")
                            },
                            placeholder = {
                                Text("8:00 AM")
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if(errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = Color.Red
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            errorMessage = null

                            val user =
                                FirebaseAuth.getInstance().currentUser

                            if(selectedDays.isEmpty()) {
                                errorMessage = "Please select at least one day."
                                return@TextButton
                            }

                            if(reminderTime.isBlank()) {
                                errorMessage = "Please enter a reminder time."
                                return@TextButton
                            }

                            if(user != null && selectedMedication != null) {

                                val scheduleData = hashMapOf(
                                    "medicationId" to selectedMedication!!.id,
                                    "medicationName" to selectedMedication!!.name,
                                    "days" to selectedDays,
                                    "time" to reminderTime
                                )

                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(user.uid)
                                    .collection("medicationSchedules")
                                    .add(scheduleData)
                                    .addOnSuccessListener {
                                        errorMessage = null
                                        selectedDays.clear()
                                        reminderTime = ""
                                        showScheduleDialog = false
                                    }
                                    .addOnFailureListener {
                                        errorMessage = "Unable to save schedule. Please try again."
                                    }
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showScheduleDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SchedulerScreenPreview() {
    CareLinkTheme {
        MedicationScheduleScreen()
    }
}