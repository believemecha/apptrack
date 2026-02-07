package com.example.apptrack.ui.screens.call

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Outgoing / dialing / ringing / connected call screen (Google Phone style).
 * Full-screen dark, avatar, name, number+label, status text, then in-call controls.
 */
@Composable
fun OutgoingCallScreen(
    uiState: CallUIState,
    contactPhoto: Bitmap?,
    onMuteToggle: () -> Unit,
    onKeypadToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onHoldToggle: () -> Unit,
    onAddCall: () -> Unit,
    onMore: () -> Unit,
    onEndCall: () -> Unit,
    keypadContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusText = when (uiState.callState) {
        CallState.Dialing -> "Calling…"
        CallState.Ringing -> "Ringing…"
        CallState.Connecting -> "Connecting…"
        CallState.Active -> "In call · ${formatCallDuration(uiState.callDurationMs)}"
        CallState.OnHold -> "On hold · ${formatCallDuration(uiState.callDurationMs)}"
        else -> ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // 1. Large circular avatar (72dp per spec)
            CallAvatar(
                contactName = uiState.contactName,
                phoneNumber = uiState.phoneNumber,
                photo = contactPhoto,
                size = 72.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Contact name – 24sp medium
            Text(
                text = uiState.contactName ?: uiState.phoneNumber,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Phone number + label – 14sp light
            Text(
                text = "${uiState.phoneLabel} ${uiState.phoneNumber}",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Status – animate when state changes
            AnimatedContent(
                targetState = statusText,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "status"
            ) { text ->
                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 5. In-call controls: row 1 – Mute | Keypad | Speaker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CallActionButton(
                    icon = if (uiState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = "Mute",
                    isActive = uiState.isMuted,
                    onClick = onMuteToggle
                )
                CallActionButton(
                    icon = Icons.Default.Dialpad,
                    label = "Keypad",
                    isActive = uiState.isKeypadVisible,
                    onClick = onKeypadToggle
                )
                CallActionButton(
                    icon = Icons.Default.VolumeUp,
                    label = "Speaker",
                    isActive = uiState.isSpeakerOn,
                    onClick = onSpeakerToggle
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Row 2 – Hold | Add Call | More
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CallActionButton(
                    icon = Icons.Default.Pause,
                    label = "Hold",
                    isActive = uiState.isOnHold,
                    onClick = onHoldToggle
                )
                CallActionButton(
                    icon = Icons.Default.Add,
                    label = "Add call",
                    isActive = false,
                    onClick = onAddCall
                )
                CallActionButton(
                    icon = Icons.Default.MoreVert,
                    label = "More",
                    isActive = false,
                    onClick = onMore
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // End call – large red circular button (72dp)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .padding(bottom = 32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .then(
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onEndCall
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End call",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }

        keypadContent?.invoke()
    }
}

private fun formatCallDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
