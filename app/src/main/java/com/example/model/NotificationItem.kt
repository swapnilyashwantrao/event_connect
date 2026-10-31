package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class NotificationType(val value: String) {
    INVITATION("invitation"),
    RSVP_RESPONSE("rsvp_response"),
    EVENT_UPDATE("event_update"),
    EVENT_CANCELLED("event_cancelled"),
    REMINDER("reminder")
}

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val notificationId: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String? = null,
    val eventId: String? = null,
    val eventTitle: String? = null,
    val title: String = "",
    val message: String = "",
    val notificationType: String = NotificationType.INVITATION.value,
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
