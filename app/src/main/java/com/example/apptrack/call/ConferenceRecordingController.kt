package com.example.apptrack.call

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.TelecomManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Controls a carrier conference. Audio is recorded by the telephone endpoint, not this app. */
object ConferenceRecordingController {
    var status by mutableStateOf("Not connected to a recording service")
        private set
    var busy by mutableStateOf(false)
        private set
    private var customer: Call? = null
    private var recorder: Call? = null
    private var endpoint = ""
    private var mergeRequested = false
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { fail("Recording connection timed out; customer call preserved") }

    fun start(context: Context, number: String) {
        if (busy) return
        val service = CallControlManager.getInCallService() ?: return
        val call = service.calls.firstOrNull { it.state == Call.STATE_ACTIVE && it.parent == null }
        if (call == null || service.calls.count { it.state != Call.STATE_DISCONNECTED } != 1) {
            status = "Start with one connected customer call"
            return
        }
        if (!number.matches(Regex("\\+?[0-9]{5,15}"))) {
            status = "Enter a valid recording-service telephone number"
            return
        }
        if (android.telephony.PhoneNumberUtils.isEmergencyNumber(number) ||
            call.details.hasProperty(Call.Details.PROPERTY_EMERGENCY_CALLBACK_MODE) ||
            android.telephony.PhoneNumberUtils.isEmergencyNumber(call.details.handle?.schemeSpecificPart.orEmpty())) {
            status = "Recording is unavailable for emergency calls"
            return
        }
        val account = call.details.accountHandle
        if (account == null || !call.details.can(Call.Details.CAPABILITY_HOLD)) {
            status = "This call cannot add a recording line"
            return
        }
        customer = call
        endpoint = number
        recorder = null
        mergeRequested = false
        busy = true
        status = "Connecting recording service; customer may be placed on hold"
        handler.postDelayed(timeout, 45000)
        try {
            val extras = Bundle().apply { putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, account) }
            context.getSystemService(TelecomManager::class.java).placeCall(Uri.fromParts("tel", number, null), extras)
        } catch (e: Exception) {
            fail("Could not call recording service: ${e.message}")
        }
    }

    fun onAdded(call: Call) {
        if (busy && recorder == null && call !== customer &&
            call.details.callDirection == Call.Details.DIRECTION_OUTGOING &&
            android.telephony.PhoneNumberUtils.compare(call.details.handle?.schemeSpecificPart, endpoint) &&
            call.details.accountHandle == customer?.details?.accountHandle) recorder = call
        update()
    }

    fun update() {
        val a = customer ?: return
        val b = recorder ?: return
        if (!busy) return
        if (a.state == Call.STATE_DISCONNECTED) {
            fail("Customer call ended")
            return
        }
        if (b.state == Call.STATE_DISCONNECTED) {
            fail("Recording service disconnected")
            return
        }
        if (a.parent != null && a.parent === b.parent) {
            handler.removeCallbacks(timeout)
            status = "Recording service joined · audio capture is controlled by the service"
        } else if (!mergeRequested && b.state == Call.STATE_ACTIVE &&
            (a.conferenceableCalls.contains(b) || b.conferenceableCalls.contains(a))) {
            mergeRequested = true
            status = "Merging recording service"
            try {
                if (a.conferenceableCalls.contains(b)) a.conference(b) else b.conference(a)
            } catch (e: Exception) { fail("Conference request failed: ${e.message}") }
        }
    }

    fun isRecordingLeg(call: Call) = call === recorder
    fun isCustomer(call: Call) = call === customer
    fun preferredCall(): Call? = customer?.takeIf { it.state != Call.STATE_DISCONNECTED }?.let { it.parent ?: it }
    fun onRemoved(call: Call) {
        if (call === customer || call === recorder) fail("Recording conference ended")
    }
    fun stop() = fail("Recording service disconnected; customer call preserved")
    private fun fail(message: String) {
        handler.removeCallbacks(timeout)
        val a = customer
        val b = recorder
        customer = null
        recorder = null
        busy = false
        mergeRequested = false
        status = message
        if (b != null && b.state != Call.STATE_DISCONNECTED) b.disconnect()
        if (a?.state == Call.STATE_HOLDING) a.unhold()
    }
}
