package com.example.carelink.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun RefillRequestTrackingScreen(
    requests: List<RefillRequest> = emptyList(),
    medicationNames: Map<String, String> = emptyMap(),
    isLoading: Boolean = false,
    error: String? = null,
    onRefresh: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedRequest by remember {
        mutableStateOf<RefillRequest?>(null)
    }

    BackHandler {
        if (selectedRequest != null) selectedRequest = null else onBack()
    }

    val sortedRequests = requests.sortedByDescending {
        it.requestedAt
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Text(
                    text = "Refill Request Tracking",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(Modifier.height(16.dp))

                Button(onClick = onRefresh, enabled = !isLoading) {
                    Text("Refresh")
                }

                Spacer(Modifier.height(16.dp))

            }
        }
        when {
            isLoading -> item { LoadingState("Loading refill requests") }

            error != null -> item { CareErrorState(
                message = error,
                onRetry = onRefresh,
                retryEnabled = !isLoading
            ) }

            sortedRequests.isEmpty() -> item { Text("No refill requests found.") }

            else -> {
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
                        Column(
                            Modifier.padding(16.dp)
                        ) {
                            Text(
                                request.medicationName.ifBlank {
                                    medicationNames[request.medicationId] ?: "Medication no longer available"
                                },
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
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Text("Medication: ${request.medicationName.ifBlank { medicationNames[request.medicationId] ?: "Medication no longer available" }}")
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
                TextButton(
                    onClick = { selectedRequest = null }
                ) {
                    Text("Close")
                }
            }

        )

    }

}
