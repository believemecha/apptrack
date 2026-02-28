package com.example.apptrack.call

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

/**
 * CallScreeningService that allows incoming calls but does not reject.
 * Blocking is still applied via CallResponse. Assistant flow is triggered
 * by InCallService when it receives the call (if assistant is enabled in settings).
 *
 * Flow:
 * 1. Incoming call → onScreenCall
 * 2. If blocked → disallow/reject/skip log
 * 3. If not blocked → allow call (response with no disallow) → system adds call to InCallService
 * 4. InCallService.onCallAdded → if assistant enabled, show overlay instead of InCallActivity
 */
class CallAssistantScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "CallAssistantScreening"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        Log.d(TAG, "onScreenCall: $phoneNumber")

        val isBlocked = CallManager.isNumberBlocked(phoneNumber)

        val response = CallResponse.Builder()
            .setDisallowCall(isBlocked)
            .setRejectCall(isBlocked)
            .setSkipCallLog(isBlocked)
            .setSkipNotification(isBlocked)
            .build()

        respondToCall(callDetails, response)
    }
}
