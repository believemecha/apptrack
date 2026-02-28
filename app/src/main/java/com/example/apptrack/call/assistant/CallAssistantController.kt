package com.example.apptrack.call.assistant

import android.telecom.Call
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton controller used by InCallService and CallAssistantOverlayService.
 * Holds the current call when assistant is screening; provides answer/end.
 *
 * Flow:
 * 1. Incoming call → InCallService checks assistant enabled → setIncomingForAssistant(call, number, name)
 * 2. Overlay shows → user or auto: answerCall() so caller hears TTS
 * 3. Overlay runs TTS + SpeechRecognizer → then endCall() or user taps "Answer manually"
 */
object CallAssistantController {

    private const val TAG = "CallAssistantController"

    data class IncomingAssistantCall(
        val call: Call,
        val phoneNumber: String,
        val contactName: String?
    )

    private val _incomingForAssistant = MutableStateFlow<IncomingAssistantCall?>(null)
    val incomingForAssistant: StateFlow<IncomingAssistantCall?> = _incomingForAssistant.asStateFlow()

    fun setIncomingForAssistant(call: Call, phoneNumber: String, contactName: String?) {
        Log.d(TAG, "setIncomingForAssistant: $phoneNumber")
        _incomingForAssistant.value = IncomingAssistantCall(call, phoneNumber, contactName)
    }

    fun clearIncoming() {
        Log.d(TAG, "clearIncoming")
        _incomingForAssistant.value = null
    }

    fun answerCall(): Boolean {
        val incoming = _incomingForAssistant.value ?: return false
        return try {
            incoming.call.answer(0)
            Log.d(TAG, "answerCall: answered ${incoming.phoneNumber}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "answerCall failed: ${e.message}", e)
            false
        }
    }

    fun rejectCall(): Boolean {
        val incoming = _incomingForAssistant.value ?: return false
        return try {
            incoming.call.disconnect()
            clearIncoming()
            Log.d(TAG, "rejectCall: rejected ${incoming.phoneNumber}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "rejectCall failed: ${e.message}", e)
            false
        }
    }

    fun endCall(): Boolean {
        val incoming = _incomingForAssistant.value ?: return false
        return try {
            incoming.call.disconnect()
            clearIncoming()
            Log.d(TAG, "endCall: disconnected ${incoming.phoneNumber}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "endCall failed: ${e.message}", e)
            false
        }
    }

    fun getCurrentCall(): Call? = _incomingForAssistant.value?.call
}
