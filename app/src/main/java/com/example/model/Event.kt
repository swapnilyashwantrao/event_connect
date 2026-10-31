package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Event(
    @DocumentId val id: String = "",
    val eventId: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val organizerPhotoUrl: String? = null,
    val title: String = "",
    val description: String = "",
    val category: String = "Party",
    val eventDate: String = "",
    val startTime: String = "",
    val endTime: String? = null,
    val location: String = "",
    val locationLink: String? = null,
    val coverImageUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val invitationMessage: String = "",
    val responseDeadline: String? = null,
    val guestLimit: Long = 0L,
    val visibility: String = "public", // "public", "private", "unlisted"
    val eventStatus: String = "active", // "active", "cancelled", "completed"
    val invitedCount: Long = 0L,
    val acceptedCount: Long = 0L,
    val maybeCount: Long = 0L,
    val declinedCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val pendingCount: Long
        get() = maxOf(0L, invitedCount - (acceptedCount + maybeCount + declinedCount))
}

object EventCategories {
    val all = listOf(
        "Birthday",
        "House Party",
        "Wedding",
        "Anniversary",
        "Get-Together",
        "Office Party",
        "Festival",
        "Dinner",
        "Custom"
    )
}
