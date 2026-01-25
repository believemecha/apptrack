package com.example.apptrack.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.apptrack.call.CallInfo
import com.example.apptrack.call.CallType
import com.example.apptrack.call.GroupedCallInfo
import com.example.apptrack.call.groupCallsByContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallManagementScreen(
    currentCall: CallInfo?,
    callHistory: List<CallInfo>,
    onBlockNumber: (String) -> Unit,
    onUnblockNumber: (String) -> Unit,
    isBlocked: (String) -> Boolean,
    onMakeCall: (String) -> Unit = {},
    onOpenDialer: () -> Unit = {},
    onOpenContacts: () -> Unit = {},
    onOpenHistory: (String) -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    onAnswerCall: () -> Unit = {},
    onRejectCall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val groupedCalls = remember(callHistory) {
        groupCallsByContact(callHistory)
    }
    
    Scaffold(
        topBar = {
            // Search Bar Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Search Bar
                    SearchBar(
                        query = "",
                        onQueryChange = {},
                        onSearchClick = onOpenContacts,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenDialer,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Dialer",
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Current Call Card
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
            
            // Call History Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                // History button removed - now using per-row history icons
            }
            
            // Call History List
            if (groupedCalls.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No call history",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(groupedCalls) { groupedCall ->
                        GroupedCallHistoryItem(
                            groupedCall = groupedCall,
                            isBlocked = isBlocked(groupedCall.phoneNumber),
                            onBlock = { onBlockNumber(groupedCall.phoneNumber) },
                            onUnblock = { onUnblockNumber(groupedCall.phoneNumber) },
                            onCall = { onMakeCall(groupedCall.phoneNumber) },
                            onOpenHistory = { onOpenHistory(groupedCall.phoneNumber) },
                            onOpenProfile = { onOpenProfile(groupedCall.phoneNumber) },
                            context = context
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .clickable { onSearchClick() }
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onSearchClick()
                }
            },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        placeholder = { Text("Search contacts...") },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        ),
        readOnly = true,
        interactionSource = interactionSource
    )
}

@Composable
fun GroupedCallHistoryItem(
    groupedCall: GroupedCallInfo,
    isBlocked: Boolean,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onCall: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    context: android.content.Context
) {
    var contactPhoto by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(groupedCall.phoneNumber) {
        contactPhoto = loadContactPhotoForNumber(context, groupedCall.phoneNumber)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact Photo (clickable to open profile)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onOpenProfile() },
                contentAlignment = Alignment.Center
            ) {
                if (contactPhoto != null) {
                    Image(
                        bitmap = contactPhoto!!.asImageBitmap(),
                        contentDescription = groupedCall.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = groupedCall.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Contact Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = groupedCall.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last call type indicator
                    CallTypeIndicator(callType = groupedCall.lastCallType)
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    Text(
                        text = formatCallDate(groupedCall.lastCallTimestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    if (groupedCall.totalCallCount > 1) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${groupedCall.totalCallCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // History Button
            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Call Button
            IconButton(
                onClick = onCall,
                modifier = Modifier.size(48.dp)
            ) {
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

@Composable
fun CallTypeIndicator(callType: CallType) {
    val (text, color) = when (callType) {
        CallType.INCOMING -> "Received" to MaterialTheme.colorScheme.primary
        CallType.OUTGOING -> "Dialed" to MaterialTheme.colorScheme.tertiary
        CallType.MISSED -> "Missed" to MaterialTheme.colorScheme.error
        CallType.REJECTED -> "Rejected" to MaterialTheme.colorScheme.error
        CallType.BLOCKED -> "Blocked" to MaterialTheme.colorScheme.error
    }
    
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

suspend fun loadContactPhotoForNumber(
    context: android.content.Context,
    phoneNumber: String
): Bitmap? = withContext(Dispatchers.IO) {
    if (ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        android.util.Log.w("CallManagementScreen", "READ_CONTACTS permission not granted")
        return@withContext null
    }
    
    try {
        // Normalize phone number (remove spaces, dashes, parentheses)
        val normalizedNumber = phoneNumber.replace(Regex("[^+\\d]"), "")
        
        // Try multiple approaches to find the contact
        var contactId: Long? = null
        var lookupKey: String? = null
        
        // Approach 1: Direct phone lookup
        val lookupUri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
            .appendPath(normalizedNumber)
            .build()
        
        val lookupCursor = context.contentResolver.query(
            lookupUri,
            arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY),
            null,
            null,
            null
        )
        
        lookupCursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(ContactsContract.PhoneLookup._ID)
                val lookupKeyIndex = it.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY)
                if (idIndex >= 0) contactId = it.getLong(idIndex)
                if (lookupKeyIndex >= 0) lookupKey = it.getString(lookupKeyIndex)
            }
        }
        
        // If not found, try with original number (in case it has formatting)
        if (contactId == null && phoneNumber != normalizedNumber) {
            val originalLookupUri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                .appendPath(phoneNumber)
                .build()
            
            val originalCursor = context.contentResolver.query(
                originalLookupUri,
                arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY),
                null,
                null,
                null
            )
            
            originalCursor?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    val lookupKeyIndex = it.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY)
                    if (idIndex >= 0) contactId = it.getLong(idIndex)
                    if (lookupKeyIndex >= 0) lookupKey = it.getString(lookupKeyIndex)
                }
            }
        }
        
        if (contactId == null) {
            android.util.Log.d("CallManagementScreen", "Contact not found for number: $phoneNumber")
            return@withContext null
        }
        
        // Load photo using contact ID
        val photoUri = if (lookupKey != null) {
            ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
        } else {
            android.net.Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_URI,
                contactId.toString()
            )
        }
        
        val photoStream = ContactsContract.Contacts.openContactPhotoInputStream(
            context.contentResolver,
            photoUri,
            true // prefer high-res
        ) ?: ContactsContract.Contacts.openContactPhotoInputStream(
            context.contentResolver,
            photoUri,
            false // fallback to thumbnail
        )
        
        photoStream?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream)
            if (bitmap != null) {
                android.util.Log.d("CallManagementScreen", "Loaded photo for contact ID: $contactId")
            }
            bitmap
        }
    } catch (e: Exception) {
        android.util.Log.e("CallManagementScreen", "Failed to load contact photo for $phoneNumber: ${e.message}", e)
        null
    }
}

@Composable
fun CurrentCallCard(
    call: CallInfo,
    onBlock: () -> Unit,
    isBlocked: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.contactName ?: call.phoneNumber,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = call.callType.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onBlock) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = if (isBlocked) "Unblock" else "Block",
                    tint = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun IncomingCallCard(
    call: CallInfo,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onBlock: () -> Unit,
    isBlocked: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = call.contactName ?: call.phoneNumber,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Incoming Call",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FloatingActionButton(
                    onClick = onReject,
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject")
                }
                
                FloatingActionButton(
                    onClick = onAnswer,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Answer")
                }
            }
        }
    }
}

fun formatCallDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000} min ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        diff < 604800000 -> "${diff / 86400000} days ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}

fun formatCallDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}
