package com.example.apptrack.call.PostCallSummary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.apptrack.call.CallType
import com.example.apptrack.ui.theme.AppTrackTheme

/**
 * Activity that displays the post-call summary popup.
 * Transparent activity that shows as an overlay.
 */
class PostCallSummaryActivity : ComponentActivity() {
    
    private val viewModel: PostCallSummaryViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure window is transparent
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Get data from intent
        val phoneNumber = intent.getStringExtra("phoneNumber") ?: "Unknown"
        val callTypeName = intent.getStringExtra("callType") ?: "INCOMING"
        val callDurationMillis = intent.getLongExtra("callDurationMillis", 0L)
        val callEndTimestamp = intent.getLongExtra("callEndTimestamp", System.currentTimeMillis())
        val contactName = intent.getStringExtra("contactName")
        val contactPhotoUriString = intent.getStringExtra("contactPhotoUri")
        val contactPhotoUri = contactPhotoUriString?.let { android.net.Uri.parse(it) }
        
        // Parse call type
        val callType = try {
            CallType.valueOf(callTypeName)
        } catch (e: IllegalArgumentException) {
            CallType.INCOMING
        }
        
        // Create summary data
        val summaryData = CallSummaryData(
            phoneNumber = phoneNumber,
            callType = callType,
            callDurationMillis = callDurationMillis,
            callEndTimestamp = callEndTimestamp,
            contactName = contactName,
            contactPhotoUri = contactPhotoUri
        )
        
        // Initialize ViewModel
        viewModel.initialize(summaryData)
        
        // Set up Compose UI
        setContent {
            AppTrackTheme {
                val isVisible by viewModel.isVisible.collectAsState()
                
                // Finish activity when popup is dismissed
                LaunchedEffect(isVisible) {
                    if (!isVisible) {
                        finish()
                        overridePendingTransition(0, 0)
                    }
                }
                
                // No Surface wrapper - Dialog handles its own background
                CallSummaryPopup(
                    viewModel = viewModel,
                    onDismiss = {
                        viewModel.dismiss()
                        finish()
                        overridePendingTransition(0, 0) // No transition animation
                    }
                )
            }
        }
    }
    
    override fun onBackPressed() {
        viewModel.dismiss()
        finish()
        overridePendingTransition(0, 0)
    }
}
