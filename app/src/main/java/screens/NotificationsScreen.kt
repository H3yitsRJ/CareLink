
package com.example.carelink.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.carelink.ui.theme.CareLinkTheme

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.carelink.data.NotificationRepository
import com.example.carelink.model.CareNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration


@Composable
fun NotificationsScreen() {

    val userId = FirebaseAuth.getInstance().currentUser?.uid

    val repository = remember {
        NotificationRepository()
    }

    var notifications by remember {
        mutableStateOf<List<CareNotification>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    DisposableEffect(userId) {
        if (userId == null) {
            isLoading = false
            errorMessage = "Please sign in to view notifications."
            onDispose { }
        } else {
            isLoading = true
            errorMessage = null

            val listener = repository.observeNotifications(userId) { result ->
                isLoading = false

                result.onSuccess {
                    notifications = it
                    errorMessage = null
                }

                result.onFailure {
                    errorMessage = it.message ?: "Unable to load notifications."
                }
            }

            onDispose {
                listener.remove()
            }
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp)
    ) {

        Text(
            text = "Notifications",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        when {
            isLoading -> {
                CircularProgressIndicator()
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
            }

            notifications.isEmpty() -> {
                Text(
                    text = "No notifications yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = notifications.sortedByDescending {
                            it.timestamp.seconds
                        },
                        key = { it.id }
                    ) { notification ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!notification.isRead) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                if (!notification.isRead && userId != null) {
                                    TextButton(
                                        onClick = {
                                            repository.markAsRead(
                                                userId,
                                                notification.id
                                            ) { result ->
                                                result.onFailure {
                                                    errorMessage =
                                                        it.message
                                                            ?: "Unable to mark notification as read."
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Mark as read")
                                    }
                                }

                                Text(
                                    text = notification.title,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = notification.message,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = java.text.SimpleDateFormat(
                                        "MMM d, yyyy • h:mm a",
                                        java.util.Locale.getDefault()
                                    ).format(notification.timestamp.toDate()),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }


}
    @Preview(showBackground = true)
    @Composable
    fun NotificationsScreenPreview() {
        CareLinkTheme {
            NotificationsScreen()
        }
    }
