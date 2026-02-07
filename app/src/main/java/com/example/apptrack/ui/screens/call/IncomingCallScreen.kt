package com.example.apptrack.ui.screens.call

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen incoming call UI (Google Phone style).
 * Big avatar, name, number+label, optional spam/unknown banner,
 * green Answer and red Decline pill buttons, optional Remind me / Message.
 */
@Composable
fun IncomingCallScreen(
    uiState: CallUIState,
    contactPhoto: Bitmap?,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onRemindMe: (() -> Unit)? = null,
    onMessage: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        // Big circular avatar with subtle pulse when ringing
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.scale(scale),
            contentAlignment = Alignment.Center
        ) {
            CallAvatar(
                contactName = uiState.contactName,
                phoneNumber = uiState.phoneNumber,
                photo = contactPhoto,
                size = 120.dp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Spam / unknown banner
        if (uiState.isSuspectedSpam) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Suspected spam caller",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (uiState.isUnknownNumber && !uiState.isSuspectedSpam) {
            Text(
                text = "Unknown number",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Contact name – 28–32sp medium
        Text(
            text = uiState.contactName ?: uiState.phoneNumber,
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Phone number + label
        Text(
            text = "${uiState.phoneLabel} ${uiState.phoneNumber}",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // Optional: Remind me / Message
        if (onRemindMe != null || onMessage != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                onRemindMe?.let { onRemind ->
                    androidx.compose.material3.TextButton(onClick = onRemind) {
                        Text("Remind me", color = MaterialTheme.colorScheme.primary)
                    }
                }
                onMessage?.let { onMsg ->
                    androidx.compose.material3.TextButton(onClick = onMsg) {
                        Text("Message", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Answer (green) and Decline (red) – giant pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IncomingCallActionButton(isAnswer = false, onClick = onDecline)
            IncomingCallActionButton(isAnswer = true, onClick = onAnswer)
        }
    }
}
