package com.henry.budgetmvp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.henry.budgetmvp.data.HouseholdInvite
import com.henry.budgetmvp.data.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdPage(
    currentUserId: String,
    members: List<UserProfile>,
    pendingInvites: List<HouseholdInvite>,
    onInviteMember: (String) -> Unit,
    onAcceptInvite: (HouseholdInvite) -> Unit,
    onDeclineInvite: (HouseholdInvite) -> Unit,
    onLeaveHousehold: () -> Unit
) {
    var inviteEmail by remember { mutableStateOf("") }
    var showLeaveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. INCOMING INVITES SECTION (Only shows if there are invites)
        if (pendingInvites.isNotEmpty()) {
            Text(
                text = "INCOMING INVITATIONS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary
            )
            pendingInvites.forEach { invite ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Invite from ${invite.fromEmail}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Join their shared budget",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { onDeclineInvite(invite) }) {
                                Icon(Icons.Default.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(
                                onClick = { onAcceptInvite(invite) },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }

        // 2. MEMBERS LIST SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "HOUSEHOLD MEMBERS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                members.forEach { member ->
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (member.userId == currentUserId) "You" else member.email,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (member.userId == member.householdId) {
                                Text("Owner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    if (member != members.last()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // 3. INVITE NEW MEMBER SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "INVITE A SPOUSE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                OutlinedTextField(
                    value = inviteEmail,
                    onValueChange = { inviteEmail = it },
                    label = { Text("Spouse's Email") },
                    placeholder = { Text("email@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) }
                )
                Button(
                    onClick = {
                        if (inviteEmail.isNotBlank()) {
                            onInviteMember(inviteEmail)
                            inviteEmail = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = inviteEmail.contains("@")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SEND INVITATION")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 4. RESET ACTION
        OutlinedButton(
            onClick = { showLeaveDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Text("RESET TO INDIVIDUAL ACCOUNT", fontWeight = FontWeight.Bold)
        }

        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                title = { Text("Leave Household") },
                text = { Text("Are you sure you want to leave this shared household? You will return to your own private budget.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onLeaveHousehold()
                            showLeaveDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("LEAVE HOUSEHOLD")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveDialog = false }) {
                        Text("CANCEL")
                    }
                }
            )
        }
    }
}
