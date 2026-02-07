package com.example.apptrack.ui.screens.call

import android.telecom.Call
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apptrack.ui.viewmodel.InCallViewModel
import com.example.apptrack.call.CallControlManager
import com.example.apptrack.call.CallType
import com.example.apptrack.ui.screens.getContactPhoto
import com.example.apptrack.ui.screens.KeypadBottomSheet

/**
 * Single entry point for the call UI: shows either [IncomingCallScreen] or [OutgoingCallScreen]
 * based on call type and state. Uses [InCallViewModel] for state.
 */
@Composable
fun GooglePhoneCallScreen(
    phoneNumber: String,
    contactName: String?,
    callType: CallType,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onEndCall: () -> Unit,
    loadContactPhoto: (android.content.Context, String) -> android.graphics.Bitmap? = ::getContactPhoto,
    modifier: Modifier = Modifier,
    viewModel: InCallViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val contactPhoto = remember(phoneNumber) { loadContactPhoto(context, phoneNumber) }

    // Sync state from Telecom / CallControlManager (frequent so hold state and highlight update quickly)
    LaunchedEffect(phoneNumber, contactName, callType) {
        while (true) {
            kotlinx.coroutines.delay(200)
            val activeCall = CallControlManager.getActiveCall()
            val telecomState = activeCall?.state ?: Call.STATE_NEW
            val duration = CallControlManager.getCallDuration()
            val dtmf = CallControlManager.getDtmfDigits()
            val isMuted = CallControlManager.isMuted()
            val isSpeaker = CallControlManager.isSpeakerOn()
            val isHold = activeCall?.state == Call.STATE_HOLDING

            viewModel.updateFromTelecom(
                phoneNumber = phoneNumber,
                contactName = contactName,
                phoneLabel = "Mobile",
                isIncoming = callType == CallType.INCOMING,
                telecomState = telecomState,
                callDurationMs = duration,
                dtmfDigits = dtmf,
                isMuted = isMuted,
                isSpeakerOn = isSpeaker,
                isOnHold = isHold,
                isSuspectedSpam = false,
                isUnknownNumber = contactName == null && phoneNumber.isNotEmpty()
            )
        }
    }

    val showIncomingUi = callType == CallType.INCOMING &&
        uiState.callState != CallState.Active &&
        uiState.callState != CallState.OnHold &&
        uiState.callState != CallState.Ended

    if (showIncomingUi) {
        IncomingCallScreen(
            uiState = uiState.copy(
                phoneNumber = phoneNumber,
                contactName = contactName,
                phoneLabel = "Mobile"
            ),
            contactPhoto = contactPhoto,
            onAnswer = onAnswer,
            onDecline = onReject,
            onRemindMe = null,
            onMessage = null,
            modifier = modifier
        )
    } else {
        OutgoingCallScreen(
            uiState = uiState.copy(
                phoneNumber = phoneNumber,
                contactName = contactName,
                phoneLabel = "Mobile"
            ),
            contactPhoto = contactPhoto,
            onMuteToggle = {
                val muted = CallControlManager.toggleMute()
                viewModel.setMuted(muted)
            },
            onKeypadToggle = {
                viewModel.setKeypadVisible(!uiState.isKeypadVisible)
            },
            onSpeakerToggle = {
                val on = !uiState.isSpeakerOn
                CallControlManager.setSpeaker(on)
                viewModel.setSpeakerOn(on)
            },
            onHoldToggle = {
                val hold = !uiState.isOnHold
                val ok = if (hold) CallControlManager.holdCall() else CallControlManager.unholdCall()
                if (ok) viewModel.setOnHold(hold)
            },
            onAddCall = { },
            onMore = { },
            onEndCall = onEndCall,
            keypadContent = {
                if (uiState.isKeypadVisible && (uiState.callState == CallState.Active || uiState.callState == CallState.OnHold)) {
                    KeypadBottomSheet(
                        isVisible = true,
                        onDismiss = { viewModel.setKeypadVisible(false) },
                        onKeyPressed = { digit ->
                            CallControlManager.playDtmfTone(digit)
                        },
                        dtmfSequence = uiState.dtmfDigits
                    )
                }
            },
            modifier = modifier
        )
    }
}
