package com.example.apptrack.ui.recents

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apptrack.call.CallType
import com.example.apptrack.call.GroupedCallInfo
import com.example.apptrack.call.groupCallsByContact
import com.example.apptrack.ui.components.DateBucket
import com.example.apptrack.ui.components.GroupedByDate
import com.example.apptrack.ui.components.groupRecentsByDate
import com.example.apptrack.ui.screens.IncomingCallCard
import com.example.apptrack.ui.screens.formatCallDate
import com.example.apptrack.ui.screens.loadContactPhotoForNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    callHistory: List<com.example.apptrack.call.CallInfo>,
    currentCall: com.example.apptrack.call.CallInfo? = null,
    onSearchClick: () -> Unit = {},
    onOpenCallAssistantSettings: () -> Unit = {},
    onOpenCallDetails: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onMakeCall: (String) -> Unit,
    onBlockNumber: (String) -> Unit,
    onUnblockNumber: (String) -> Unit = {},
    onAnswerCall: () -> Unit = {},
    onRejectCall: () -> Unit = {},
    onDeleteLog: ((String) -> Unit)? = null,
    isBlocked: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var optionsFor by remember { mutableStateOf<GroupedCallInfo?>(null) }
    val groupedCalls = remember(callHistory) { groupCallsByContact(callHistory) }
    val sections = remember(groupedCalls) { groupRecentsByDate(groupedCalls) }

    optionsFor?.let { grouped ->
        RecentsRowOptionsModal(
            groupedCall = grouped,
            isBlocked = isBlocked(grouped.phoneNumber),
            onDismiss = { optionsFor = null },
            onCall = { onMakeCall(grouped.phoneNumber); optionsFor = null },
            onMessage = { /* Message intent - optional */ optionsFor = null },
            onBlock = { onBlockNumber(grouped.phoneNumber); optionsFor = null },
            onUnblock = { onUnblockNumber(grouped.phoneNumber); optionsFor = null },
            onDelete = { onDeleteLog?.invoke(grouped.phoneNumber); optionsFor = null }
        )
    }

    if (sections.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "No recent calls",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            currentCall?.let { call ->
                if (call.callType == CallType.INCOMING) {
                    IncomingCallCard(
                        call = call,
                        onAnswer = onAnswerCall,
                        onReject = onRejectCall,
                        onBlock = { onBlockNumber(call.phoneNumber) },
                        isBlocked = isBlocked(call.phoneNumber)
                    )
                }
            }
            // Search bar (like reference: hamburger, placeholder, mic)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onOpenCallAssistantSettings) {
                        Icon(Icons.Default.Menu, contentDescription = "Call Assistant settings")
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onSearchClick() },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Search contacts",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Phone, contentDescription = "Voice search")
                    }
                }
            }
            // Filter chips: All, Missed, Contacts, Non-spam
            var selectedFilter by remember { mutableStateOf("All") }
            val filterChips = listOf("All", "Missed", "Contacts", "Non-spam")
            val filteredSections by remember {
                derivedStateOf {
                    val filter = selectedFilter
                    sections.map { (bucket, items) ->
                        bucket to items.filter { g ->
                            when (filter) {
                                "Missed" -> g.hasMissedCalls
                                "Contacts" -> g.contactName != null
                                "All", "Non-spam" -> true
                                else -> true
                            }
                        }
                    }.filter { it.second.isNotEmpty() }
                }
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterChips, key = { it }) { chip ->
                    FilterChip(
                        selected = selectedFilter == chip,
                        onClick = { selectedFilter = chip },
                        label = { Text(chip) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
            // Favourites strip – scrolls with the list (not fixed at top)
            if (groupedCalls.isNotEmpty()) {
                item(key = "favourites_header") {
                    Text(
                        "Favourites",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
                item(key = "favourites_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        groupedCalls.take(6).forEach { grouped ->
                            FavouriteContactChip(
                                groupedCall = grouped,
                                context = context,
                                onCall = { onMakeCall(grouped.phoneNumber) },
                                onProfile = { onOpenProfile(grouped.phoneNumber) }
                            )
                        }
                    }
                }
            }
            filteredSections.forEach { (bucket, items) ->
                item(key = "header_${bucket.label}") {
                    Text(
                        text = bucket.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
                items(
                    items = items,
                    key = { it.phoneNumber }
                ) { grouped ->
                    RecentsRow(
                        groupedCall = grouped,
                        context = context,
                        isBlocked = isBlocked(grouped.phoneNumber),
                        onClick = { onOpenCallDetails(grouped.phoneNumber) },
                        onLongClick = { optionsFor = grouped },
                        onOpenProfile = { onOpenProfile(grouped.phoneNumber) },
                        onCall = { onMakeCall(grouped.phoneNumber) },
                        onOpenHistory = { onOpenCallDetails(grouped.phoneNumber) }
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun FavouriteContactChip(
    groupedCall: GroupedCallInfo,
    context: android.content.Context,
    onCall: () -> Unit,
    onProfile: () -> Unit
) {
    var contactPhoto by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(groupedCall.phoneNumber) {
        contactPhoto = loadContactPhotoForNumber(context, groupedCall.phoneNumber)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onProfile() }
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (contactPhoto != null) {
                    androidx.compose.foundation.Image(
                        bitmap = contactPhoto!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = groupedCall.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            IconButton(
                onClick = onCall,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Call",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = groupedCall.displayName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecentsRow(
    groupedCall: GroupedCallInfo,
    context: android.content.Context,
    isBlocked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOpenProfile: () -> Unit,
    onCall: () -> Unit,
    onOpenHistory: () -> Unit
) {
    var contactPhoto by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(groupedCall.phoneNumber) {
        contactPhoto = loadContactPhotoForNumber(context, groupedCall.phoneNumber)
    }

    // Reference layout: avatar left, name + (call type + Mobile + time), phone right
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar first (click -> profile)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onOpenProfile() },
                contentAlignment = Alignment.Center
            ) {
                if (contactPhoto != null) {
                    androidx.compose.foundation.Image(
                        bitmap = contactPhoto!!.asImageBitmap(),
                        contentDescription = groupedCall.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = groupedCall.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = groupedCall.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (groupedCall.totalCallCount > 1) {
                        Text(
                            text = "(${groupedCall.totalCallCount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    if (groupedCall.hasMissedCalls) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallTypeIcon(
                        callType = groupedCall.lastCallType,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = callTypeLabel(groupedCall.lastCallType),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (groupedCall.lastCallType == CallType.MISSED) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (groupedCall.lastCallType == CallType.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Mobile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatCallDate(groupedCall.lastCallTimestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(onClick = onCall, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Call",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun callTypeLabel(callType: CallType): String = when (callType) {
    CallType.INCOMING -> "Incoming"
    CallType.OUTGOING -> "Outgoing"
    CallType.MISSED -> "Missed call"
    CallType.REJECTED -> "Rejected"
    CallType.BLOCKED -> "Blocked"
}

@Composable
private fun CallTypeIcon(
    callType: CallType,
    modifier: Modifier = Modifier
) {
    val pair = when (callType) {
        CallType.INCOMING -> Icons.Default.Call to MaterialTheme.colorScheme.primary
        CallType.OUTGOING -> Icons.Default.Call to MaterialTheme.colorScheme.tertiary
        CallType.MISSED -> Icons.Default.Call to MaterialTheme.colorScheme.error
        CallType.REJECTED -> Icons.Default.Call to MaterialTheme.colorScheme.error
        CallType.BLOCKED -> Icons.Default.Call to MaterialTheme.colorScheme.error
    }
    Icon(
        imageVector = pair.first,
        contentDescription = null,
        modifier = modifier,
        tint = pair.second
    )
}

@Composable
private fun RecentsRowOptionsModal(
    groupedCall: GroupedCallInfo,
    isBlocked: Boolean,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(groupedCall.displayName) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    groupedCall.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onCall) { Text("Call") }
                TextButton(onClick = onMessage) { Text("Message") }
                TextButton(onClick = { if (isBlocked) onUnblock() else onBlock() }) {
                    Text(if (isBlocked) "Unblock" else "Block")
                }
                TextButton(onClick = onDelete) { Text("Delete log") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
