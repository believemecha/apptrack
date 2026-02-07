package com.example.apptrack.call.PostCallSummary

import android.net.Uri
import com.example.apptrack.call.CallType

/**
 * Data class containing all information needed to display the post-call summary popup.
 */
data class CallSummaryData(
    val phoneNumber: String,
    val callType: CallType,
    val callDurationMillis: Long,
    val callEndTimestamp: Long,
    val contactName: String? = null,
    val contactPhotoUri: Uri? = null,
    val phoneLabel: String = "Mobile",
    val isSpam: Boolean = false
)
