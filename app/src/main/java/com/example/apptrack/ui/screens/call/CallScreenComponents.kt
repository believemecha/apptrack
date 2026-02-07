package com.example.apptrack.ui.screens.call

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Reusable call avatar: circular, photo or initials, Material dark surface.
 */
@Composable
fun CallAvatar(
    contactName: String?,
    phoneNumber: String,
    photo: Bitmap?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            val initial = contactName?.split(" ")?.take(2)?.joinToString("") { it.firstOrNull()?.toString() ?: "" }?.uppercase()?.take(2)
                ?: phoneNumber.takeLast(2).takeIf { it.length >= 2 } ?: "?"
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineLarge,
                fontSize = (size.value / 2.2f).sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Large circular call action button (Mute, Keypad, Speaker, etc.) with label.
 * 56dp circle, icon, label in small caps below. Active state = filled style. Scale on press.
 */
@Composable
fun CallActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeTint: Color = MaterialTheme.colorScheme.primary,
    inactiveTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(80), label = "scale"
    )

    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (isActive) activeTint.copy(alpha = 0.24f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = if (isActive) activeTint else inactiveTint
            )
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = if (isActive) activeTint else inactiveTint,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Giant pill-shaped Answer (green) or Decline (red) button for incoming call.
 * 64–72dp height, rounded 50%, elevated, with icon.
 */
@Composable
fun IncomingCallActionButton(
    isAnswer: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(80), label = "scale"
    )

    val backgroundColor = if (isAnswer) Color(0xFF4CAF50) else Color(0xFFE53935)
    val icon = if (isAnswer) Icons.Default.Call else Icons.Default.Close

    Box(
        modifier = modifier
            .size(width = 160.dp, height = 72.dp)
            .scale(scale)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(36.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (isAnswer) "Answer" else "Decline",
            modifier = Modifier.size(36.dp),
            tint = Color.White
        )
    }
}
