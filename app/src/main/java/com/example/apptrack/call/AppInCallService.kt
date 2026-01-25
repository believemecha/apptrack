package com.example.apptrack.call

import android.content.Intent
import android.os.IBinder
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.example.apptrack.ui.screens.InCallActivity

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
        Log.d(TAG, "onCallAdded: ${call.details.handle}, state: ${call.state}, direction: ${call.details.callDirection}")
        
        // Set audio mode for call
        CallControlManager.setAudioModeInCall()
        
        // Show in-call UI immediately - use a small delay to ensure service is ready
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("phoneNumber", call.details.handle?.schemeSpecificPart ?: "")
                putExtra("callType", if (call.details.callDirection == Call.Details.DIRECTION_INCOMING) "INCOMING" else "OUTGOING")
            }
            try {
                startActivity(intent)
                Log.d(TAG, "InCallActivity started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start InCallActivity: ${e.message}", e)
            }
        }, 100)
        
        // Register callbacks
        call.registerCallback(callCallback)
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
            Log.d(TAG, "Call state changed: $state")
            when (state) {
                Call.STATE_ACTIVE -> {
                    Log.d(TAG, "Call is now active - starting timer")
                    // Call is answered/active, start the timer
                    CallControlManager.startCallTimer()
                }
                Call.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Call disconnected - closing InCallActivity")
                    // Call ended, stop timer and close the activity
                    CallControlManager.setAudioModeNormal()
                    
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
                }
            }
        }
        
        override fun onCallDestroyed(call: Call) {
            super.onCallDestroyed(call)
            Log.d(TAG, "Call destroyed")
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind called - InCallService is being bound by system")
        return super.onBind(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        CallControlManager.setInCallService(null)
        CallControlManager.setAudioModeNormal()
        Log.d(TAG, "InCallService destroyed")
    }
}
