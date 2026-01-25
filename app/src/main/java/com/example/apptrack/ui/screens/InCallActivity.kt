package com.example.apptrack.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.apptrack.call.CallControlManager
import com.example.apptrack.call.CallType
import com.example.apptrack.ui.theme.AppTrackTheme
import android.telecom.Call
import android.util.Log
import java.util.Locale

class InCallActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "InCallActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if this is a close intent
        if (intent.action == "CLOSE_ACTIVITY") {
            Log.d(TAG, "Received close intent, finishing activity")
            finish()
            return
        }
        
        // Initialize CallControlManager if not already done
        CallControlManager.initialize(this)
        
        // Monitor call state and close when disconnected
        // Use a handler to check for calls with a delay (in case InCallService isn't bound yet)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var checkCount = 0
        val maxChecks = 10 // Check for 5 seconds (10 * 500ms)
        
        val checkForCall = object : Runnable {
            override fun run() {
                val inCallService = CallControlManager.getInCallService()
                val activeCall = inCallService?.calls?.firstOrNull { 
                    it.state == Call.STATE_ACTIVE || 
                    it.state == Call.STATE_DIALING || 
                    it.state == Call.STATE_RINGING ||
                    it.state == Call.STATE_CONNECTING
                }
                
                if (activeCall != null) {
                    Log.d(TAG, "Active call found, registering callback")
                    // Register callback to detect when call is disconnected
                    activeCall.registerCallback(object : Call.Callback() {
                        override fun onStateChanged(call: Call, state: Int) {
                            Log.d(TAG, "Call state changed in activity: $state")
                            if (state == Call.STATE_DISCONNECTED) {
                                Log.d(TAG, "Call disconnected, closing activity")
                                runOnUiThread {
                                    finish()
                                }
                            }
                        }
                    }, android.os.Handler(android.os.Looper.getMainLooper()))
                } else {
                    checkCount++
                    if (checkCount < maxChecks) {
                        // Keep checking - InCallService might not be bound yet
                        Log.d(TAG, "No active call found yet, checking again... ($checkCount/$maxChecks)")
                        handler.postDelayed(this, 500)
                    } else {
                        // After max checks, if still no call, keep activity open anyway
                        // (call might be using system dialer's InCallService)
                        Log.d(TAG, "No active call found after $maxChecks checks, keeping activity open")
                    }
                }
            }
        }
        handler.postDelayed(checkForCall, 100) // Start checking after 100ms
        
        setContent {
            AppTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InCallScreen(
                        phoneNumber = intent.getStringExtra("phoneNumber") ?: "",
                        contactName = intent.getStringExtra("contactName"),
                        callType = CallType.valueOf(intent.getStringExtra("callType") ?: "INCOMING"),
                        onAnswer = { 
                            CallControlManager.answerCall()
                        },
                        onReject = { 
                            CallControlManager.endCall()
                            finish() 
                        },
                        onEndCall = { 
                            CallControlManager.endCall()
                            finish() 
                        }
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        CallControlManager.setAudioModeNormal()
    }
}

@Composable
fun InCallScreen(
    phoneNumber: String,
    contactName: String?,
    callType: CallType,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onEndCall: () -> Unit
) {
    var isMuted by remember { mutableStateOf(CallControlManager.isMuted()) }
    var isSpeakerOn by remember { mutableStateOf(CallControlManager.isSpeakerOn()) }
    var callDuration by remember { mutableStateOf(0L) }
    var isCallActive by remember { mutableStateOf(false) }
    
    // Sync state and update call duration periodically
    // Also check if call is actually active (answered)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500) // Update every 500ms for responsive UI
            isMuted = CallControlManager.isMuted()
            isSpeakerOn = CallControlManager.isSpeakerOn()
            callDuration = CallControlManager.getCallDuration()
            
            // Check if call is actually active (answered)
            val activeCall = CallControlManager.getActiveCall()
            isCallActive = activeCall?.state == Call.STATE_ACTIVE || callDuration > 0
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        // Caller Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = contactName ?: phoneNumber,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            if (contactName != null) {
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            // Show status text
            if (!isCallActive && callType == CallType.INCOMING) {
                Text(
                    text = "Incoming call",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else if (!isCallActive && callType == CallType.OUTGOING) {
                Text(
                    text = "Calling...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            // Show call duration for active calls
            if (isCallActive && callDuration > 0) {
                Text(
                    text = formatCallDurationTimer(callDuration),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Call Controls
        // Show incoming call UI only if call is not yet active
        if (callType == CallType.INCOMING && !isCallActive) {
            // Incoming call - Answer/Reject buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Reject Button
                FloatingActionButton(
                    onClick = onReject,
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Reject",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Answer Button
                FloatingActionButton(
                    onClick = onAnswer,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Answer",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        } else {
            // Active call - Full controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Secondary controls (Mute, Speaker, etc.)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Mute Button
                    IconButton(
                        onClick = {
                            isMuted = CallControlManager.toggleMute()
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Text(
                            text = if (isMuted) "🔇" else "🎤",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    // Speaker Button
                    IconButton(
                        onClick = {
                            isSpeakerOn = CallControlManager.toggleSpeaker()
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Text(
                            text = if (isSpeakerOn) "🔊" else "📢",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.size(32.dp),
                            color = if (isSpeakerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Keypad Button (placeholder)
                    IconButton(
                        onClick = { /* TODO: Show keypad */ },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Text(
                            text = "⌨️",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // End Call Button
                FloatingActionButton(
                    onClick = onEndCall,
                    containerColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "End Call",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

fun formatCallDurationTimer(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        else -> String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
