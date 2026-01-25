package com.example.apptrack.call

import java.util.Date

data class CallInfo(
    val phoneNumber: String,
    val contactName: String? = null,
    val callType: CallType,
    val timestamp: Long,
    val duration: Long = 0L, // in milliseconds
    val isBlocked: Boolean = false
)

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    BLOCKED
}
