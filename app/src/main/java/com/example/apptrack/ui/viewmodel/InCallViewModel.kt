package com.example.apptrack.ui.viewmodel

import android.telecom.Call
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apptrack.ui.screens.call.CallState
import com.example.apptrack.ui.screens.call.CallUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds call and UI state for the in-call screen.
 * Sync with [CallControlManager] / Telecom [Call] from the Activity.
 */
class InCallViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CallUIState())
    val uiState: StateFlow<CallUIState> = _uiState.asStateFlow()

    /** When user toggles hold, we set this so sync does not overwrite optimistic hold state before Telecom reports STATE_HOLDING. */
    private var lastHoldToggleAtMs: Long = 0
    private val holdOptimisticWindowMs = 2000L

    fun updateFromTelecom(
        phoneNumber: String,
        contactName: String?,
        phoneLabel: String,
        isIncoming: Boolean,
        telecomState: Int,
        callDurationMs: Long,
        dtmfDigits: String,
        isMuted: Boolean,
        isSpeakerOn: Boolean,
        isOnHold: Boolean,
        isSuspectedSpam: Boolean = false,
        isUnknownNumber: Boolean = false
    ) {
        val callState = when (telecomState) {
            Call.STATE_DIALING -> CallState.Dialing
            Call.STATE_RINGING -> CallState.Ringing
            Call.STATE_CONNECTING -> CallState.Connecting
            Call.STATE_ACTIVE -> CallState.Active
            Call.STATE_HOLDING -> CallState.OnHold
            Call.STATE_DISCONNECTED -> CallState.Ended
            else -> CallState.Dialing
        }
        _uiState.update { current ->
            val now = System.currentTimeMillis()
            val keepOptimisticHold = current.isOnHold &&
                !isOnHold &&
                (now - lastHoldToggleAtMs) < holdOptimisticWindowMs
            val effectiveHold = if (keepOptimisticHold) true else isOnHold
            current.copy(
                phoneNumber = phoneNumber,
                contactName = contactName,
                phoneLabel = phoneLabel,
                callState = callState,
                callDurationMs = callDurationMs,
                dtmfDigits = dtmfDigits,
                isMuted = isMuted,
                isSpeakerOn = isSpeakerOn,
                isOnHold = effectiveHold,
                isSuspectedSpam = isSuspectedSpam,
                isUnknownNumber = isUnknownNumber
            )
        }
    }

    fun setKeypadVisible(visible: Boolean) {
        _uiState.update { it.copy(isKeypadVisible = visible) }
    }

    fun setMuted(muted: Boolean) {
        _uiState.update { it.copy(isMuted = muted) }
    }

    fun setSpeakerOn(on: Boolean) {
        _uiState.update { it.copy(isSpeakerOn = on) }
    }

    fun setOnHold(hold: Boolean) {
        lastHoldToggleAtMs = System.currentTimeMillis()
        _uiState.update { it.copy(isOnHold = hold) }
    }
}
