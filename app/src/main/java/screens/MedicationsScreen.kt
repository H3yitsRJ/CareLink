package com.example.carelink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    onAddMedication: () -> Unit = {}
) {

    var medications by remember {
        mutableStateOf<List<Medication>>(emptyList())
    }

    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .collection("medications")
                .get()
                .addOnSuccessListener { result ->

                    medications = result.documents.map { document ->
                        Medication(
                            id = document.getString("id").orEmpty(),
                            patientId = document.getString("patientId").orEmpty(),
                            name = document.getString("name").orEmpty(),
                            strength = document.getString("strength").orEmpty(),
                            dose = document.getString("dose").orEmpty(),
                            frequency = document.getString("frequency").orEmpty(),
                            reminderTimes =
                                document.get("reminderTimes") as? List<String>
                                    ?: emptyList(),
                            instructions =
                                document.getString("instructions").orEmpty(),
                            active =
                                document.getBoolean("active") ?: true
                        )
                    }
                }
        }
    }

    val formattedDate = SimpleDateFormat(
        "EEEE, MMMM d",
        Locale.getDefault()
    ).format(Date())
    Scaffold(
        bottomBar = {
            BottomNavBar(selectedDestination = BottomNavDestination.Medications)
        }
    ){ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(32.dp)
        ){
            Text(
                text = "Medications",
                fontSize = 43.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Manage and schedule your medications"
            )

            Spacer(modifier = Modifier.height(50.dp))

            Button(
                onClick = onAddMedication,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(63.dp)
                    .width(197.dp)
            ){
                Text(
                    text = "Add Medication",
                    fontSize = 20.sp
                )
            }

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
                        text = "No medications to display. \n\nAdd a medication to start tracking doses and schedules.",
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
                            medication = medications[index]
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun MedicationCard(
    medication: Medication
) {
    Card(
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
