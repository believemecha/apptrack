package com.example.apptrack.call

import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.util.Log
import com.example.apptrack.ui.screens.InCallActivity
import com.example.apptrack.call.PostCallSummary.CallSummaryData
import com.example.apptrack.call.PostCallSummary.CallSummaryLauncher
import com.example.apptrack.call.CallType

class AppInCallService : InCallService() {
    
    companion object {
        private const val TAG = "AppInCallService"
    }
    
    override fun onCreate() {
        super.onCreate()
        CallControlManager.initialize(this)
        CallControlManager.setInCallService(this)
        Log.d(TAG, "InCallService created")
    }
    
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val phoneNumber = call.details.handle?.schemeSpecificPart ?: ""
        val callState = call.state
        val direction = call.details.callDirection
        
        Log.d(TAG, "onCallAdded: $phoneNumber, state: $callState (${getStateName(callState)}), direction: $direction")
        Log.d(TAG, "Call capabilities: ${call.details.callCapabilities}")
        Log.d(TAG, "Call properties: ${call.details.callProperties}")
        
        // Only set audio mode for incoming calls that are ringing
        // For outgoing calls or active calls, audio is already set
        if (direction == Call.Details.DIRECTION_INCOMING && callState == Call.STATE_RINGING) {
            Log.d(TAG, "Incoming call ringing - setting audio mode")
            CallControlManager.setAudioModeInCall()
        }
        
        // Show in-call UI immediately - use a small delay to ensure service is ready
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                // Get contact name for the call
                val contactName = getContactName(phoneNumber)
                
                val intent = Intent(this, InCallActivity::class.java).apply {
                    // Critical flags to show over lock screen and bypass background start restrictions
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or 
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                            Intent.FLAG_ACTIVITY_NO_HISTORY)
                    putExtra("phoneNumber", phoneNumber)
                    putExtra("contactName", contactName)
                    putExtra("callType", if (direction == Call.Details.DIRECTION_INCOMING) "INCOMING" else "OUTGOING")
                }
                startActivity(intent)
                Log.d(TAG, "InCallActivity started successfully for $phoneNumber (name: $contactName)")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException starting InCallActivity (may need SYSTEM_ALERT_WINDOW permission): ${e.message}", e)
                // Try again with different flags as fallback
                try {
                    val contactName = getContactName(phoneNumber)
                    val fallbackIntent = Intent(this, InCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra("phoneNumber", phoneNumber)
                        putExtra("contactName", contactName)
                        putExtra("callType", if (direction == Call.Details.DIRECTION_INCOMING) "INCOMING" else "OUTGOING")
                    }
                    startActivity(fallbackIntent)
                    Log.d(TAG, "InCallActivity started with fallback flags")
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to start InCallActivity with fallback: ${e2.message}", e2)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start InCallActivity: ${e.message}", e)
            }
        }, 100)
        
        // Register callbacks to track state changes
        call.registerCallback(callCallback)
    }
    
    private fun getStateName(state: Int): String {
        return when (state) {
            Call.STATE_NEW -> "NEW"
            Call.STATE_RINGING -> "RINGING"
            Call.STATE_DIALING -> "DIALING"
            Call.STATE_ACTIVE -> "ACTIVE"
            Call.STATE_HOLDING -> "HOLDING"
            Call.STATE_DISCONNECTED -> "DISCONNECTED"
            Call.STATE_CONNECTING -> "CONNECTING"
            Call.STATE_DISCONNECTING -> "DISCONNECTING"
            Call.STATE_SELECT_PHONE_ACCOUNT -> "SELECT_PHONE_ACCOUNT"
            else -> "UNKNOWN($state)"
        }
    }
    
    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "onCallRemoved: ${call.details.handle}")
        call.unregisterCallback(callCallback)
        
        // Reset audio mode when no calls
        if (calls.isEmpty()) {
            CallControlManager.setAudioModeNormal()
        }
    }
    
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            val phoneNumber = call.details.handle?.schemeSpecificPart ?: ""
            val stateName = getStateName(state)
            Log.d(TAG, "Call state changed for $phoneNumber: $state ($stateName)")
            
            when (state) {
                Call.STATE_RINGING -> {
                    Log.d(TAG, "Call is ringing - ensure audio mode is set")
                    CallControlManager.setAudioModeInCall()
                }
                Call.STATE_ACTIVE -> {
                    Log.d(TAG, "Call is now active - starting timer")
                    // Call is answered/active, start the timer
                    CallControlManager.startCallTimer()
                }
                Call.STATE_DISCONNECTED -> {
                    val disconnectCause = call.details.disconnectCause
                    Log.d(TAG, "Call disconnected - cause: ${disconnectCause?.code} (${getDisconnectCauseName(disconnectCause?.code ?: -1)})")
                    
                    // Get call information before cleanup
                    val phoneNumber = call.details.handle?.schemeSpecificPart ?: ""
                    val callDirection = call.details.callDirection
                    val callDuration = CallControlManager.getCallDuration()
                    val callEndTimestamp = System.currentTimeMillis()
                    
                    // Determine call type based on direction and disconnect cause
                    val callType = when {
                        disconnectCause?.code == DisconnectCause.MISSED -> CallType.MISSED
                        disconnectCause?.code == DisconnectCause.REJECTED -> CallType.REJECTED
                        callDirection == Call.Details.DIRECTION_INCOMING -> CallType.INCOMING
                        else -> CallType.OUTGOING
                    }
                    
                    // Call ended, stop timer and close the activity
                    CallControlManager.setAudioModeNormal()
                    
                    // Refresh call history to show the new call in the list
                    // Use a small delay to ensure the call log is written to the system
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val callManager = com.example.apptrack.call.CallManager.getInstance(this@AppInCallService)
                        callManager.refreshCallHistory()
                        Log.d(TAG, "Refreshed call history after call ended")
                    }, 500) // 500ms delay to ensure call log is written
                    
                    // Close the InCallActivity by sending a broadcast or intent
                    val intent = Intent("com.example.apptrack.CALL_DISCONNECTED").apply {
                        setPackage(packageName)
                    }
                    sendBroadcast(intent)
                    
                    // Also try to close via activity intent
                    val closeIntent = Intent(this@AppInCallService, InCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        action = "CLOSE_ACTIVITY"
                    }
                    try {
                        startActivity(closeIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to close InCallActivity: ${e.message}", e)
                    }
                    
                    // Show post-call summary popup
                    // Delay to ensure InCallActivity is closed first
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            // Get contact name and photo
                            val contactName = getContactName(phoneNumber)
                            val contactPhotoUri = getContactPhotoUri(phoneNumber)
                            
                            val summaryData = CallSummaryData(
                                phoneNumber = phoneNumber,
                                callType = callType,
                                callDurationMillis = callDuration,
                                callEndTimestamp = callEndTimestamp,
                                contactName = contactName,
                                contactPhotoUri = contactPhotoUri
                            )
                            CallSummaryLauncher.show(this@AppInCallService, summaryData)
                            Log.d(TAG, "Post-call summary triggered for $phoneNumber (name: $contactName)")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to show post-call summary: ${e.message}", e)
                        }
                    }, 800) // Delay to ensure UI is ready
                }
            }
        }
        
        override fun onCallDestroyed(call: Call) {
            super.onCallDestroyed(call)
            Log.d(TAG, "Call destroyed: ${call.details.handle?.schemeSpecificPart}")
        }
    }
    
    private fun getDisconnectCauseName(code: Int): String {
        return when (code) {
            DisconnectCause.ERROR -> "ERROR"
            DisconnectCause.REJECTED -> "REJECTED"
            DisconnectCause.REMOTE -> "REMOTE"
            DisconnectCause.CANCELED -> "CANCELED"
            DisconnectCause.MISSED -> "MISSED"
            DisconnectCause.ANSWERED_ELSEWHERE -> "ANSWERED_ELSEWHERE"
            DisconnectCause.BUSY -> "BUSY"
            DisconnectCause.LOCAL -> "LOCAL"
            else -> "UNKNOWN($code)"
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind called - InCallService is being bound by system")
        return super.onBind(intent)
    }
    
    private fun getContactName(phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        it.getString(nameIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get contact name: ${e.message}", e)
            null
        }
    }
    
    private fun getContactPhotoUri(phoneNumber: String): Uri? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    val lookupKeyIndex = it.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY)
                    if (idIndex >= 0) {
                        val contactId = it.getLong(idIndex)
                        val lookupKey = if (lookupKeyIndex >= 0) it.getString(lookupKeyIndex) else null
                        
                        // Get photo URI
                        val contactUri = if (lookupKey != null) {
                            ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
                        } else {
                            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString())
                        }
                        
                        val photoCursor = contentResolver.query(
                            contactUri,
                            arrayOf(ContactsContract.Contacts.PHOTO_URI),
                            null,
                            null,
                            null
                        )
                        photoCursor?.use { photoCursor ->
                            if (photoCursor.moveToFirst()) {
                                val photoIndex = photoCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                                if (photoIndex >= 0) {
                                    val photoUriString = photoCursor.getString(photoIndex)
                                    if (photoUriString != null) {
                                        return Uri.parse(photoUriString)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get contact photo URI: ${e.message}", e)
            null
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        CallControlManager.setInCallService(null)
        CallControlManager.setAudioModeNormal()
        Log.d(TAG, "InCallService destroyed")
    }
}
