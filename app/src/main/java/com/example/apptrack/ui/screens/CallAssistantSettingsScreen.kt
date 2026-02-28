package com.example.apptrack.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import com.example.apptrack.call.assistant.AssistantPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallAssistantSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { AssistantPreferences(context) }
    var enabled by remember { mutableStateOf(prefs.isAssistantEnabled) }
    var greeting by remember { mutableStateOf(prefs.greetingText) }
    var transcription by remember { mutableStateOf(prefs.isTranscriptionEnabled) }
    var autoBlockSpam by remember { mutableStateOf(prefs.isAutoBlockSpamEnabled) }

    fun save() {
        prefs.isAssistantEnabled = enabled
        prefs.greetingText = greeting
        prefs.isTranscriptionEnabled = transcription
        prefs.isAutoBlockSpamEnabled = autoBlockSpam
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Call Assistant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            androidx.compose.material3.Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    prefs.isAssistantEnabled = it
                }
            )
            Text(
                "Enable Call Assistant",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Screen incoming calls with TTS greeting and capture messages.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Greeting (played to caller)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            androidx.compose.material3.OutlinedTextField(
                value = greeting,
                onValueChange = {
                    greeting = it
                    prefs.greetingText = it
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                placeholder = { Text(AssistantPreferences.DEFAULT_GREETING) }
            )
            Spacer(modifier = Modifier.height(20.dp))

            androidx.compose.material3.Switch(
                checked = transcription,
                onCheckedChange = {
                    transcription = it
                    prefs.isTranscriptionEnabled = it
                }
            )
            Text(
                "Enable transcription",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Save caller's message as text and show in call details.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            androidx.compose.material3.Switch(
                checked = autoBlockSpam,
                onCheckedChange = {
                    autoBlockSpam = it
                    prefs.isAutoBlockSpamEnabled = it
                }
            )
            Text(
                "Auto-block spam callers",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "When you mark a number as spam, block it automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
