package com.example.apptrack.call.PostCallSummary

import com.example.apptrack.call.CallInfo

/**
 * Stats shown in the after-call popup (Truecaller-style).
 */
data class AfterCallStats(
    val totalCallsWithNumber: Int = 0,
    val lastCallTimestamp: Long? = null,
    val spamReports: Int = 0,
    val previousCallDurationMs: Long = 0L,
    val callsInLast30Days: Int = 0
)

private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000

/**
 * Compute after-call stats for a number from call history.
 */
fun computeAfterCallStats(callHistory: List<CallInfo>, phoneNumber: String): AfterCallStats {
    val withNumber = callHistory.filter { it.phoneNumber == phoneNumber }
    val total = withNumber.size
    val now = System.currentTimeMillis()
    val last30Days = withNumber.filter { now - it.timestamp < THIRTY_DAYS_MS }
    val lastCallTs = withNumber.maxByOrNull { it.timestamp }?.timestamp
    val previousDuration = withNumber
        .filter { it.duration > 0 }
        .maxByOrNull { it.timestamp }
        ?.duration ?: 0L
    return AfterCallStats(
        totalCallsWithNumber = total,
        lastCallTimestamp = lastCallTs,
        spamReports = 0,
        previousCallDurationMs = previousDuration,
        callsInLast30Days = last30Days.size
    )
}
