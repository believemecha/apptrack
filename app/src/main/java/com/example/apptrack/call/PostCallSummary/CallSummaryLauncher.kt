package com.example.apptrack.call.PostCallSummary

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Helper class to launch the Post-Call Summary popup.
 * Provides a simple interface for AppInCallService to trigger the summary.
 */
object CallSummaryLauncher {
    private const val TAG = "CallSummaryLauncher"
    
    /**
     * Shows the post-call summary popup.
     * 
     * @param context The context to launch the activity from
     * @param summaryData The call summary data to display
     */
    fun show(context: Context, summaryData: CallSummaryData) {
        try {
            val intent = Intent(context, PostCallSummaryActivity::class.java).apply {
                // Use NEW_TASK flag to launch from service context
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                
                // Pass data via intent extras
                putExtra("phoneNumber", summaryData.phoneNumber)
                putExtra("callType", summaryData.callType.name)
                putExtra("callDurationMillis", summaryData.callDurationMillis)
                putExtra("callEndTimestamp", summaryData.callEndTimestamp)
                summaryData.contactName?.let { putExtra("contactName", it) }
                summaryData.contactPhotoUri?.let { putExtra("contactPhotoUri", it.toString()) }
            }
            
            context.startActivity(intent)
            Log.d(TAG, "Post-call summary launched for ${summaryData.phoneNumber}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch post-call summary: ${e.message}", e)
        }
    }
}
