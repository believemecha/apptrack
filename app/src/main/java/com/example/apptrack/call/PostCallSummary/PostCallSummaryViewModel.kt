package com.example.apptrack.call.PostCallSummary

import androidx.lifecycle.ViewModel
import com.example.apptrack.call.CallType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing the post-call summary popup state.
 */
class PostCallSummaryViewModel : ViewModel() {
    
    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()
    
    private val _summaryData = MutableStateFlow<CallSummaryData?>(null)
    val summaryData: StateFlow<CallSummaryData?> = _summaryData.asStateFlow()
    
    
    /**
     * Initialize the popup with call summary data.
     */
    fun initialize(data: CallSummaryData) {
        _summaryData.value = data
        _isVisible.value = true
    }
    
    /**
     * Dismiss the popup.
     */
    fun dismiss() {
        _isVisible.value = false
    }
    
    /**
     * Format call duration as mm:ss.
     */
    fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Format timestamp as readable time.
     */
    fun formatTime(timestamp: Long): String {
        val time = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        return time
    }
    
    /**
     * Get "Call ended" text with time ago (like "Call ended less than 1m ago").
     */
    fun getCallEndedText(callEndTimestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - callEndTimestamp
        val minutes = (diff / (1000 * 60)).toInt()
        val hours = (diff / (1000 * 60 * 60)).toInt()
        val days = (diff / (1000 * 60 * 60 * 24)).toInt()
        
        return when {
            minutes < 1 -> "Call ended less than 1m ago"
            minutes < 60 -> "Call ended $minutes${if (minutes == 1) "m" else "m"} ago"
            hours < 24 -> "Call ended $hours${if (hours == 1) "h" else "h"} ago"
            days < 7 -> "Call ended $days${if (days == 1) "d" else "d"} ago"
            else -> {
                val date = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                    .format(java.util.Date(callEndTimestamp))
                "Call ended on $date"
            }
        }
    }
    
    /**
     * Get call type display text.
     */
    fun getCallTypeText(callType: CallType): String {
        return when (callType) {
            CallType.INCOMING -> "Incoming Call"
            CallType.OUTGOING -> "Outgoing Call"
            CallType.MISSED -> "Missed Call"
            CallType.REJECTED -> "Rejected Call"
            CallType.BLOCKED -> "Blocked Call"
        }
    }
    
    /**
     * Get call type color as ARGB integer (for UI theming).
     */
    fun getCallTypeColorInt(callType: CallType): Long {
        return when (callType) {
            CallType.INCOMING -> 0xFF4CAF50L // Green
            CallType.OUTGOING -> 0xFF2196F3L // Blue
            CallType.MISSED -> 0xFFF44336L // Red
            CallType.REJECTED -> 0xFFFF9800L // Orange
            CallType.BLOCKED -> 0xFF9E9E9EL // Gray
        }
    }
    
    override fun onCleared() {
        super.onCleared()
    }
}
