package com.example.carelink.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import com.example.carelink.data.HealthConcernRepository
import com.example.carelink.model.HealthConcern
import com.example.carelink.model.ConcernSeverity
import com.example.carelink.model.ConcernStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color


@Composable
fun AddHealthConcernScreen(
    patientId: String,
    repository: HealthConcernRepository,
    onSaved: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("Low") }
    var notes by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add health concern")

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Health concern") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Severity")

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { severity = "Low" },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (severity == "Low") Color(0xFFDDEFD8) else Color.Transparent
                    )
                ) {
                    Text("Low")
                }

                OutlinedButton(
                    onClick = { severity = "Medium" },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (severity == "Medium") Color(0xFFDDEFD8) else Color.Transparent
                    )
                ) {
                    Text("Medium")
                }

                OutlinedButton(
                    onClick = { severity = "High" },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (severity == "High") Color(0xFFDDEFD8) else Color.Transparent
                    )
                ) {
                    Text("High")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val recordedDate = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        ).format(Date())

                        val concern = HealthConcern(
                            id = "",
                            patientId = patientId,
                            title = title.trim(),
                            severity = ConcernSeverity.valueOf(severity.uppercase()),
                            recordedDate = recordedDate,
                            description = notes.trim(),
                            status = ConcernStatus.ACTIVE,
                            appointmentId = null
                        )

                        repository.addConcern(
                            patientId = patientId,
                            concern = concern
                        ) { result ->
                            if (result.isSuccess) {
                                onSaved()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save health concern")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    onCancel()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Text("Cancel")
            }
        }
    }
}