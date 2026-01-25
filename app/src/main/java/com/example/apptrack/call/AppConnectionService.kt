package com.example.apptrack.call

import android.content.Intent
import android.net.Uri
import android.telecom.*
import android.util.Log
import com.example.apptrack.ui.screens.InCallActivity

class AppConnectionService : ConnectionService() {
    
    companion object {
        private const val TAG = "AppConnectionService"
    }
    
    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(TAG, "onCreateOutgoingConnection: ${request?.address}")
        
        if (request == null) {
            Log.e(TAG, "ConnectionRequest is null!")
            // Return a minimal connection to avoid null
            val connection = object : Connection() {
                init {
                    setConnectionCapabilities(Connection.CAPABILITY_HOLD)
                }
            }
            connection.setDisconnected(DisconnectCause(DisconnectCause.ERROR))
            return connection
        }
        
        val connection = AppConnection(request.address, false)
        // Set required capabilities
        connection.setConnectionCapabilities(
            Connection.CAPABILITY_HOLD or
            Connection.CAPABILITY_SUPPORT_HOLD or
            Connection.CAPABILITY_MUTE
        )
        connection.setInitializing()
        connection.setDialing()
        
        // IMPORTANT: Set to ACTIVE after a short delay to trigger InCallService binding
        // The system only binds InCallService when connection is ACTIVE
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Setting connection to ACTIVE to trigger InCallService")
            connection.setActive()
            
            // Initialize audio for call
            CallControlManager.initialize(this)
            CallControlManager.setAudioModeInCall()
            
            // Also manually launch InCallActivity as fallback
            launchInCallActivity(request.address?.schemeSpecificPart ?: "", false)
        }, 500)
        
        return connection
    }
    
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(TAG, "onCreateIncomingConnection: ${request?.address}")
        
        if (request == null) {
            Log.e(TAG, "ConnectionRequest is null!")
            // Return a minimal connection to avoid null
            val connection = object : Connection() {
                init {
                    setConnectionCapabilities(Connection.CAPABILITY_HOLD)
                }
            }
            connection.setDisconnected(DisconnectCause(DisconnectCause.ERROR))
            return connection
        }
        
        val connection = AppConnection(request.address, true)
        // Set required capabilities
        connection.setConnectionCapabilities(
            Connection.CAPABILITY_HOLD or
            Connection.CAPABILITY_SUPPORT_HOLD or
            Connection.CAPABILITY_MUTE
        )
        connection.setRinging()
        
        return connection
    }
    
    // Store active connections to control them
    private val activeConnections = mutableMapOf<String, AppConnection>()
    
    private inner class AppConnection(private val address: Uri?, private val isIncoming: Boolean) : Connection() {
        
        private val phoneNumber = address?.schemeSpecificPart ?: ""
        
        init {
            // MANDATORY: Set address with proper presentation
            if (address != null) {
                setAddress(address, TelecomManager.PRESENTATION_ALLOWED)
            } else {
                Log.w(TAG, "Address is null, setting empty address")
                setAddress(Uri.EMPTY, TelecomManager.PRESENTATION_UNKNOWN)
            }
            
            // Set default capabilities if not already set
            if (connectionCapabilities == 0) {
                setConnectionCapabilities(Connection.CAPABILITY_HOLD)
            }
            
            // Store connection when active
            if (phoneNumber.isNotEmpty()) {
                activeConnections[phoneNumber] = this
            }
        }
        
        override fun onAnswer() {
            Log.d(TAG, "onAnswer")
            setActive()
            // Start call timer when call is answered
            CallControlManager.startCallTimer()
            // Launch in-call UI when call is answered
            launchInCallActivity(address?.schemeSpecificPart ?: "", isIncoming)
        }
        
        override fun onReject() {
            Log.d(TAG, "onReject")
            setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
            destroy()
        }
        
        override fun onDisconnect() {
            Log.d(TAG, "onDisconnect - local disconnect requested")
            activeConnections.remove(phoneNumber)
            setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            destroy()
        }
        
        override fun onAbort() {
            Log.d(TAG, "onAbort")
            activeConnections.remove(phoneNumber)
            setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
            destroy()
        }
        
        override fun onHold() {
            Log.d(TAG, "onHold")
            setOnHold()
        }
        
        override fun onUnhold() {
            Log.d(TAG, "onUnhold")
            setActive()
        }
    }
    
    private fun launchInCallActivity(phoneNumber: String, isIncoming: Boolean) {
        try {
            val intent = Intent(this, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("phoneNumber", phoneNumber)
                putExtra("callType", if (isIncoming) "INCOMING" else "OUTGOING")
            }
            startActivity(intent)
            Log.d(TAG, "Launched InCallActivity for $phoneNumber (incoming: $isIncoming)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch InCallActivity: ${e.message}", e)
        }
    }
}
