package com.example.carelink.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.model.RefillRequest
import com.example.carelink.model.RefillRequestStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RefillRequestTrackingScreen(
    requests: List<RefillRequest> = emptyList(),
    medicationNames: Map<String, String> = emptyMap(),
    isLoading: Boolean = false,
    error: String? = null,
    onRefresh: () -> Unit = {}
) {
    var selectedRequest by remember {
        mutableStateOf<RefillRequest?>(null)
    }

    val sortedRequests = requests.sortedByDescending {
        it.requestedAt
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Refill Request Tracking",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = onRefresh) {
            Text("Refresh")
        }

        Spacer(Modifier.height(16.dp))

        when {
            isLoading -> CircularProgressIndicator()

            error != null -> Text(
                text = error,
                color = MaterialTheme.colorScheme.error
            )

            sortedRequests.isEmpty() ->
                Text("No refill requests found.")

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedRequests, key = { it.id }) { request ->
                    val status = request.status
                    val completed = status == RefillRequestStatus.COMPLETED ||
                            status == RefillRequestStatus.CANCELLED

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRequest = request },
                        colors = CardDefaults.cardColors(
                            containerColor = if (completed)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                medicationNames[request.medicationId]
                                    ?: request.medicationId,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                SimpleDateFormat(
                                    "MMM d, yyyy",
                                    Locale.getDefault()
                                ).format(Date(request.requestedAt))
                            )
                            Text("Status: ${status.name}")
                        }
                    }
                }
            }
        }
    }

    selectedRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { selectedRequest = null },
            title = { Text("Refill Request Details") },
            text = {
                Column {
                    Text("Medication: ${medicationNames[request.medicationId] ?: request.medicationId}")
                    Text("Status: ${request.status.name}")
                    Text("Request ID: ${request.id}")
                    Text("Requested by: ${request.requestedById}")
                    Text("Note: ${request.note.ifBlank { "None" }}")
                    Text(
                        "Date: " + SimpleDateFormat(
                            "MMM d, yyyy h:mm a",
                            Locale.getDefault()
                        ).format(Date(request.requestedAt))
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedRequest = null }) {
                    Text("Close")
                }
            }
        )
    }
}