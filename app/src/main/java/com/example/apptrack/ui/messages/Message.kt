package com.example.apptrack.ui.messages

/**
 * A message (draft/template) for messaging management CRUD.
 */
data class Message(
    val id: Long,
    val phoneNumber: String,
    val contactName: String?,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun displayTitle(): String = contactName?.takeIf { it.isNotBlank() } ?: phoneNumber.ifBlank { "Unknown" }
}
