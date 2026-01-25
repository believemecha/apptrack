package com.example.apptrack.call

import android.net.Uri
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class AppCallScreeningService : CallScreeningService() {
    
    companion object {
        private const val TAG = "AppCallScreeningService"
    }
    
    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "onScreenCall: ${callDetails.handle}")
        
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""
        
        // Check if number is blocked
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
