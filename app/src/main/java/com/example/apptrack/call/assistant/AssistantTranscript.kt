package com.example.apptrack.call.assistant

/**
 * Model for a transcript captured by the Call Assistant.
 */
data class AssistantTranscript(
    val id: Long,
    val phoneNumber: String,
    val transcriptText: String,
    val timestampMillis: Long,
    val contactName: String? = null
)
