package com.example.ui.invitations

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.Event
import com.example.model.Invitation
import com.example.model.RsvpStatus
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseDecline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitationsScreen(
    invitations: List<Invitation>,
    onInvitationClick: (Invitation) -> Unit,
    onSubmitRsvp: (Invitation, RsvpStatus, String?) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Awaiting", "Accepted", "Maybe/Declined", "All")

    var respondingInvite by remember { mutableStateOf<Invitation?>(null) }
    var rsvpStatusChoice by remember { mutableStateOf(RsvpStatus.ACCEPTED) }
    var rsvpNoteText by remember { mutableStateOf("") }

    val filteredInvitations = remember(invitations, selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> invitations.filter { it.rsvpStatus == RsvpStatus.PENDING.name }
            1 -> invitations.filter { it.rsvpStatus == RsvpStatus.ACCEPTED.name }
            2 -> invitations.filter { it.rsvpStatus == RsvpStatus.MAYBE.name || it.rsvpStatus == RsvpStatus.DECLINED.name }
            else -> invitations
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Invitations", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (filteredInvitations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Mail,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No invitations in this category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "When other users invite you to events, they will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredInvitations) { invite ->
                        InvitationItemCard(
                            invitation = invite,
                            onClick = { onInvitationClick(invite) },
                            onRespondClick = {
                                respondingInvite = invite
                                rsvpStatusChoice = when (invite.rsvpStatus) {
                                    RsvpStatus.ACCEPTED.name -> RsvpStatus.ACCEPTED
                                    RsvpStatus.MAYBE.name -> RsvpStatus.MAYBE
                                    RsvpStatus.DECLINED.name -> RsvpStatus.DECLINED
                                    else -> RsvpStatus.ACCEPTED
                                }
                                rsvpNoteText = invite.responseMessage ?: ""
                            }
                        )
                    }
                }
            }
        }
    }

    // Detailed RSVP Response Modal
    respondingInvite?.let { invite ->
        val presetNotes = when (rsvpStatusChoice) {
            RsvpStatus.ACCEPTED -> listOf(
                "Thanks for inviting me! I'll be there.",
                "Looking forward to celebrating with you!",
                "Can't wait! Count me in."
            )
            RsvpStatus.MAYBE -> listOf(
                "I have some work that day. I'll confirm later.",
                "Tentative yes, checking my schedule!",
                "Will let you know soon."
            )
            RsvpStatus.DECLINED -> listOf(
                "Sorry, I won't be able to make it.",
                "Unfortunately have a prior commitment. Have fun!",
                "Sending best wishes, so sorry to miss it!"
            )
            else -> emptyList()
        }

        AlertDialog(
            onDismissRequest = { respondingInvite = null },
            title = {
                Text(
                    text = "Respond to Invitation",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = invite.eventTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Hosted by ${invite.senderName} • ${invite.eventDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your Attendance Status:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusChoiceChip(
                            label = "Yes",
                            isSelected = rsvpStatusChoice == RsvpStatus.ACCEPTED,
                            color = EmeraldSuccess,
                            modifier = Modifier.weight(1f),
                            onClick = { rsvpStatusChoice = RsvpStatus.ACCEPTED }
                        )
                        StatusChoiceChip(
                            label = "Maybe",
                            isSelected = rsvpStatusChoice == RsvpStatus.MAYBE,
                            color = AmberAccent,
                            modifier = Modifier.weight(1f),
                            onClick = { rsvpStatusChoice = RsvpStatus.MAYBE }
                        )
                        StatusChoiceChip(
                            label = "No",
                            isSelected = rsvpStatusChoice == RsvpStatus.DECLINED,
                            color = RoseDecline,
                            modifier = Modifier.weight(1f),
                            onClick = { rsvpStatusChoice = RsvpStatus.DECLINED }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Quick Response Note:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetNotes.forEach { note ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { rsvpNoteText = note },
                                shape = RoundedCornerShape(8.dp),
                                color = if (rsvpNoteText == note) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = note,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = rsvpNoteText,
                        onValueChange = { rsvpNoteText = it },
                        label = { Text("Custom Note to Organizer (Optional)") },
                        placeholder = { Text("Write a message...") },
                        modifier = Modifier.fillMaxWidth().testTag("rsvp_note_input"),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentInvite = respondingInvite ?: return@Button
                        respondingInvite = null
                        onSubmitRsvp(currentInvite, rsvpStatusChoice, rsvpNoteText.ifBlank { null })
                    },
                    modifier = Modifier.testTag("submit_rsvp_response_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (rsvpStatusChoice) {
                            RsvpStatus.ACCEPTED -> EmeraldSuccess
                            RsvpStatus.MAYBE -> AmberAccent
                            RsvpStatus.DECLINED -> RoseDecline
                            else -> IndigoPrimary
                        }
                    )
                ) {
                    Text("Save Response", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { respondingInvite = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusChoiceChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) color else color.copy(alpha = 0.1f),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isSelected) Color.White else color,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun InvitationItemCard(
    invitation: Invitation,
    onClick: () -> Unit,
    onRespondClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = invitation.eventCategory,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Invited by ${invitation.senderName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (invitation.rsvpStatus) {
                        RsvpStatus.ACCEPTED.name -> EmeraldSuccess.copy(alpha = 0.12f)
                        RsvpStatus.MAYBE.name -> AmberAccent.copy(alpha = 0.12f)
                        RsvpStatus.DECLINED.name -> RoseDecline.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when (invitation.rsvpStatus) {
                            RsvpStatus.ACCEPTED.name -> "✓ Accepted"
                            RsvpStatus.MAYBE.name -> "? Maybe"
                            RsvpStatus.DECLINED.name -> "✗ Declined"
                            else -> "• Awaiting"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (invitation.rsvpStatus) {
                            RsvpStatus.ACCEPTED.name -> EmeraldSuccess
                            RsvpStatus.MAYBE.name -> AmberAccent
                            RsvpStatus.DECLINED.name -> RoseDecline
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = invitation.eventTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text(invitation.eventDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(14.dp))
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text(invitation.eventLocation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (!invitation.invitationMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"${invitation.invitationMessage}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!invitation.responseMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Your note: \"${invitation.responseMessage}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRespondClick,
                modifier = Modifier.fillMaxWidth().height(40.dp).testTag("change_rsvp_btn"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (invitation.rsvpStatus == RsvpStatus.PENDING.name) IndigoPrimary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (invitation.rsvpStatus == RsvpStatus.PENDING.name) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    text = if (invitation.rsvpStatus == RsvpStatus.PENDING.name) "Respond to Invitation" else "Change Response",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
