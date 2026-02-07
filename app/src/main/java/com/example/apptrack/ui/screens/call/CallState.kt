package com.example.apptrack.ui.screens.call

/**
 * Represents the current state of a call for UI.
 * Maps from Android Telecom Call.STATE_* and call direction.
 */
enum class CallState {
    /** User is dialing; call not yet connected. */
    Dialing,
    /** Remote party is ringing (outgoing) or we are ringing (incoming). */
    Ringing,
    /** Call is being connected. */
    Connecting,
    /** Call is active (connected). */
    Active,
    /** Call has ended. */
    Ended,
    /** Incoming call was missed. */
    Missed,
    /** Call is on hold. */
    OnHold
}

/**
 * UI state for the in-call screen: contact info, call state, and control toggles.
 */
data class CallUIState(
    val phoneNumber: String = "",
    val contactName: String? = null,
    val phoneLabel: String = "Mobile",
    val callState: CallState = CallState.Dialing,
    val callDurationMs: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isOnHold: Boolean = false,
    val isKeypadVisible: Boolean = false,
    val dtmfDigits: String = "",
    val isSuspectedSpam: Boolean = false,
    val isUnknownNumber: Boolean = false,
    val contactPhotoPath: String? = null
)
