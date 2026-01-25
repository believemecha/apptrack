package com.example.apptrack.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
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
        
        // Initialize CallControlManager if not already done
        CallControlManager.initialize(this)
        
        // Monitor call state and close when disconnected
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var checkCount = 0
        val maxChecks = 10
        
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
                        Log.d(TAG, "No active call found yet, checking again... ($checkCount/$maxChecks)")
                        handler.postDelayed(this, 500)
                    } else {
                        Log.d(TAG, "No active call found after $maxChecks checks, keeping activity open")
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
            
            val activeCall = CallControlManager.getActiveCall()
            isCallActive = activeCall?.state == Call.STATE_ACTIVE || callDuration > 0
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Contact Photo/Avatar - Large circular
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
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
                // Show initials or first letter
                val displayText = if (contactName != null) {
                    contactName.take(2).uppercase()
                } else if (phoneNumber.isNotEmpty()) {
                    phoneNumber.takeLast(2)
                } else {
                    "?"
                }
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Contact Name
        Text(
            text = contactName ?: phoneNumber,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Phone Number (if contact name exists)
        if (contactName != null && phoneNumber.isNotEmpty()) {
            Text(
                text = formatPhoneNumber(phoneNumber),
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Call Status/Duration
        Text(
            text = when {
                !isCallActive && callType == CallType.INCOMING -> "Incoming call"
                !isCallActive && callType == CallType.OUTGOING -> "Calling..."
                isCallActive && callDuration > 0 -> formatCallDurationTimer(callDuration)
                else -> ""
            },
            style = MaterialTheme.typography.titleMedium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
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
                    containerColor = MaterialTheme.colorScheme.error,
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
                    containerColor = MaterialTheme.colorScheme.primary,
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
            // Active call - Full controls (Google Phone style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Control buttons row (Mute, Keypad, Speaker, Add Call, Hold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Mute Button
                    ControlButtonWithText(
                        iconText = if (isMuted) "🔇" else "🎤",
                        label = "Mute",
                        isActive = isMuted,
                        onClick = {
                            isMuted = CallControlManager.toggleMute()
                        }
                    )
                    
                    // Keypad Button
                    ControlButtonWithText(
                        iconText = "⌨️",
                        label = "Keypad",
                        isActive = false,
                        onClick = { /* TODO: Show keypad */ }
                    )
                    
                    // Speaker Button
                    ControlButtonWithText(
                        iconText = if (isSpeakerOn) "🔊" else "📢",
                        label = "Speaker",
                        isActive = isSpeakerOn,
                        onClick = {
                            isSpeakerOn = CallControlManager.toggleSpeaker()
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // End Call Button - Large button with pure red horizontally flipped phone icon
                FloatingActionButton(
                    onClick = onEndCall,
                    containerColor = MaterialTheme.colorScheme.background, // Same as screen background
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "End Call",
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                scaleX = -1f // Flip horizontally only, no tilt
                            },
                        tint = Color(0xFFFF0000) // Pure red icon
                    )
                }
            }
        }
    }
}

@Composable
fun ControlButtonWithText(
    iconText: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = if (isActive) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
        ) {
            Text(
                text = iconText,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
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
