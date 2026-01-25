package com.example.apptrack.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.apptrack.call.CallControlManager
import com.example.apptrack.call.CallType
import com.example.apptrack.call.ManualCallRecorder
import com.example.apptrack.ui.theme.AppTrackTheme
import android.telecom.Call
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.delay

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
        
        // Enable showing over lock screen and turn screen on
        // This allows the call UI to appear even when phone is locked
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        
        // Initialize CallControlManager if not already done
        CallControlManager.initialize(this)
        
        // Monitor call state and close when disconnected
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var checkCount = 0
        val maxChecks = 10
        
        val checkForCall = object : Runnable {
            override fun run() {
                try {
                    val inCallService = CallControlManager.getInCallService()
                    if (inCallService == null) {
                        Log.w(TAG, "InCallService is null, will retry...")
                        checkCount++
                        if (checkCount < maxChecks) {
                            handler.postDelayed(this, 500)
                        } else {
                            Log.w(TAG, "InCallService still null after $maxChecks checks, keeping activity open")
                        }
                        return
                    }
                    
                    val activeCall = inCallService.calls?.firstOrNull { 
                        try {
                            val state = it.state
                            state == Call.STATE_ACTIVE || 
                            state == Call.STATE_DIALING || 
                            state == Call.STATE_RINGING ||
                            state == Call.STATE_CONNECTING
                        } catch (e: Exception) {
                            Log.e(TAG, "Error checking call state: ${e.message}", e)
                            false
                        }
                    }
                    
                    if (activeCall != null) {
                        Log.d(TAG, "Active call found, registering callback")
                        try {
                            activeCall.registerCallback(object : Call.Callback() {
                                override fun onStateChanged(call: Call, state: Int) {
                                    Log.d(TAG, "Call state changed in activity: $state")
                                    if (state == Call.STATE_DISCONNECTED) {
                                        Log.d(TAG, "Call disconnected, closing activity")
                                        runOnUiThread {
                                            if (!isFinishing) {
                                                try {
                                                    finish()
                                                } catch (e: Exception) {
                                                    Log.e(TAG, "Error finishing activity: ${e.message}", e)
                                                }
                                            }
                                        }
                                    }
                                }
                            }, android.os.Handler(android.os.Looper.getMainLooper()))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error registering call callback: ${e.message}", e)
                        }
                    } else {
                        checkCount++
                        if (checkCount < maxChecks) {
                            Log.d(TAG, "No active call found yet, checking again... ($checkCount/$maxChecks)")
                            handler.postDelayed(this, 500)
                        } else {
                            Log.d(TAG, "No active call found after $maxChecks checks, keeping activity open")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in checkForCall: ${e.message}", e)
                    // Don't crash - just retry
                    checkCount++
                    if (checkCount < maxChecks) {
                        handler.postDelayed(this, 500)
                    }
                }
            }
        }
        handler.postDelayed(checkForCall, 100)
        
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
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(CallControlManager.isMuted()) }
    var isSpeakerOn by remember { mutableStateOf(CallControlManager.isSpeakerOn()) }
    var callDuration by remember { mutableStateOf(0L) }
    var isCallActive by remember { mutableStateOf(false) }
    var contactPhoto by remember { mutableStateOf<Bitmap?>(null) }
    var isOnHold by remember { mutableStateOf(false) }
        var showKeypad by remember { mutableStateOf(false) }
        var dtmfSequence by remember { mutableStateOf("") } // Display DTMF sequence
        val callRecorder = remember { ManualCallRecorder.getInstance(context) }
        var isRecording by remember { mutableStateOf(false) }
        
        // Load contact photo
    LaunchedEffect(phoneNumber) {
        contactPhoto = getContactPhoto(context, phoneNumber)
    }
    
    // Sync state and update call duration periodically
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            isMuted = CallControlManager.isMuted()
            isSpeakerOn = CallControlManager.isSpeakerOn()
                callDuration = CallControlManager.getCallDuration()
                dtmfSequence = CallControlManager.getDtmfDigits() // Update DTMF sequence display
                isRecording = callRecorder.isRecording() // Update recording state
                
                val activeCall = CallControlManager.getActiveCall()
            isCallActive = activeCall?.state == Call.STATE_ACTIVE || callDuration > 0
            isOnHold = activeCall?.state == Call.STATE_HOLDING
        }
    }
    
    // Cleanup recording when call ends
    LaunchedEffect(isCallActive) {
        if (!isCallActive && isRecording) {
            val recordingPath = callRecorder.stopRecording()
            if (recordingPath != null) {
                Log.d("InCallActivity", "Call ended, recording saved: $recordingPath")
            }
            isRecording = false
        }
    }
    
    // Dark background like the image
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)) // Dark gray/black background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Contact Photo/Avatar - Large circular
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                if (contactPhoto != null) {
                    Image(
                        bitmap = contactPhoto!!.asImageBitmap(),
                        contentDescription = "Contact photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Show initials
                    val displayText = if (contactName != null) {
                        contactName.split(" ").take(2).joinToString("") { it.firstOrNull()?.toString() ?: "" }.uppercase()
                    } else if (phoneNumber.isNotEmpty()) {
                        phoneNumber.takeLast(2)
                    } else {
                        "?"
                    }
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Call type indicator (e.g., "Via Google Voice Wi-Fi call")
            Text(
                text = when (callType) {
                    CallType.INCOMING -> "Incoming call"
                    CallType.OUTGOING -> "Outgoing call"
                    else -> "Phone call"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Contact Name - Prominently displayed (always show name or number)
            Text(
                text = contactName ?: formatPhoneNumber(phoneNumber),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Phone Number (always show if we have a contact name, or show formatted number if no name)
            if (contactName != null && phoneNumber.isNotEmpty()) {
                // Show number below name if contact exists
                Text(
                    text = formatPhoneNumber(phoneNumber),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            } else if (phoneNumber.isNotEmpty()) {
                // Show formatted number if no contact name
                Text(
                    text = formatPhoneNumber(phoneNumber),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Call Duration
            if (isCallActive && callDuration > 0) {
                Text(
                    text = formatCallDurationTimer(callDuration),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            } else if (!isCallActive) {
                Text(
                    text = when (callType) {
                        CallType.INCOMING -> "Incoming call"
                        CallType.OUTGOING -> "Calling..."
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
            
            // DTMF Sequence Display (show what's being typed)
            if (dtmfSequence.isNotEmpty() && isCallActive) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Entered: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = dtmfSequence,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 4.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Keypad Bottom Sheet (shown when keypad button is clicked)
            if (isCallActive) {
                KeypadBottomSheet(
                    isVisible = showKeypad,
                    onDismiss = { showKeypad = false },
                    onKeyPressed = { digit ->
                        CallControlManager.playDtmfTone(digit)
                        // Update displayed sequence immediately
                        dtmfSequence = CallControlManager.getDtmfDigits()
                    },
                    dtmfSequence = dtmfSequence
                )
            }
            
            // Call Controls
            if (callType == CallType.INCOMING && !isCallActive) {
                // Incoming call - Answer/Reject buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Reject Button
                    FloatingActionButton(
                        onClick = onReject,
                        containerColor = Color(0xFF424242),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Reject",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                    
                    // Answer Button
                    FloatingActionButton(
                        onClick = onAnswer,
                        containerColor = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Answer",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }
            } else {
                // Active call - Control buttons in 2x2 grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // First row: Mute, Keypad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ControlButtonWithIcon(
                            iconText = if (isMuted) "🔇" else "🎤",
                            label = "Mute",
                            isActive = isMuted,
                            onClick = {
                                isMuted = CallControlManager.toggleMute()
                            }
                        )
                        
                        ControlButtonWithIcon(
                            iconText = "⌨️",
                            label = "Keypad",
                            isActive = showKeypad,
                            onClick = { 
                                showKeypad = !showKeypad
                            }
                        )
                    }
                    
                    // Second row: Speaker, Hold
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ControlButtonWithIcon(
                            iconText = if (isSpeakerOn) "🔊" else "📱", // 🔊 when on, 📱 when off - very clear difference
                            label = "Speaker",
                            isActive = isSpeakerOn,
                            onClick = {
                                val targetState = !isSpeakerOn
                                // Update UI immediately for visual feedback
                                isSpeakerOn = targetState
                                
                                // 1. Immediate request via InCallService
                                CallControlManager.setSpeaker(targetState)
                                
                                // 2. Delayed "Force" request (Crucial for MIUI/Samsung)
                                // The system often resets audio route 200-500ms after the call connects
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    CallControlManager.setSpeaker(targetState)
                                    // Sync with actual state
                                    isSpeakerOn = CallControlManager.isSpeakerOn()
                                }, 500)
                                
                                // Also sync after a longer delay to ensure it stuck
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    isSpeakerOn = CallControlManager.isSpeakerOn()
                                }, 1000)
                            }
                        )
                        
                        ControlButtonWithIcon(
                            iconText = if (isOnHold) "▶️" else "⏸️", // Play icon when on hold, pause when not
                            label = "Hold",
                            isActive = isOnHold,
                            onClick = {
                                val activeCall = CallControlManager.getActiveCall()
                                if (activeCall != null) {
                                    try {
                                        if (isOnHold) {
                                            // Unhold the call
                                            activeCall.unhold()
                                            isOnHold = false
                                            Log.d("InCallActivity", "Call unhold requested")
                                        } else {
                                            // Hold the call - check if supported
                                            if (activeCall.details.hasProperty(Call.Details.CAPABILITY_SUPPORT_HOLD)) {
                                                activeCall.hold()
                                                isOnHold = true
                                                Log.d("InCallActivity", "Call hold requested")
                                            } else {
                                                Log.w("InCallActivity", "Call does not support hold capability")
                                            }
                                        }
                                        // Sync from actual call state after a delay
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            val updatedCall = CallControlManager.getActiveCall()
                                            isOnHold = updatedCall?.state == Call.STATE_HOLDING
                                        }, 300)
                                    } catch (e: Exception) {
                                        Log.e("InCallActivity", "Error toggling hold: ${e.message}", e)
                                    }
                                } else {
                                    Log.w("InCallActivity", "No active call to hold/unhold")
                                }
                            }
                        )
                    }
                    
                    // Third row: Recording
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ControlButtonWithIcon(
                            iconText = if (isRecording) "🔴" else "⏺️",
                            label = if (isRecording) "Recording" else "Record",
                            isActive = isRecording,
                            onClick = {
                                if (isRecording) {
                                    val recordingPath = callRecorder.stopRecording()
                                    if (recordingPath != null) {
                                        Log.d("InCallActivity", "Recording stopped: $recordingPath")
                                        android.widget.Toast.makeText(
                                            context,
                                            "Call recording saved",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Recording failed or was empty",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    isRecording = false
                                } else {
                                    if (callRecorder.startRecording(phoneNumber)) {
                                        isRecording = true
                                        Log.d("InCallActivity", "Recording started")
                                        android.widget.Toast.makeText(
                                            context,
                                            "Recording started",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Failed to start recording",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // End Call Button - Large red circular button
                    FloatingActionButton(
                        onClick = onEndCall,
                        containerColor = Color(0xFFE53935), // Red color
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "End Call",
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer {
                                    scaleX = -1f // Flip horizontally
                                },
                            tint = Color.White
                        )
                    }
                }
            }
            
            // App branding at bottom
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AppTrack",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ControlButtonWithIcon(
    iconText: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(IntrinsicSize.Min)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = if (isActive) 
                        Color(0xFF4CAF50).copy(alpha = 0.5f) // More visible green tint when active
                    else 
                        Color(0xFF424242), // Dark gray when inactive
                    shape = CircleShape
                )
        ) {
            Text(
                text = iconText,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp,
                color = if (isActive) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.9f)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 12.sp,
            color = if (isActive) Color(0xFF4CAF50).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeypadBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onKeyPressed: (Char) -> Unit,
    dtmfSequence: String = ""
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title and DTMF sequence display
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Keypad",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (dtmfSequence.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Entered: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = dtmfSequence,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 4.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Keypad Grid
                KeypadGrid(
                    onKeyPressed = onKeyPressed
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun KeypadGrid(
    onKeyPressed: (Char) -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    KeypadButton(
                        key = key,
                        onClick = { onKeyPressed(key[0]) }
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    key: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            // Show letters for some keys
            if (key == "2") {
                Text("ABC", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            } else if (key == "3") {
                Text("DEF", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            } else if (key == "4") {
                Text("GHI", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            } else if (key == "5") {
                Text("JKL", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            } else if (key == "6") {
                Text("MNO", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            } else if (key == "7") {
                Text("PQRS", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            } else if (key == "8") {
                Text("TUV", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            } else if (key == "9") {
                Text("WXYZ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

fun getContactPhoto(context: android.content.Context, phoneNumber: String): Bitmap? {
    return try {
        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
            .appendPath(phoneNumber)
            .build()
        
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                val contactId = it.getLong(it.getColumnIndex(ContactsContract.PhoneLookup._ID))
                val photoUri = ContactsContract.Contacts.getLookupUri(contactId, "")
                
                val photoStream = ContactsContract.Contacts.openContactPhotoInputStream(
                    context.contentResolver,
                    photoUri
                )
                
                photoStream?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                null
            }
        }
    } catch (e: Exception) {
        Log.e("InCallActivity", "Failed to load contact photo: ${e.message}")
        null
    }
}

fun formatPhoneNumber(phoneNumber: String): String {
    // Simple formatting - can be enhanced
    return if (phoneNumber.length > 10) {
        phoneNumber
    } else {
        phoneNumber
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
