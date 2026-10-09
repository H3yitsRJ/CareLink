package com.example.carelink.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@Composable
fun MedicationRefillsScreen(
    onRequestRefill: () -> Unit = {},
    onTrackRefills: () -> Unit = {}
) {
    var showPharmacyDialog by remember {
        mutableStateOf(false)
    }
    var selectedPharmacy by remember {
        mutableStateOf<String?>(null)
    }
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Medication Refills",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Manage and track your medication refill requests.")

        Button(
            onClick = { showPharmacyDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Request a Refill")
        }

        OutlinedButton(
            onClick = onTrackRefills,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Track Refill Requests")
        }
    }
    if (showPharmacyDialog) {
        AlertDialog(
            onDismissRequest = { showPharmacyDialog = false },
            title = { Text("Choose your pharmacy") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Where would you like to request your refill?")

                    OutlinedButton(
                        onClick = {
                            showPharmacyDialog = false
                            selectedPharmacy = "CVS Pharmacy"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CVS Pharmacy")
                    }

                    OutlinedButton(
                        onClick = {
                            showPharmacyDialog = false
                            selectedPharmacy = "Walgreens"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Walgreens")
                    }

                    OutlinedButton(
                        onClick = {
                            showPharmacyDialog = false
                            onRequestRefill()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Other / Enter Manually")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPharmacyDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    selectedPharmacy?.let { pharmacy ->
        AlertDialog(
            onDismissRequest = {
                selectedPharmacy = null
            },
            title = {
                Text("Open $pharmacy?")
            },
            text = {
                Text(
                    "You are about to leave CareLink " +
                            "to manage your prescriptions through $pharmacy."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = when (pharmacy) {
                            "CVS Pharmacy" -> "https://www.cvs.com/pharmacy"
                            "Walgreens" -> "https://www.walgreens.com"
                            else -> null
                        }

                        if (url != null) {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                            )
                            context.startActivity(intent)
                        }

                        selectedPharmacy = null
                    }
                ) {
                    Text("Open Pharmacy")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedPharmacy = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}