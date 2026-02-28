package com.example.apptrack.call

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log

object CallControlManager {
    private const val TAG = "CallControlManager"
    
    private var inCallService: InCallService? = null
    private var audioManager: AudioManager? = null
    private var isMutedState = false
    private var callStartTime: Long? = null
    private var dtmfDigits = StringBuilder() // Store DTMF digits pressed during call
    
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
            val s = it.state
            s == Call.STATE_ACTIVE ||
            s == Call.STATE_DIALING ||
            s == Call.STATE_RINGING ||
            s == Call.STATE_CONNECTING ||
            s == Call.STATE_HOLDING
        }
    }

    /** Call that is currently on hold. Use for unhold(). */
    fun getHeldCall(): Call? {
        return inCallService?.calls?.firstOrNull { it.state == Call.STATE_HOLDING }
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

    /** Force microphone unmuted (e.g. for Call Assistant so caller hears TTS and we can capture speech). */
    fun ensureMicUnmuted() {
        isMutedState = false
        audioManager?.isMicrophoneMute = false
        Log.d(TAG, "ensureMicUnmuted: mic unmuted")
    }
    
    fun setSpeaker(isOn: Boolean): Boolean {
        val service = getInCallService()
        if (service != null) {
            try {
                val route = if (isOn) {
                    CallAudioState.ROUTE_SPEAKER
                } else {
                    CallAudioState.ROUTE_WIRED_OR_EARPIECE
                }
                Log.d(TAG, "Setting audio route via InCallService: $route (isOn: $isOn)")
                service.setAudioRoute(route)
                
                // Verify the state was set
                Handler(Looper.getMainLooper()).postDelayed({
                    val currentRoute = service.callAudioState?.route ?: -1
                    Log.d(TAG, "Current audio route after set: $currentRoute (expected: $route)")
                }, 200)
                
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Exception setting audio route via InCallService: ${e.message}", e)
                // Fallback to AudioManager
                return setSpeakerFallback(isOn)
            }
        } else {
            Log.w(TAG, "InCallService is null, using AudioManager fallback")
            // Fallback for non-default dialer mode
            return setSpeakerFallback(isOn)
        }
    }
    
    private fun setSpeakerFallback(isOn: Boolean): Boolean {
        audioManager?.let { am ->
            try {
                am.isSpeakerphoneOn = isOn
                Log.d(TAG, "Set speakerphone via AudioManager fallback: $isOn")
                return am.isSpeakerphoneOn
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fallback: ${e.message}", e)
            }
        }
        return false
    }
    
    fun toggleSpeaker(): Boolean {
        val currentState = isSpeakerOn()
        val newState = !currentState
        Log.d(TAG, "Toggling speaker from $currentState to $newState")
        return setSpeaker(newState)
    }
    
    fun isSpeakerOn(): Boolean {
        val service = getInCallService()
        if (service != null) {
            try {
                val currentRoute = service.callAudioState?.route ?: -1
                val isOn = currentRoute == CallAudioState.ROUTE_SPEAKER
                Log.d(TAG, "Speaker state from InCallService: $isOn (route: $currentRoute)")
                return isOn
            } catch (e: Exception) {
                Log.e(TAG, "Exception getting audio route: ${e.message}", e)
            }
        }
        // Fallback to AudioManager
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
                // Use setMode to ensure proper audio routing
                am.mode = AudioManager.MODE_IN_CALL
                Log.d(TAG, "Audio mode set to MODE_IN_CALL")
                
                // Ensure audio routing is set up (earpiece by default)
                // Only reset if not already set by user
                val currentSpeakerState = am.isSpeakerphoneOn
                if (!currentSpeakerState) {
                    am.isSpeakerphoneOn = false
                }
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

    /**
     * Use MODE_IN_COMMUNICATION for Call Assistant: better full-duplex and routes
     * VOICE_COMMUNICATION AudioTrack toward the call path so TTS can be heard by the caller.
     */
    fun setAudioModeInCommunication() {
        audioManager?.let { am ->
            try {
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                Log.d(TAG, "Audio mode set to MODE_IN_COMMUNICATION")
                am.isSpeakerphoneOn = false
                am.isBluetoothScoOn = false
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                val currentVolume = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                if (currentVolume == 0) {
                    am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxOf(1, maxVolume / 2), 0)
                }
                ensureMicUnmuted()
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException in setAudioModeInCommunication: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set MODE_IN_COMMUNICATION: ${e.message}", e)
            }
        }
    }

    /**
     * Prepare audio for Call Assistant BEFORE answering. Ensures TTS routes through earpiece
     * (VOICE_CALL path) so mic picks it up for uplink. Call this before call.answer(), then
     * again after answer to re-apply (Telecom may change mode on answer).
     */
    fun prepareAudioForCallAssistant() {
        audioManager?.let { am ->
            try {
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                Log.d(TAG, "prepareAudioForCallAssistant: MODE_IN_COMMUNICATION")
                am.isSpeakerphoneOn = false
                am.isBluetoothScoOn = false
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, 0)
                Log.d(TAG, "prepareAudioForCallAssistant: STREAM_VOICE_CALL volume=$maxVol")
                ensureMicUnmuted()
                // Optional: disable AEC/NS on some OEMs so earpiece loopback is not filtered out
                try {
                    am.setParameters("noise_suppression=off")
                    am.setParameters("aec=off")
                    am.setParameters("ns=off")
                    Log.d(TAG, "prepareAudioForCallAssistant: AEC/NS params set off")
                } catch (_: Exception) { /* ignore if unsupported */ }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException in prepareAudioForCallAssistant: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed prepareAudioForCallAssistant: ${e.message}", e)
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
        val call = getActiveCall() ?: return false
        if (call.state == Call.STATE_HOLDING) return true
        return try {
            call.hold()
            true
        } catch (e: Exception) {
            Log.e(TAG, "holdCall failed: ${e.message}", e)
            false
        }
    }
    
    fun unholdCall(): Boolean {
        val call = getHeldCall()
        if (call == null) {
            Log.w(TAG, "unholdCall: no held call found (getHeldCall=null)")
            return false
        }
        return try {
            call.unhold()
            Log.d(TAG, "unholdCall: unhold() succeeded")
            true
        } catch (e: Exception) {
            Log.e(TAG, "unholdCall: unhold() failed: ${e.message}", e)
            false
        }
    }
    
    fun playDtmfTone(digit: Char) {
        val call = getActiveCall()
        if (call != null) {
            try {
                Log.d(TAG, "Playing DTMF tone: $digit")
                // Store the digit
                dtmfDigits.append(digit)
                Log.d(TAG, "DTMF sequence so far: ${dtmfDigits.toString()}")
                
                call.playDtmfTone(digit)
                // Stop the tone after a short delay (typically 200ms)
                Handler(Looper.getMainLooper()).postDelayed({
                    call.stopDtmfTone()
                }, 200)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play DTMF tone: ${e.message}", e)
            }
        } else {
            Log.w(TAG, "No active call to play DTMF tone")
        }
    }
    
    fun getDtmfDigits(): String {
        return dtmfDigits.toString()
    }
    
    fun clearDtmfDigits() {
        dtmfDigits.clear()
        Log.d(TAG, "DTMF digits cleared")
    }
    
    fun resetCallState() {
        // Reset all call-related state when call ends
        stopCallTimer()
        clearDtmfDigits()
        isMutedState = false
        Log.d(TAG, "Call state reset")
    }
}
