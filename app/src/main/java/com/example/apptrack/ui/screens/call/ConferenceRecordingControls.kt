package com.example.apptrack.ui.screens.call

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.apptrack.call.ConferenceRecordingController

@Composable
fun ConferenceRecordingControls() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("conference_recording", 0) }
    var number by remember { mutableStateOf(preferences.getString("number", "").orEmpty()) }
    var configure by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(ConferenceRecordingController.status, style = MaterialTheme.typography.bodySmall)
        Row {
            TextButton(onClick = {
                if (ConferenceRecordingController.busy) ConferenceRecordingController.stop()
                else configure = true
            }) { Text(if (ConferenceRecordingController.busy) "Disconnect recorder" else "Record via conference") }
        }
    }
    if (configure) AlertDialog(
        onDismissRequest = { configure = false },
        title = { Text("Recording service") },
        text = {
            Column {
                Text("Enter a telephone recording endpoint you operate or subscribe to. It joins as a participant and may incur call charges. Notify participants before recording. A normal phone number will not record automatically.")
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Recording number (+country code)") })
            }
        },
        confirmButton = { TextButton(onClick = {
            preferences.edit().putString("number", number.trim()).apply()
            configure = false
            ConferenceRecordingController.start(context, number.trim())
        }) { Text("Connect and merge") } },
        dismissButton = { TextButton(onClick = { configure = false }) { Text("Cancel") } }
    )
}
