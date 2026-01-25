package com.example.apptrack.call

import android.content.Context
import android.media.AudioManager
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log

object CallControlManager {
    private const val TAG = "CallControlManager"
    
    private var inCallService: InCallService? = null
    private var audioManager: AudioManager? = null
    private var isMutedState = false
    private var callStartTime: Long? = null
    
    fun initialize(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        Log.d(TAG, "CallControlManager initialized")
    }
    
    fun setInCallService(service: InCallService?) {
        inCallService = service
        Log.d(TAG, "InCallService set: ${service != null}")
    }
    
    fun getInCallService(): InCallService? {
        return inCallService
    }
    
    fun getActiveCall(): Call? {
        return inCallService?.calls?.firstOrNull { 
            it.state == Call.STATE_ACTIVE || 
            it.state == Call.STATE_DIALING || 
            it.state == Call.STATE_RINGING ||
            it.state == Call.STATE_CONNECTING
        }
    }
    
    fun getIncomingCall(): Call? {
        return inCallService?.calls?.firstOrNull { 
            it.state == Call.STATE_RINGING && 
            it.details.callDirection == Call.Details.DIRECTION_INCOMING
        }
    }
    
    fun answerCall() {
        val call = getIncomingCall()
        if (call != null) {
            Log.d(TAG, "Answering incoming call")
            // Answer with audio only (voice call)
            call.answer(VideoProfile.STATE_AUDIO_ONLY)
            setAudioModeInCall()
        } else {
            Log.w(TAG, "No incoming call to answer")
        }
    }
    
    fun toggleMute(): Boolean {
        isMutedState = !isMutedState
        // Use AudioManager to mute/unmute microphone
        audioManager?.isMicrophoneMute = isMutedState
        Log.d(TAG, "Mute toggled: $isMutedState")
        return isMutedState
    }
    
    fun isMuted(): Boolean {
        return isMutedState
    }
    
    fun toggleSpeaker(): Boolean {
        audioManager?.let { am ->
            val isSpeakerOn = am.isSpeakerphoneOn
            am.isSpeakerphoneOn = !isSpeakerOn
            val newState = am.isSpeakerphoneOn
            Log.d(TAG, "Speaker toggled: $newState")
            return newState
        }
        return false
    }
    
    fun isSpeakerOn(): Boolean {
        return audioManager?.isSpeakerphoneOn ?: false
    }
    
    fun startCallTimer() {
        callStartTime = System.currentTimeMillis()
        Log.d(TAG, "Call timer started at: $callStartTime")
    }
    
    fun stopCallTimer() {
        callStartTime = null
        Log.d(TAG, "Call timer stopped")
    }
    
    fun getCallDuration(): Long {
        return callStartTime?.let { startTime ->
            System.currentTimeMillis() - startTime
        } ?: 0L
    }
    
    fun setAudioModeInCall() {
        audioManager?.let { am ->
            try {
                // For Telecom-managed calls, we should NOT request audio focus manually
                // The Telecom framework handles audio focus automatically
                // We only need to set the audio mode
                
                // NOTE: Don't start timer here - wait for call to be actually received/answered
                
                // Set audio mode for call - this is critical for call audio
                am.mode = AudioManager.MODE_IN_CALL
                Log.d(TAG, "Audio mode set to MODE_IN_CALL")
                
                // Ensure audio routing is set up (earpiece by default)
                am.isSpeakerphoneOn = false
                am.isBluetoothScoOn = false
                
                // Set audio stream volume if muted
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                val currentVolume = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                if (currentVolume == 0) {
                    val targetVolume = maxOf(1, maxVolume / 2)
                    am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, targetVolume, 0)
                    Log.d(TAG, "Set call volume to $targetVolume (max: $maxVolume)")
                }
                Log.d(TAG, "Current call volume: $currentVolume / $maxVolume")
                Log.d(TAG, "Audio routing configured for call (Telecom will handle audio focus)")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException - Missing RECORD_AUDIO permission?: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set audio mode: ${e.message}", e)
            }
        }
    }
    
    fun setAudioModeNormal() {
        audioManager?.let { am ->
            try {
                // Stop call timer
                stopCallTimer()
                
                // Reset audio mode (Telecom framework handles audio focus)
                am.mode = AudioManager.MODE_NORMAL
                Log.d(TAG, "Audio mode set to MODE_NORMAL")
                
                // Reset speaker
                am.isSpeakerphoneOn = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set audio mode: ${e.message}", e)
            }
        }
    }
    
    fun endCall() {
        val call = getActiveCall()
        call?.disconnect()
        setAudioModeNormal()
    }
    
    fun holdCall(): Boolean {
        val call = getActiveCall()
        return if (call != null && call.details.hasProperty(Call.Details.CAPABILITY_SUPPORT_HOLD)) {
            call.hold()
            true
        } else {
            false
        }
    }
    
    fun unholdCall(): Boolean {
        val call = getActiveCall()
        return if (call != null) {
            call.unhold()
            true
        } else {
            false
        }
    }
}
