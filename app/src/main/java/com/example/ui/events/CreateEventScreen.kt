package com.example.ui.events

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.Event
import com.example.model.EventCategories
import com.example.model.UserProfile
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEventScreen(
    currentProfile: UserProfile?,
    allUsers: List<UserProfile>,
    onBackClick: () -> Unit,
    onSubmitEvent: (Event, List<UserProfile>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(EventCategories.all[0]) }
    var description by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("2026-11-20") }
    var startTime by remember { mutableStateOf("18:30") }
    var endTime by remember { mutableStateOf("22:00") }
    var location by remember { mutableStateOf("") }
    var invitationMessage by remember { mutableStateOf("I would love to have you celebrate with us!") }
    var responseDeadline by remember { mutableStateOf("2026-11-18") }
    var guestLimitText by remember { mutableStateOf("0") }
    var visibility by remember { mutableStateOf("public") }

    val selectedRecipients = remember { mutableStateListOf<UserProfile>() }
    var userSearchQuery by remember { mutableStateOf("") }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Cover preset options
    val coverOptions = listOf(
        "Party" to R.drawable.hero_party_banner,
        "Birthday" to R.drawable.birthday_cover,
        "Dinner" to R.drawable.dinner_cover
    )
    var selectedCoverIndex by remember { mutableStateOf(0) }

    val filteredUsers = remember(userSearchQuery, allUsers) {
        if (userSearchQuery.isBlank()) allUsers
        else allUsers.filter {
            it.fullName.contains(userSearchQuery, ignoreCase = true) ||
                    it.username.contains(userSearchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Event & Invites", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Title & Category
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Event Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Event Title *") },
                            placeholder = { Text("e.g. Chloe's 25th Birthday Bash") },
                            leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("event_title_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            EventCategories.all.forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Event Description") },
                            placeholder = { Text("Tell your guests what to expect...") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            }

            // Cover Image Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Cover Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            coverOptions.forEachIndexed { index, option ->
                                val isSelected = selectedCoverIndex == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) IndigoPrimary else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedCoverIndex = index }
                                ) {
                                    Image(
                                        painter = painterResource(id = option.second),
                                        contentDescription = option.first,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = if (isSelected) 0.25f else 0.5f))
                                    )
                                    Text(
                                        text = option.first,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Date, Time & Venue
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Date, Time & Venue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = eventDate,
                            onValueChange = { eventDate = it },
                            label = { Text("Event Date (YYYY-MM-DD) *") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                label = { Text("Start Time *") },
                                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                label = { Text("End Time") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location / Venue Address *") },
                            placeholder = { Text("e.g. 124 Grand Ave or Central Park Pavilion") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("event_location_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = responseDeadline,
                            onValueChange = { responseDeadline = it },
                            label = { Text("RSVP Deadline") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Invitation Message
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Invitation Message to Guests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = invitationMessage,
                            onValueChange = { invitationMessage = it },
                            label = { Text("Personalized Invitation Note *") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            }

            // Recipient Picker Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Recipients",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${selectedRecipients.size} Selected",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = userSearchQuery,
                            onValueChange = { userSearchQuery = it },
                            placeholder = { Text("Search users by name or handle...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (userSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { userSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("search_recipients_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredUsers.isEmpty()) {
                            Text(
                                text = if (allUsers.isEmpty()) "No other registered users in directory yet. You can create the event and invite friends once they join!" else "No matching users found.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredUsers.forEach { user ->
                                    val isSelected = selectedRecipients.any { it.userId == user.userId }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isSelected) {
                                                    selectedRecipients.removeAll { it.userId == user.userId }
                                                } else {
                                                    selectedRecipients.add(user)
                                                }
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(36.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                if (!user.profileImageUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = user.profileImageUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(user.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("@${user.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Surface(
                                                modifier = Modifier.size(24.dp),
                                                shape = CircleShape,
                                                color = if (isSelected) EmeraldSuccess else Color.Transparent,
                                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                                            ) {
                                                if (isSelected) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Preview & Send Action Button
            item {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            errorMessage = "Please enter an event title."
                            return@Button
                        }
                        if (location.isBlank()) {
                            errorMessage = "Please enter an event location."
                            return@Button
                        }
                        if (eventDate.isBlank() || startTime.isBlank()) {
                            errorMessage = "Please enter a valid date and start time."
                            return@Button
                        }
                        errorMessage = null
                        showPreviewDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("preview_and_send_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedRecipients.isNotEmpty()) "Preview & Send (${selectedRecipients.size} Guests)" else "Preview & Create Event",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Invitation Preview & Confirmation Dialog
    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSending) showPreviewDialog = false },
            title = {
                Text(
                    text = "Event & Invitation Preview",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Category: $selectedCategory")
                    Text("Date & Time: $eventDate at $startTime")
                    Text("Location: $location")
                    Text("Personal Note: \"$invitationMessage\"")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recipients: ${if (selectedRecipients.isEmpty()) "No guests selected (Public listing)" else "${selectedRecipients.size} guest(s): " + selectedRecipients.joinToString(", ") { it.fullName }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSending = true
                        val newEvent = Event(
                            organizerId = currentProfile?.userId ?: "",
                            organizerName = currentProfile?.fullName ?: "Host",
                            organizerPhotoUrl = currentProfile?.profileImageUrl,
                            title = title.trim(),
                            description = description.trim(),
                            category = selectedCategory,
                            eventDate = eventDate.trim(),
                            startTime = startTime.trim(),
                            endTime = endTime.trim().ifBlank { null },
                            location = location.trim(),
                            coverImageUrl = coverOptions[selectedCoverIndex].first,
                            invitationMessage = invitationMessage.trim(),
                            responseDeadline = responseDeadline.trim().ifBlank { null },
                            guestLimit = guestLimitText.toLongOrNull() ?: 0L,
                            visibility = visibility,
                            eventStatus = "active"
                        )
                        onSubmitEvent(newEvent, selectedRecipients.toList())
                    },
                    enabled = !isSending,
                    modifier = Modifier.testTag("confirm_send_invitations_btn")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sending...")
                    } else {
                        Text("Confirm & Send")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPreviewDialog = false },
                    enabled = !isSending
                ) {
                    Text("Edit Details")
                }
            }
        )
    }
}
