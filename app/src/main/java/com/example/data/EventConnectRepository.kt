package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Event
import com.example.model.Invitation
import com.example.model.NotificationItem
import com.example.model.NotificationType
import com.example.model.RsvpStatus
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class EventConnectRepository(val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing EventConnect.")
    }

    suspend fun getOrCreateUserProfile(
        userId: String,
        fullName: String,
        email: String,
        photoUrl: String?
    ): UserProfile {
        val userDoc = db.collection("users").document(userId)
        val snapshot = try {
            userDoc.get().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, userDoc.path)
            throw e
        }

        if (snapshot.exists()) {
            return snapshot.toObject(UserProfile::class.java) ?: UserProfile(
                userId = userId,
                fullName = fullName,
                username = sanitizeUsername(fullName, userId)
            )
        }

        val username = sanitizeUsername(fullName, userId)
        val profileData = hashMapOf<String, Any?>(
            "userId" to userId,
            "fullName" to fullName.ifBlank { "User" },
            "username" to username,
            "profileImageUrl" to photoUrl,
            "bio" to "Excited for great events!",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            userDoc.set(profileData).await()
            val privateInfoDoc = userDoc.collection("private").document("info")
            privateInfoDoc.set(
                mapOf(
                    "email" to email,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, userDoc.path)
            throw e
        }

        return UserProfile(
            userId = userId,
            fullName = fullName,
            username = username,
            profileImageUrl = photoUrl,
            bio = "Excited for great events!"
        )
    }

    fun observeCurrentUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(UserProfile::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun updateUserProfile(fullName: String, bio: String?, photoUrl: String?) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val updates = mutableMapOf<String, Any?>(
            "fullName" to fullName.trim(),
            "bio" to bio?.trim(),
            "profileImageUrl" to photoUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            docRef.update(updates).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun getAllUsers(): List<UserProfile> {
        val currentUid = auth.currentUser?.uid ?: ""
        val path = "users"
        return try {
            val snapshot = db.collection(path).get().await()
            snapshot.toObjects(UserProfile::class.java).filter { it.userId != currentUid }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
        }
    }

    suspend fun searchUsers(query: String): List<UserProfile> {
        val cleanQuery = query.trim().lowercase()
        val allUsers = getAllUsers()
        if (cleanQuery.isBlank()) return allUsers
        return allUsers.filter {
            it.fullName.lowercase().contains(cleanQuery) ||
                    it.username.lowercase().contains(cleanQuery)
        }
    }

    suspend fun createEvent(
        event: Event,
        recipients: List<UserProfile>
    ): String {
        val uid = requireUserId()
        val eventId = if (event.eventId.isNotBlank()) event.eventId else "evt_${UUID.randomUUID().toString().take(12)}"
        val finalEvent = event.copy(
            eventId = eventId,
            organizerId = uid,
            invitedCount = recipients.size.toLong(),
            acceptedCount = 0L,
            maybeCount = 0L,
            declinedCount = 0L,
            eventStatus = "active"
        )

        val batch = db.batch()
        val eventDocRef = db.collection("events").document(eventId)

        val eventPayload = hashMapOf<String, Any?>(
            "eventId" to eventId,
            "organizerId" to uid,
            "organizerName" to finalEvent.organizerName.ifBlank { "Host" },
            "organizerPhotoUrl" to finalEvent.organizerPhotoUrl,
            "title" to finalEvent.title,
            "description" to finalEvent.description,
            "category" to finalEvent.category,
            "eventDate" to finalEvent.eventDate,
            "startTime" to finalEvent.startTime,
            "endTime" to finalEvent.endTime,
            "location" to finalEvent.location,
            "locationLink" to finalEvent.locationLink,
            "coverImageUrl" to finalEvent.coverImageUrl,
            "imageUrls" to finalEvent.imageUrls,
            "invitationMessage" to finalEvent.invitationMessage,
            "responseDeadline" to finalEvent.responseDeadline,
            "guestLimit" to finalEvent.guestLimit,
            "visibility" to finalEvent.visibility,
            "eventStatus" to finalEvent.eventStatus,
            "invitedCount" to finalEvent.invitedCount,
            "acceptedCount" to 0L,
            "maybeCount" to 0L,
            "declinedCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        batch.set(eventDocRef, eventPayload)

        for (recipient in recipients) {
            val inviteRef = eventDocRef.collection("invitations").document(recipient.userId)
            val invitePayload = hashMapOf<String, Any?>(
                "recipientId" to recipient.userId,
                "recipientName" to recipient.fullName,
                "recipientUsername" to recipient.username,
                "recipientPhotoUrl" to recipient.profileImageUrl,
                "senderId" to uid,
                "senderName" to finalEvent.organizerName.ifBlank { "Host" },
                "eventId" to eventId,
                "eventTitle" to finalEvent.title,
                "eventDate" to finalEvent.eventDate,
                "eventLocation" to finalEvent.location,
                "eventCategory" to finalEvent.category,
                "coverImageUrl" to finalEvent.coverImageUrl,
                "invitationMessage" to finalEvent.invitationMessage,
                "invitationStatus" to "sent",
                "rsvpStatus" to RsvpStatus.PENDING.name,
                "sentAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            batch.set(inviteRef, invitePayload)

            val notifId = "notif_${UUID.randomUUID().toString().take(12)}"
            val notifRef = db.collection("users").document(recipient.userId)
                .collection("notifications").document(notifId)
            val notifPayload = hashMapOf<String, Any?>(
                "notificationId" to notifId,
                "recipientId" to recipient.userId,
                "senderId" to uid,
                "senderName" to finalEvent.organizerName.ifBlank { "Host" },
                "eventId" to eventId,
                "eventTitle" to finalEvent.title,
                "title" to "New Event Invitation!",
                "message" to "${finalEvent.organizerName} invited you to '${finalEvent.title}' on ${finalEvent.eventDate}",
                "notificationType" to NotificationType.INVITATION.value,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            batch.set(notifRef, notifPayload)
        }

        try {
            batch.commit().await()
            return eventId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, eventDocRef.path)
            throw e
        }
    }

    fun observeHostedEvents(): Flow<List<Event>> = flow {
        val uid = requireUserId()
        val path = "events"
        emitAll(
            db.collection(path)
                .whereEqualTo("organizerId", uid)
                .snapshots()
                .map { it.toObjects(Event::class.java).sortedByDescending { ev -> ev.eventDate } }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePublicEvents(): Flow<List<Event>> = flow {
        val path = "events"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { it.toObjects(Event::class.java).sortedByDescending { ev -> ev.eventDate } }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeEvent(eventId: String): Flow<Event?> = flow {
        val path = "events/$eventId"
        emitAll(
            db.collection("events").document(eventId)
                .snapshots()
                .map { it.toObject(Event::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeReceivedInvitations(): Flow<List<Invitation>> = flow {
        val uid = requireUserId()
        val path = "invitations"
        emitAll(
            db.collectionGroup(path)
                .whereEqualTo("recipientId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Invitation::class.java)
                        .sortedByDescending { it.sentAt?.toDate() }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeEventInvitations(eventId: String): Flow<List<Invitation>> = flow {
        val path = "events/$eventId/invitations"
        emitAll(
            db.collection("events").document(eventId).collection("invitations")
                .snapshots()
                .map { it.toObjects(Invitation::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun respondToInvitation(
        event: Event,
        invitation: Invitation,
        newStatus: RsvpStatus,
        responseNote: String?
    ) {
        val uid = requireUserId()
        val inviteRef = db.collection("events").document(event.eventId)
            .collection("invitations").document(invitation.recipientId)

        val previousStatus = invitation.rsvpStatus
        val isNewResponse = previousStatus != newStatus.name

        val inviteUpdates = mutableMapOf<String, Any?>(
            "rsvpStatus" to newStatus.name,
            "responseMessage" to responseNote?.trim(),
            "respondedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            inviteRef.update(inviteUpdates).await()

            if (isNewResponse) {
                var acceptedDelta = 0L
                var maybeDelta = 0L
                var declinedDelta = 0L

                when (previousStatus) {
                    RsvpStatus.ACCEPTED.name -> acceptedDelta -= 1L
                    RsvpStatus.MAYBE.name -> maybeDelta -= 1L
                    RsvpStatus.DECLINED.name -> declinedDelta -= 1L
                }

                when (newStatus) {
                    RsvpStatus.ACCEPTED -> acceptedDelta += 1L
                    RsvpStatus.MAYBE -> maybeDelta += 1L
                    RsvpStatus.DECLINED -> declinedDelta += 1L
                    RsvpStatus.PENDING -> {}
                }

                val eventRef = db.collection("events").document(event.eventId)
                val eventUpdates = mutableMapOf<String, Any>(
                    "acceptedCount" to FieldValue.increment(acceptedDelta),
                    "maybeCount" to FieldValue.increment(maybeDelta),
                    "declinedCount" to FieldValue.increment(declinedDelta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                eventRef.update(eventUpdates).await()

                val notifId = "notif_${UUID.randomUUID().toString().take(12)}"
                val notifRef = db.collection("users").document(event.organizerId)
                    .collection("notifications").document(notifId)
                val notifPayload = hashMapOf<String, Any?>(
                    "notificationId" to notifId,
                    "recipientId" to event.organizerId,
                    "senderId" to uid,
                    "senderName" to invitation.recipientName,
                    "eventId" to event.eventId,
                    "eventTitle" to event.title,
                    "title" to "RSVP Update: ${newStatus.label}",
                    "message" to "${invitation.recipientName} responded '${newStatus.label}' to '${event.title}'${if (!responseNote.isNullOrBlank()) ": \"$responseNote\"" else ""}",
                    "notificationType" to NotificationType.RSVP_RESPONSE.value,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                notifRef.set(notifPayload).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, inviteRef.path)
            throw e
        }
    }

    suspend fun cancelEvent(event: Event, reason: String) {
        val uid = requireUserId()
        if (event.organizerId != uid) {
            throw IllegalStateException("Only event organizer can cancel an event.")
        }
        val eventRef = db.collection("events").document(event.eventId)
        try {
            eventRef.update(
                mapOf(
                    "eventStatus" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val invitesSnapshot = eventRef.collection("invitations").get().await()
            val invitees = invitesSnapshot.toObjects(Invitation::class.java)

            val batch = db.batch()
            for (invitee in invitees) {
                val notifId = "notif_${UUID.randomUUID().toString().take(12)}"
                val notifRef = db.collection("users").document(invitee.recipientId)
                    .collection("notifications").document(notifId)
                val notifPayload = hashMapOf<String, Any?>(
                    "notificationId" to notifId,
                    "recipientId" to invitee.recipientId,
                    "senderId" to uid,
                    "senderName" to event.organizerName,
                    "eventId" to event.eventId,
                    "eventTitle" to event.title,
                    "title" to "Event Cancelled",
                    "message" to "'${event.title}' has been cancelled by the host. Reason: ${reason.ifBlank { "No reason provided" }}",
                    "notificationType" to NotificationType.EVENT_CANCELLED.value,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                batch.set(notifRef, notifPayload)
            }
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, eventRef.path)
            throw e
        }
    }

    suspend fun sendReminderToPending(event: Event) {
        val uid = requireUserId()
        if (event.organizerId != uid) return
        val eventRef = db.collection("events").document(event.eventId)
        try {
            val invitesSnapshot = eventRef.collection("invitations")
                .whereEqualTo("rsvpStatus", RsvpStatus.PENDING.name)
                .get()
                .await()

            val pendingInvites = invitesSnapshot.toObjects(Invitation::class.java)
            if (pendingInvites.isEmpty()) return

            val batch = db.batch()
            for (invite in pendingInvites) {
                val notifId = "notif_${UUID.randomUUID().toString().take(12)}"
                val notifRef = db.collection("users").document(invite.recipientId)
                    .collection("notifications").document(notifId)
                val notifPayload = hashMapOf<String, Any?>(
                    "notificationId" to notifId,
                    "recipientId" to invite.recipientId,
                    "senderId" to uid,
                    "senderName" to event.organizerName,
                    "eventId" to event.eventId,
                    "eventTitle" to event.title,
                    "title" to "RSVP Reminder",
                    "message" to "Friendly reminder to respond to '${event.title}' scheduled for ${event.eventDate}!",
                    "notificationType" to NotificationType.REMINDER.value,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                batch.set(notifRef, notifPayload)
            }
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, eventRef.path)
            throw e
        }
    }

    fun observeNotifications(): Flow<List<NotificationItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(NotificationItem::class.java)
                        .sortedByDescending { it.createdAt?.toDate() }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("notifications").document(notificationId)
        try {
            docRef.update("isRead", true).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun deleteNotification(notificationId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("notifications").document(notificationId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    private fun sanitizeUsername(fullName: String, userId: String): String {
        val base = fullName.lowercase().replace(Regex("[^a-z0-9]"), "")
        val suffix = userId.take(4).lowercase()
        return if (base.length >= 3) "${base.take(15)}_$suffix" else "user_$suffix"
    }
}
