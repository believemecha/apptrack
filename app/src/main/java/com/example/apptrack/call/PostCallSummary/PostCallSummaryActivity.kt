package com.example.apptrack.call.PostCallSummary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.apptrack.call.CallManager
import com.example.apptrack.call.CallType
import com.example.apptrack.ui.screens.getContactPhoto
import com.example.apptrack.ui.theme.AppTrackTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Activity that displays the Truecaller-style after-call popup.
 * Transparent activity with full-width bottom sheet.
 */
class PostCallSummaryActivity : ComponentActivity() {

    private val viewModel: PostCallSummaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        val phoneNumber = intent.getStringExtra("phoneNumber") ?: "Unknown"
        val callTypeName = intent.getStringExtra("callType") ?: "INCOMING"
        val callDurationMillis = intent.getLongExtra("callDurationMillis", 0L)
        val callEndTimestamp = intent.getLongExtra("callEndTimestamp", System.currentTimeMillis())
        val contactName = intent.getStringExtra("contactName")
        val contactPhotoUriString = intent.getStringExtra("contactPhotoUri")
        val contactPhotoUri = contactPhotoUriString?.let { android.net.Uri.parse(it) }

        val callType = try {
            CallType.valueOf(callTypeName)
        } catch (e: IllegalArgumentException) {
            CallType.INCOMING
        }

        val summaryData = CallSummaryData(
            phoneNumber = phoneNumber,
            callType = callType,
            callDurationMillis = callDurationMillis,
            callEndTimestamp = callEndTimestamp,
            contactName = contactName,
            contactPhotoUri = contactPhotoUri
        )
        viewModel.initialize(summaryData)

        setContent {
            AppTrackTheme {
                val context = LocalContext.current
                val contactPhoto = remember { mutableStateOf<android.graphics.Bitmap?>(null) }

                LaunchedEffect(phoneNumber) {
                    contactPhoto.value = withContext(Dispatchers.IO) {
                        getContactPhoto(context, phoneNumber)
                    }
                }
                LaunchedEffect(phoneNumber) {
                    val callManager = CallManager.getInstance(context)
                    callManager.refreshCallHistory()
                    delay(600)
                    val history = callManager.callHistory.value
                    viewModel.setStats(computeAfterCallStats(history, phoneNumber))
                }

                val isVisible by viewModel.isVisible.collectAsState()
                LaunchedEffect(isVisible) {
                    if (!isVisible) {
                        finish()
                        overridePendingTransition(0, 0)
                    }
                }

                AfterCallPopup(
                    viewModel = viewModel,
                    contactPhoto = contactPhoto.value,
                    onDismiss = {
                        viewModel.dismiss()
                        finish()
                        overridePendingTransition(0, 0)
                    },
                    onCallBack = { num ->
                        viewModel.dismiss()
                        finish()
                        CallManager.getInstance(context).makeCall(num)
                    },
                    onBlockNumber = { num ->
                        CallManager.getInstance(context).blockNumber(num)
                        viewModel.dismiss()
                        finish()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        viewModel.dismiss()
        finish()
        overridePendingTransition(0, 0)
    }
}
