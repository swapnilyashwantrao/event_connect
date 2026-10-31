package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RsvpStatus(val label: String) {
    PENDING("Awaiting Response"),
    ACCEPTED("Accepted"),
    MAYBE("Maybe"),
    DECLINED("Declined")
}

@IgnoreExtraProperties
data class Invitation(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val recipientUsername: String = "",
    val recipientPhotoUrl: String? = null,
    val senderId: String = "",
    val senderName: String = "",
    val eventId: String = "",
    val eventTitle: String = "",
    val eventDate: String = "",
    val eventLocation: String = "",
    val eventCategory: String = "Party",
    val coverImageUrl: String? = null,
    val invitationMessage: String = "",
    val invitationStatus: String = "sent", // "sent", "delivered", "seen"
    val rsvpStatus: String = "PENDING", // PENDING, ACCEPTED, MAYBE, DECLINED
    val responseMessage: String? = null,
    @ServerTimestamp val sentAt: Timestamp? = null,
    @ServerTimestamp val respondedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
