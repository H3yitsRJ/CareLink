
package com.example.carelink.model

import com.google.firebase.Timestamp

data class CareNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val relatedFeature: String? = null,
    val relatedId: String? = null
)
