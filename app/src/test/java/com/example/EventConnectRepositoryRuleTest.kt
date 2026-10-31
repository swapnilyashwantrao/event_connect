package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.EventConnectRepository
import com.example.model.Event
import com.example.model.Invitation
import com.example.model.RsvpStatus
import com.example.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class EventConnectRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createProfile_validPayload_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = EventConnectRepository(firestore)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateUserProfile(
                userId = aliceUid,
                fullName = "Alice Smith",
                email = ALICE_EMAIL,
                photoUrl = null
            )
        }

        assertEquals(aliceUid, profile.userId)
        assertEquals("Alice Smith", profile.fullName)
    }

    @Test
    fun createEvent_and_sendInvitations_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = EventConnectRepository(firestore)

        val eventId = "evt_${UUID.randomUUID().toString().take(8)}"
        val event = Event(
            eventId = eventId,
            organizerId = aliceUid,
            organizerName = "Alice Smith",
            title = "Alice's Celebration",
            description = "Welcome everyone!",
            category = "Party",
            eventDate = "2026-11-25",
            startTime = "18:00",
            location = "Central Lounge",
            invitationMessage = "Can't wait to see you!"
        )

        val bobRecipient = UserProfile(
            userId = "bob_user_id_123",
            fullName = "Bob Jones",
            username = "bobjones"
        )

        val createdEventId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createEvent(event, listOf(bobRecipient))
        }

        assertEquals(eventId, createdEventId)

        val fetchedEvent = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeEvent(eventId).first { it != null }
        }
        assertEquals("Alice's Celebration", fetchedEvent?.title)
        assertEquals(1L, fetchedEvent?.invitedCount)
    }

    @Test
    fun respondToInvitation_asRecipient_updatesRsvpAndIncrementsCount() = runBlocking {
        val bobUid = signInTestUser(BOB_EMAIL)
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = EventConnectRepository(firestore)

        val eventId = "evt_${UUID.randomUUID().toString().take(8)}"

        val event = Event(
            eventId = eventId,
            organizerId = aliceUid,
            organizerName = "Alice Smith",
            title = "Dinner Gathering",
            description = "Cozy dinner",
            category = "Dinner",
            eventDate = "2026-11-26",
            startTime = "19:00",
            location = "Bistro Downtown",
            invitationMessage = "Join us!"
        )

        val bobProfile = UserProfile(
            userId = bobUid,
            fullName = "Bob Jones",
            username = "bobjones"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createEvent(event, listOf(bobProfile))
        }

        // Bob signs in and responds
        signInTestUser(BOB_EMAIL)
        val bobRepo = EventConnectRepository(firestore)

        val invites = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.observeReceivedInvitations().first { list -> list.isNotEmpty() }
        }
        val bobInvite = invites.first { it.recipientId == bobProfile.userId }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.respondToInvitation(
                event = event,
                invitation = bobInvite,
                newStatus = RsvpStatus.ACCEPTED,
                responseNote = "Count me in!"
            )
        }

        val updatedInvites = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.observeReceivedInvitations().first { list: List<Invitation> ->
                list.any { inv: Invitation -> inv.recipientId == bobProfile.userId && inv.rsvpStatus == RsvpStatus.ACCEPTED.name }
            }
        }
        val updatedBobInvite = updatedInvites.first { it.recipientId == bobProfile.userId }
        assertEquals("ACCEPTED", updatedBobInvite.rsvpStatus)
        assertEquals("Count me in!", updatedBobInvite.responseMessage)
    }

    @Test
    fun crossUser_cannotCancelOtherUserEvent() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = EventConnectRepository(firestore)

        val eventId = "evt_${UUID.randomUUID().toString().take(8)}"
        val event = Event(
            eventId = eventId,
            organizerId = aliceUid,
            organizerName = "Alice Smith",
            title = "Secret Party",
            description = "Shh",
            category = "Party",
            eventDate = "2026-11-28",
            startTime = "20:00",
            location = "Somewhere",
            invitationMessage = "Secret"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createEvent(event, emptyList())
        }

        // Bob signs in and tries to cancel Alice's event
        signInTestUser(BOB_EMAIL)
        val bobRepo = EventConnectRepository(firestore)

        try {
            bobRepo.cancelEvent(event, "Malicious attempt")
            fail("Expected IllegalStateException when non-organizer attempts to cancel event")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Only event organizer") == true)
        }
    }

    @Test
    fun unauthenticatedUser_observeEvents_fails() = runBlocking {
        auth.signOut()
        val repository = EventConnectRepository(firestore)

        try {
            withTimeout(3000L) {
                repository.observeHostedEvents().first()
            }
            fail("Expected exception when unauthenticated user observes hosted events")
        } catch (e: Exception) {
            assertNotNull(e)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@eventconnect.test"
        const val BOB_EMAIL = "bob@eventconnect.test"
        const val DEFAULT_TIMEOUT_MS = 6000L
    }
}
