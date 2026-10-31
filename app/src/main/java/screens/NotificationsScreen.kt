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
import com.example.carelink.data.NotificationData
import com.example.carelink.data.NotificationRepository
import com.example.carelink.model.CareNotification
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    userId: String? = FirebaseAuth.getInstance().currentUser?.uid,
    data: NotificationData? = null
) {
    BackHandler(onBack = onBack)
    val connected = LocalConnectionAvailable.current
    val repository = data ?: remember { NotificationRepository() }
    var notifications by remember(userId) { mutableStateOf<List<CareNotification>>(emptyList()) }
    var loading by remember(userId) { mutableStateOf(true) }
    var loadError by remember(userId) { mutableStateOf<String?>(null) }
    var saveError by remember(userId) { mutableStateOf<String?>(null) }
    var pending by remember(userId) { mutableStateOf(emptySet<String>()) }
    var retry by remember { mutableIntStateOf(0) }
    val alive = remember(userId) { mutableStateOf(true) }
    DisposableEffect(userId) { alive.value = true; onDispose { alive.value = false } }
    DisposableEffect(userId, repository, retry, connected) {
        var active = true
        loading = true
        loadError = null
        if (userId == null) {
            loading = false
            loadError = "Sign in to view notifications."
            onDispose { active = false }
        } else {
            val stop = repository.observeNotifications(userId) { result ->
                if (active) {
                    loading = false
                    result.onSuccess { notifications = it; loadError = null }
                        .onFailure { loadError = "We couldn't load notifications. Please try again." }
                }
            }
            onDispose { active = false; stop() }
        }
    }
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Notifications", style = MaterialTheme.typography.headlineMedium)
                if (saveError != null) StateMessage(saveError!!, true)
                if (loadError != null) {
                    StateMessage(loadError!!, true)
                    TextButton(onClick = { retry++ }, enabled = userId != null) { Text("Retry") }
                }
            }
        }
        when {
            loading -> item { LoadingState("Loading notifications") }
            notifications.isEmpty() && loadError == null -> item { StateMessage("No notifications yet.") }
            else -> {
                items(notifications.sortedByDescending { it.timestamp }, key = { it.id }) { notification ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                        containerColor = if (notification.isRead) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                    )) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(notification.title, style = MaterialTheme.typography.titleMedium)
                            Text(notification.message)
                            Text(if (notification.isRead) "Read" else "Unread", style = MaterialTheme.typography.labelLarge)
                            Text(SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(notification.timestamp.toDate()), style = MaterialTheme.typography.bodySmall)
                            if (!notification.isRead && userId != null) {
                                TextButton(enabled = connected && notification.id !in pending, onClick = {
                                    if (!connected || notification.id in pending) return@TextButton
                                    pending = pending + notification.id
                                    saveError = null
                                    repository.markAsRead(userId, notification.id) { result ->
                                        if (alive.value) {
                                            pending = pending - notification.id
                                            result.onSuccess {
                                                notifications = notifications.map { if (it.id == notification.id) it.copy(isRead = true) else it }
                                            }.onFailure { saveError = "We couldn't mark the notification as read. Please try again." }
                                        }
                                    }
                                }) { Text(if (notification.id in pending) "Marking as read" else "Mark as read") }
                            }
                        }
                    }
                }
            }
        }
    }
}
