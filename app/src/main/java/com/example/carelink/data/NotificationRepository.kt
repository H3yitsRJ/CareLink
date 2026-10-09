
package com.example.carelink.data

import com.example.carelink.model.CareNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class NotificationRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {
    fun observeNotifications(
        userId: String,
        onResult: (Result<List<CareNotification>>) -> Unit
    ): ListenerRegistration {
        return firestore
            .collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents
                    ?.map { document ->
                        CareNotification(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            message = document.getString("message") ?: "",
                            timestamp = document.getTimestamp("timestamp")
                                ?: com.google.firebase.Timestamp.now(),
                            isRead = document.getBoolean("isRead") ?: false,
                            relatedFeature =
                                document.getString("relatedFeature"),
                            relatedId =
                                document.getString("relatedId")
                        )
                    } ?: emptyList()

                onResult(Result.success(notifications))
            }
    }

    fun markAsRead(
        userId: String,
        notificationId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        firestore
            .collection("users")
            .document(userId)
            .collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }
}
