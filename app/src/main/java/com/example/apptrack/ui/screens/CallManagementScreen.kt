package com.example.apptrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.apptrack.call.CallInfo
import com.example.apptrack.call.CallType
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
    onAnswerCall: () -> Unit = {},
    onRejectCall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call Manager") },
                actions = {
                    IconButton(onClick = { /* Open settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenDialer
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Open Dialer")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
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
                } else {
                    CurrentCallCard(
                        call = call,
                        onBlock = { onBlockNumber(call.phoneNumber) },
                        isBlocked = isBlocked(call.phoneNumber)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Call History Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Call History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${callHistory.size} calls",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Call History List
            if (callHistory.isEmpty()) {
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(callHistory.sortedByDescending { it.timestamp }) { call ->
                        CallHistoryItem(
                            call = call,
                            isBlocked = isBlocked(call.phoneNumber),
                            onBlock = { onBlockNumber(call.phoneNumber) },
                            onUnblock = { onUnblockNumber(call.phoneNumber) },
                            onCall = { onMakeCall(call.phoneNumber) }
                        )
                    }
                }
            }
        }
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
                // Reject Button
                FloatingActionButton(
                    onClick = onReject,
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject")
                }
                
                // Answer Button
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

@Composable
fun CallHistoryItem(
    call: CallInfo,
    isBlocked: Boolean,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Call Type Icon
            Icon(
                imageVector = when (call.callType) {
                    CallType.INCOMING -> Icons.Default.Phone
                    CallType.OUTGOING -> Icons.Default.Phone
                    CallType.MISSED -> Icons.Default.Close
                    CallType.REJECTED -> Icons.Default.Close
                    CallType.BLOCKED -> Icons.Default.Close
                },
                contentDescription = null,
                tint = when (call.callType) {
                    CallType.INCOMING -> MaterialTheme.colorScheme.primary
                    CallType.OUTGOING -> MaterialTheme.colorScheme.primary
                    CallType.MISSED -> MaterialTheme.colorScheme.error
                    CallType.REJECTED -> MaterialTheme.colorScheme.error
                    CallType.BLOCKED -> MaterialTheme.colorScheme.error
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.contactName ?: call.phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCallDate(call.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (call.duration > 0) {
                        Text(
                            text = "• ${formatCallDuration(call.duration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCall) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = if (isBlocked) onUnblock else onBlock) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = if (isBlocked) "Unblock" else "Block",
                        tint = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
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
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
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
