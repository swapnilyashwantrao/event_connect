package com.example.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.EventConnectRepository
import com.example.model.Event
import com.example.model.Invitation
import com.example.model.RsvpStatus
import com.example.model.UserProfile
import com.example.ui.auth.signOutUser
import com.example.ui.directory.UserDirectoryScreen
import com.example.ui.events.CreateEventScreen
import com.example.ui.events.EventDetailsScreen
import com.example.ui.events.OrganizerDashboardScreen
import com.example.ui.home.HomeScreen
import com.example.ui.invitations.InvitationsScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.theme.RoseDecline
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object Invitations : Screen()
    object CreateEvent : Screen()
    object Notifications : Screen()
    object Profile : Screen()
    object Directory : Screen()
    data class EventDetails(val eventId: String) : Screen()
    data class OrganizerDashboard(val eventId: String) : Screen()
}

@Composable
fun AppNavigation(
    currentUser: FirebaseUser,
    repository: EventConnectRepository,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isProfileLoading by remember { mutableStateOf(true) }

    var allUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    // Real-time flows from repository
    val hostedEvents by repository.observeHostedEvents().collectAsState(initial = emptyList())
    val receivedInvitations by repository.observeReceivedInvitations().collectAsState(initial = emptyList())
    val notifications by repository.observeNotifications().collectAsState(initial = emptyList())

    val unreadNotificationsCount = notifications.count { !it.isRead }
    val awaitingRsvpCount = receivedInvitations.count { it.rsvpStatus == RsvpStatus.PENDING.name }

    // Initialize or load user profile document in Firestore
    LaunchedEffect(currentUser.uid) {
        try {
            userProfile = repository.getOrCreateUserProfile(
                userId = currentUser.uid,
                fullName = currentUser.displayName ?: "User",
                email = currentUser.email ?: "",
                photoUrl = currentUser.photoUrl?.toString()
            )
        } catch (e: Exception) {
            userProfile = UserProfile(
                userId = currentUser.uid,
                fullName = currentUser.displayName ?: "User",
                username = "user_${currentUser.uid.take(4)}"
            )
        } finally {
            isProfileLoading = false
        }

        // Fetch directory of users
        allUsers = repository.getAllUsers()
    }

    if (isProfileLoading && userProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Scaffold(
        bottomBar = {
            if (currentScreen in listOf(Screen.Home, Screen.Invitations, Screen.CreateEvent, Screen.Notifications, Screen.Profile)) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Home,
                        onClick = { currentScreen = Screen.Home },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.Invitations,
                        onClick = { currentScreen = Screen.Invitations },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (awaitingRsvpCount > 0) {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text("$awaitingRsvpCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Mail, contentDescription = "Invitations")
                            }
                        },
                        label = { Text("Invites", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("nav_invites")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.CreateEvent,
                        onClick = {
                            scope.launch { allUsers = repository.getAllUsers() }
                            currentScreen = Screen.CreateEvent
                        },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Create") },
                        label = { Text("Create", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("nav_create")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.Notifications,
                        onClick = { currentScreen = Screen.Notifications },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationsCount > 0) {
                                        Badge(containerColor = RoseDecline) {
                                            Text("$unreadNotificationsCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        },
                        label = { Text("Alerts", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("nav_alerts")
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.Profile,
                        onClick = { currentScreen = Screen.Profile },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        userProfile = userProfile,
                        hostedEvents = hostedEvents,
                        receivedInvitations = receivedInvitations,
                        notifications = notifications,
                        onCreateEventClick = {
                            scope.launch { allUsers = repository.getAllUsers() }
                            currentScreen = Screen.CreateEvent
                        },
                        onInvitationsClick = { currentScreen = Screen.Invitations },
                        onDiscoverUsersClick = {
                            scope.launch { allUsers = repository.getAllUsers() }
                            currentScreen = Screen.Directory
                        },
                        onNotificationsClick = { currentScreen = Screen.Notifications },
                        onEventClick = { eventId ->
                            currentScreen = Screen.EventDetails(eventId)
                        },
                        onInvitationClick = { invite ->
                            currentScreen = Screen.EventDetails(invite.eventId)
                        },
                        onQuickRsvp = { invite, status ->
                            scope.launch {
                                try {
                                    val ev = hostedEvents.find { it.eventId == invite.eventId }
                                        ?: Event(eventId = invite.eventId, organizerId = invite.senderId, title = invite.eventTitle)
                                    repository.respondToInvitation(ev, invite, status, null)
                                    Toast.makeText(context, "RSVP updated: ${status.label}", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to update RSVP: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                is Screen.Invitations -> {
                    InvitationsScreen(
                        invitations = receivedInvitations,
                        onInvitationClick = { invite ->
                            currentScreen = Screen.EventDetails(invite.eventId)
                        },
                        onSubmitRsvp = { invite, status, note ->
                            scope.launch {
                                try {
                                    val ev = hostedEvents.find { it.eventId == invite.eventId }
                                        ?: Event(eventId = invite.eventId, organizerId = invite.senderId, title = invite.eventTitle)
                                    repository.respondToInvitation(ev, invite, status, note)
                                    Toast.makeText(context, "Response recorded: ${status.label}", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to submit RSVP: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                is Screen.CreateEvent -> {
                    CreateEventScreen(
                        currentProfile = userProfile,
                        allUsers = allUsers,
                        onBackClick = { currentScreen = Screen.Home },
                        onSubmitEvent = { newEvent, recipients ->
                            scope.launch {
                                try {
                                    val eventId = repository.createEvent(newEvent, recipients)
                                    Toast.makeText(context, "Event created & ${recipients.size} invitations sent!", Toast.LENGTH_LONG).show()
                                    currentScreen = Screen.OrganizerDashboard(eventId)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to create event: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                is Screen.Notifications -> {
                    NotificationsScreen(
                        notifications = notifications,
                        onNotificationClick = { notif ->
                            scope.launch {
                                repository.markNotificationAsRead(notif.notificationId)
                            }
                            if (!notif.eventId.isNullOrBlank()) {
                                currentScreen = Screen.EventDetails(notif.eventId)
                            }
                        },
                        onDeleteNotification = { notifId ->
                            scope.launch {
                                repository.deleteNotification(notifId)
                            }
                        }
                    )
                }

                is Screen.Profile -> {
                    ProfileScreen(
                        userProfile = userProfile,
                        hostedCount = hostedEvents.size,
                        invitationsCount = receivedInvitations.size,
                        onUpdateProfile = { name, bio ->
                            scope.launch {
                                try {
                                    repository.updateUserProfile(name, bio, userProfile?.profileImageUrl)
                                    userProfile = userProfile?.copy(fullName = name, bio = bio)
                                    Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onSignOutClick = {
                            signOutUser(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOut,
                                scope = scope
                            )
                        }
                    )
                }

                is Screen.Directory -> {
                    BackHandler { currentScreen = Screen.Home }
                    UserDirectoryScreen(
                        users = allUsers,
                        onBackClick = { currentScreen = Screen.Home },
                        onInviteUserClick = { user ->
                            currentScreen = Screen.CreateEvent
                        }
                    )
                }

                is Screen.EventDetails -> {
                    BackHandler { currentScreen = Screen.Home }
                    val event = hostedEvents.find { it.eventId == screen.eventId }
                    val eventState by repository.observeEvent(screen.eventId).collectAsState(initial = event)
                    val activeEvent = eventState ?: event

                    if (activeEvent != null) {
                        EventDetailsScreen(
                            event = activeEvent,
                            currentUserId = currentUser.uid,
                            onBackClick = { currentScreen = Screen.Home },
                            onOpenOrganizerDashboard = {
                                currentScreen = Screen.OrganizerDashboard(screen.eventId)
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is Screen.OrganizerDashboard -> {
                    BackHandler { currentScreen = Screen.Home }
                    val event = hostedEvents.find { it.eventId == screen.eventId }
                    val eventState by repository.observeEvent(screen.eventId).collectAsState(initial = event)
                    val activeEvent = eventState ?: event
                    val eventInvitations by repository.observeEventInvitations(screen.eventId).collectAsState(initial = emptyList())

                    if (activeEvent != null) {
                        OrganizerDashboardScreen(
                            event = activeEvent,
                            invitations = eventInvitations,
                            onBackClick = { currentScreen = Screen.Home },
                            onRemindPending = {
                                scope.launch {
                                    try {
                                        repository.sendReminderToPending(activeEvent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to send reminders: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onCancelEvent = { reason ->
                                scope.launch {
                                    try {
                                        repository.cancelEvent(activeEvent, reason)
                                        Toast.makeText(context, "Event cancelled and guests notified", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to cancel event: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
