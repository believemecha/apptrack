package com.example.apptrack.call.PostCallSummary

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.apptrack.call.CallType
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Truecaller-style after-call popup: full-width bottom sheet with contact header,
 * stats chips, action buttons, and useful links. Slide-up animation, swipe/click outside to dismiss.
 */
@Composable
fun AfterCallPopup(
    viewModel: PostCallSummaryViewModel,
    contactPhoto: Bitmap?,
    onDismiss: () -> Unit,
    onCallBack: (String) -> Unit = {},
    onMessage: (String) -> Unit = {},
    onViewContact: (String) -> Unit = {},
    onBlockNumber: (String) -> Unit = {},
    onAddToFavorites: (String) -> Unit = {},
    onSeeCallHistory: (String) -> Unit = {},
    onSearchOnWeb: (String) -> Unit = {},
    onSeeAnalytics: () -> Unit = {}
) {
    val isVisible by viewModel.isVisible.collectAsState()
    val summaryDataState by viewModel.summaryData.collectAsState()
    val statsState by viewModel.afterCallStats.collectAsState()

    val data = summaryDataState
    if (!isVisible || data == null) return
    val summaryData: CallSummaryData = data
    val stats = statsState ?: AfterCallStats()

    Dialog(
        onDismissRequest = {
            viewModel.dismiss()
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { viewModel.dismiss(); onDismiss() }
        ) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(top = 48.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) { },
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 32.dp)
                    ) {
                        // Dismiss (X) top-right
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = {
                                    viewModel.dismiss()
                                    onDismiss()
                                },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // --- CONTACT HEADER ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar 48dp
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (contactPhoto != null) {
                                    Image(
                                        bitmap = contactPhoto.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = (summaryData.contactName?.firstOrNull() ?: summaryData.phoneNumber.firstOrNull())?.uppercase() ?: "?",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.size(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = summaryData.contactName ?: summaryData.phoneNumber,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${summaryData.phoneLabel} · ${summaryData.phoneNumber}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CallTypeIcon(callType = summaryData.callType)
                                    Text(
                                        text = viewModel.getCallTypeText(summaryData.callType),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when (summaryData.callType) {
                                            CallType.MISSED -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Text(
                                        text = " · ${viewModel.formatDuration(summaryData.callDurationMillis)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = viewModel.formatTodayTime(summaryData.callEndTimestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // --- STATS ROW (pill chips) ---
                        StatsRow(viewModel = viewModel, stats = stats)

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // --- ACTION BUTTONS ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ActionRow(
                                icon = Icons.Default.Call,
                                label = "Call Back",
                                onClick = { onCallBack(summaryData.phoneNumber) }
                            )
                            ActionRow(
                                icon = Icons.Default.Message,
                                label = "Message",
                                onClick = { onMessage(summaryData.phoneNumber) }
                            )
                            ActionRow(
                                icon = Icons.Default.Person,
                                label = if (summaryData.contactName != null) "View Contact" else "Save Contact",
                                onClick = { onViewContact(summaryData.phoneNumber) }
                            )
                            ActionRow(
                                icon = Icons.Default.Block,
                                label = "Block Number",
                                onClick = { onBlockNumber(summaryData.phoneNumber) },
                                tint = MaterialTheme.colorScheme.error
                            )
                            ActionRow(
                                icon = Icons.Default.Star,
                                label = "Add to Favorites",
                                onClick = { onAddToFavorites(summaryData.phoneNumber) }
                            )
                        }

                        // --- USEFUL LINKS ---
                        Text(
                            text = "Useful Links",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LinkRow(
                                icon = Icons.Default.History,
                                label = "See Call History for This Number",
                                onClick = { onSeeCallHistory(summaryData.phoneNumber) }
                            )
                            LinkRow(
                                icon = Icons.Default.Phone,
                                label = "Search This Number on Web",
                                onClick = { onSearchOnWeb(summaryData.phoneNumber) }
                            )
                            LinkRow(
                                icon = Icons.Default.History,
                                label = "See Total Analytics",
                                onClick = onSeeAnalytics
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallTypeIcon(callType: CallType) {
    val errorColor = MaterialTheme.colorScheme.error
    val variantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val (icon, tint) = when (callType) {
        CallType.INCOMING -> Icons.Default.CallReceived to Color(0xFF4CAF50)
        CallType.OUTGOING -> Icons.Default.CallMade to Color(0xFF2196F3)
        CallType.MISSED -> Icons.Default.CallMissed to errorColor
        else -> Icons.Default.Phone to variantColor
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = tint
    )
}

@Composable
private fun StatsRow(viewModel: PostCallSummaryViewModel, stats: AfterCallStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatChip(
            text = "Total calls: ${stats.totalCallsWithNumber}",
            modifier = Modifier.weight(1f)
        )
        StatChip(
            text = "Last 30 days: ${stats.callsInLast30Days}",
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stats.lastCallTimestamp?.let { ts ->
            StatChip(
                text = "Last: ${viewModel.formatTodayTime(ts)}",
                modifier = Modifier.weight(1f)
            )
        }
        if (stats.previousCallDurationMs > 0) {
            StatChip(
                text = "Prev duration: ${viewModel.formatDuration(stats.previousCallDurationMs)}",
                modifier = Modifier.weight(1f)
            )
        }
        if (stats.spamReports > 0) {
            StatChip(
                text = "Spam reports: ${stats.spamReports}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 1.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = tint
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = tint
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
