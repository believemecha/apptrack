package com.example.apptrack.ui.dialer

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apptrack.call.CallInfo
import com.example.apptrack.call.GroupedCallInfo
import com.example.apptrack.call.groupCallsByContact
import com.example.apptrack.ui.screens.loadContactPhotoForNumber

@Composable
fun DialPadScreen(
    callHistory: List<CallInfo> = emptyList(),
    onCall: (String) -> Unit,
    onBack: () -> Unit,
    onContactClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    val groupedCalls = remember(callHistory) { groupCallsByContact(callHistory) }
    val suggestedWhenEmpty = groupedCalls.take(5)
    val suggestedWhenTyping = remember(phoneNumber, groupedCalls) {
        if (phoneNumber.isEmpty()) emptyList()
        else {
            val digitsOnly = phoneNumber.filter { it.isDigit() }
            groupedCalls.filter { grouped ->
                val num = grouped.phoneNumber.filter { it.isDigit() }
                num.contains(digitsOnly) || grouped.displayName.contains(phoneNumber, ignoreCase = true)
            }.take(5)
        }
    }
    val suggestedList = if (phoneNumber.isEmpty()) suggestedWhenEmpty else suggestedWhenTyping
    val sectionTitle = if (phoneNumber.isEmpty()) "Suggested" else "All contacts"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Suggested / All contacts section
        if (groupedCalls.isNotEmpty()) {
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.height(160.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(suggestedList, key = { it.phoneNumber }) { grouped ->
                    SuggestedContactRow(
                        groupedCall = grouped,
                        context = context,
                        highlightDigits = if (phoneNumber.isEmpty()) null else phoneNumber,
                        onCall = { onCall(grouped.phoneNumber) },
                        onContact = { onContactClick(grouped.phoneNumber) }
                    )
                }
            }
        }

        // Number display row: More | number | X
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* more options */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            Text(
                text = phoneNumber.ifEmpty { "" },
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                    if (phoneNumber.isNotEmpty()) phoneNumber = phoneNumber.dropLast(1)
                    },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("✕", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Dial pad grid – pill-shaped keys
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DialKey("1", "_", RoundedCornerShape(50), onClick = { phoneNumber += "1" })
                DialKey("2", "ABC", RoundedCornerShape(50), onClick = { phoneNumber += "2" })
                DialKey("3", "DEF", RoundedCornerShape(50), onClick = { phoneNumber += "3" })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DialKey("4", "GHI", RoundedCornerShape(50), onClick = { phoneNumber += "4" })
                DialKey("5", "JKL", RoundedCornerShape(50), onClick = { phoneNumber += "5" })
                DialKey("6", "MNO", RoundedCornerShape(50), onClick = { phoneNumber += "6" })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DialKey("7", "PQRS", RoundedCornerShape(50), onClick = { phoneNumber += "7" })
                DialKey("8", "TUV", RoundedCornerShape(50), onClick = { phoneNumber += "8" })
                DialKey("9", "WXYZ", RoundedCornerShape(50), onClick = { phoneNumber += "9" })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DialKey("*", "", RoundedCornerShape(50), onClick = { phoneNumber += "*" })
                DialKey("0", "+", RoundedCornerShape(50), onClick = { phoneNumber += "0" })
                DialKey("#", "", RoundedCornerShape(50), onClick = { phoneNumber += "#" })
            }
        }

        // Green Call FAB (elongated, icon + "Call")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val green = androidx.compose.ui.graphics.Color(0xFF4CAF50)
            ExtendedFloatingActionButton(
                onClick = { if (phoneNumber.isNotEmpty()) onCall(phoneNumber) },
                containerColor = if (phoneNumber.isNotEmpty()) green else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (phoneNumber.isNotEmpty()) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(24.dp))
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                Text("Call", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SuggestedContactRow(
    groupedCall: GroupedCallInfo,
    context: android.content.Context,
    highlightDigits: String?,
    onCall: () -> Unit,
    onContact: () -> Unit
) {
    var contactPhoto by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(groupedCall.phoneNumber) {
        contactPhoto = loadContactPhotoForNumber(context, groupedCall.phoneNumber)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onContact() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (contactPhoto != null) {
                    androidx.compose.foundation.Image(
                        bitmap = contactPhoto!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = groupedCall.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = groupedCall.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Mobile ${groupedCall.phoneNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onCall, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Call",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DialKey(
    number: String,
    letters: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(80.dp)
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                fontSize = 32.sp
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
