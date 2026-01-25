package com.example.apptrack.call

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CallManager(private val context: Context) {
    
    companion object {
        private var instance: CallManager? = null
        private val blockedNumbersSet = mutableSetOf<String>()
        
        fun getInstance(context: Context): CallManager {
            return instance ?: CallManager(context.applicationContext).also { instance = it }
        }
        
        fun isNumberBlocked(phoneNumber: String): Boolean {
            return blockedNumbersSet.contains(phoneNumber)
        }
    }
    
    private val telephonyManager: TelephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }
    
    private val _currentCall = MutableStateFlow<CallInfo?>(null)
    val currentCall: StateFlow<CallInfo?> = _currentCall.asStateFlow()
    
    private val _callHistory = MutableStateFlow<List<CallInfo>>(emptyList())
    val callHistory: StateFlow<List<CallInfo>> = _callHistory.asStateFlow()
    
    private val telecomManager: TelecomManager by lazy {
        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    }
    
    private val callManagerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    phoneNumber?.let { number ->
                        val callInfo = CallInfo(
                            phoneNumber = number,
                            callType = CallType.INCOMING,
                            timestamp = System.currentTimeMillis()
                        )
                        _currentCall.value = callInfo
                    }
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    // Call answered or outgoing call started
                    _currentCall.value?.let { call ->
                        _currentCall.value = call.copy(
                            callType = if (call.callType == CallType.INCOMING) CallType.INCOMING else CallType.OUTGOING
                        )
                    }
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    // Call ended
                    _currentCall.value?.let { call ->
                        val durationMs = System.currentTimeMillis() - call.timestamp
                        val updatedCall = call.copy(
                            duration = durationMs // Store in milliseconds
                        )
                        _callHistory.value = _callHistory.value + updatedCall
                        _currentCall.value = null
                    }
                }
            }
        }
    }
    
    fun startListening() {
        // Check permission before listening
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("CallManager", "READ_PHONE_STATE permission not granted")
            return
        }
        
        try {
            @Suppress("DEPRECATION")
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: SecurityException) {
            Log.e("CallManager", "Failed to start listening: ${e.message}", e)
        }
    }
    
    fun stopListening() {
        @Suppress("DEPRECATION")
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }
    
    fun blockNumber(phoneNumber: String) {
        blockedNumbersSet.add(phoneNumber)
    }
    
    fun unblockNumber(phoneNumber: String) {
        blockedNumbersSet.remove(phoneNumber)
    }
    
    fun isBlocked(phoneNumber: String): Boolean {
        return blockedNumbersSet.contains(phoneNumber)
    }
    
    fun getBlockedNumbers(): Set<String> {
        return blockedNumbersSet.toSet()
    }
    
    fun isDefaultPhoneApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val defaultDialer = telecomManager.defaultDialerPackage
                defaultDialer == context.packageName
            } catch (e: Exception) {
                Log.e("CallManager", "Failed to check default phone app: ${e.message}", e)
                false
            }
        } else {
            false
        }
    }
    
    fun requestDefaultDialerRole(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // Android 10+ (API 29+) uses RoleManager - this is the correct way
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
                    if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER)) {
                        val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        Log.d("CallManager", "Requested ROLE_DIALER using RoleManager")
                        return true
                    } else {
                        Log.w("CallManager", "ROLE_DIALER is not available")
                    }
                }
                
                // Fallback to TelecomManager for Android 6-9
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d("CallManager", "Requested default dialer using TelecomManager")
                    return true
                } else {
                    Log.w("CallManager", "Default dialer intent cannot be resolved")
                }
            } catch (e: Exception) {
                Log.e("CallManager", "Failed to request default dialer role: ${e.message}", e)
            }
        }
        return false
    }
    
    fun getDefaultPhoneAppIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                // Check if intent can be resolved
                val resolveInfo: ResolveInfo? = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                if (resolveInfo != null) {
                    Log.d("CallManager", "Default dialer intent can be resolved")
                    return intent
                } else {
                    Log.w("CallManager", "Default dialer intent cannot be resolved, using fallback")
                }
            } catch (e: Exception) {
                Log.e("CallManager", "Failed to get default phone app intent: ${e.message}", e)
            }
            
            // Fallback to general default apps settings
            Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            // Fallback for older versions
            Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }
    
    fun makeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("CallManager", "CALL_PHONE permission not granted")
            return
        }
        
        // Check if we're the default phone app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isDefaultPhoneApp()) {
            Log.w("CallManager", "App is not set as default phone app")
            // Return false to indicate we need to set as default
            return
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val uri = Uri.fromParts("tel", phoneNumber, null)
                
                // For real calls, use the system's default SIM phone account for actual telephony
                // Get the default phone account (SIM card) for real calls
                val defaultPhoneAccounts = telecomManager.callCapablePhoneAccounts
                val defaultPhoneAccount = defaultPhoneAccounts.firstOrNull()
                
                val extras = Bundle().apply {
                    // Use the default SIM's phone account for real telephony connection
                    if (defaultPhoneAccount != null) {
                        putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, defaultPhoneAccount)
                        Log.d("CallManager", "Using default SIM phone account: $defaultPhoneAccount")
                    } else {
                        Log.w("CallManager", "No default phone account found, using system default")
                    }
                }
                
                Log.d("CallManager", "Placing call to $phoneNumber (using real telephony)")
                telecomManager.placeCall(uri, extras)
            } else {
                // Fallback for older versions
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (e: SecurityException) {
            Log.e("CallManager", "Failed to make call: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to make call: ${e.message}", e)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.M)
    fun getPhoneAccountHandle(): PhoneAccountHandle? {
        return try {
            val componentName = ComponentName(context, AppConnectionService::class.java)
            PhoneAccountHandle(componentName, "AppTrack")
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to get phone account handle: ${e.message}", e)
            null
        }
    }
    
    fun registerPhoneAccount() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val componentName = ComponentName(context, AppConnectionService::class.java)
                val phoneAccountHandle = PhoneAccountHandle(componentName, "AppTrack")
                
                // Check if already registered
                val existingAccounts = telecomManager.callCapablePhoneAccounts
                val alreadyRegistered = existingAccounts.any { it == phoneAccountHandle }
                
                if (alreadyRegistered) {
                    Log.d("CallManager", "Phone account already registered")
                    return
                }
                
                // Build phone account with minimal required capabilities for default phone app
                // MANDATORY: Must have CAPABILITY_CALL_PROVIDER for default phone app
                val phoneAccountBuilder = PhoneAccount.builder(phoneAccountHandle, "AppTrack")
                    .setCapabilities(
                        PhoneAccount.CAPABILITY_CALL_PROVIDER
                    )
                    .setShortDescription("AppTrack Call Manager")
                    .setSupportedUriSchemes(listOf(PhoneAccount.SCHEME_TEL))
                
                // Try to set icon, but don't fail if it doesn't work
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeResource(
                        context.resources, 
                        context.applicationInfo.icon
                    )
                    if (bitmap != null) {
                        val icon = android.graphics.drawable.Icon.createWithBitmap(bitmap)
                        phoneAccountBuilder.setIcon(icon)
                    }
                } catch (e: Exception) {
                    Log.w("CallManager", "Could not set phone account icon: ${e.message}")
                }
                
                val phoneAccount = phoneAccountBuilder.build()
                
                telecomManager.registerPhoneAccount(phoneAccount)
                Log.d("CallManager", "Phone account registered with capabilities: ${phoneAccount.capabilities}")
                
                // Verify registration and check status
                try {
                    // Check if phone account is registered
                    val registeredAccount = telecomManager.getPhoneAccount(phoneAccountHandle)
                    if (registeredAccount != null) {
                        Log.d("CallManager", "✓ Phone account is registered!")
                        Log.d("CallManager", "  - Enabled: ${registeredAccount.isEnabled}")
                        Log.d("CallManager", "  - Capabilities: ${registeredAccount.capabilities}")
                        Log.d("CallManager", "  - Label: ${registeredAccount.label}")
                        Log.d("CallManager", "  - Short Description: ${registeredAccount.shortDescription}")
                    } else {
                        Log.e("CallManager", "✗ Phone account NOT found after registration!")
                    }
                    
                    // List all call-capable phone accounts
                    val registeredAccounts = telecomManager.callCapablePhoneAccounts
                    Log.d("CallManager", "Total call-capable phone accounts: ${registeredAccounts.size}")
                    registeredAccounts.forEach { handle ->
                        val account = telecomManager.getPhoneAccount(handle)
                        Log.d("CallManager", "  - ${handle.componentName.className} (${handle.id})")
                        account?.let {
                            Log.d("CallManager", "    Enabled: ${it.isEnabled}, Capabilities: ${it.capabilities}")
                        }
                        if (handle == phoneAccountHandle) {
                            Log.d("CallManager", "    ✓ This is our phone account!")
                        }
                    }
                    
                    // Check if we're the default
                    val defaultDialer = telecomManager.defaultDialerPackage
                    Log.d("CallManager", "Default dialer package: $defaultDialer")
                    Log.d("CallManager", "Our package name: ${context.packageName}")
                    if (defaultDialer == context.packageName) {
                        Log.d("CallManager", "  ✓ We are the default dialer!")
                    } else {
                        Log.d("CallManager", "  ✗ We are NOT the default dialer. Current: $defaultDialer")
                        Log.d("CallManager", "  → Go to Settings → Apps → Default apps → Phone app to set us as default")
                    }
                } catch (e: Exception) {
                    Log.e("CallManager", "Failed to verify phone account registration: ${e.message}", e)
                }
            } catch (e: Exception) {
                Log.e("CallManager", "Failed to register phone account: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }
    
    fun answerCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ANSWER_PHONE_CALLS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // This will be handled by ConnectionService
                    Log.d("CallManager", "Answer call requested")
                }
            } catch (e: Exception) {
                Log.e("CallManager", "Failed to answer call: ${e.message}", e)
            }
        }
    }
    
    fun rejectCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // This will be handled by ConnectionService
                Log.d("CallManager", "Reject call requested")
            } catch (e: Exception) {
                Log.e("CallManager", "Failed to reject call: ${e.message}", e)
            }
        }
    }
    
    suspend fun loadCallHistory() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("CallManager", "READ_CALL_LOG permission not granted")
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                val calls = mutableListOf<CallInfo>()
                val cursor: Cursor? = context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.TYPE,
                        CallLog.Calls.DATE,
                        CallLog.Calls.DURATION
                    ),
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )
                
                val phoneNumbers = mutableSetOf<String>()
                
                cursor?.use {
                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                    val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                    val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
                    
                    var count = 0
                    val maxCalls = 500 // Limit to 500 most recent calls
                    
                    // First pass: collect all calls and phone numbers
                    while (it.moveToNext() && count < maxCalls) {
                        val number = it.getString(numberIndex) ?: "Unknown"
                        val type = it.getInt(typeIndex)
                        val date = it.getLong(dateIndex)
                        val durationSeconds = it.getLong(durationIndex)
                        val duration = durationSeconds * 1000 // Convert seconds to milliseconds
                        
                        val callType = when (type) {
                            CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                            CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                            CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
                            else -> CallType.INCOMING
                        }
                        
                        calls.add(
                            CallInfo(
                                phoneNumber = number,
                                contactName = null, // Will be filled in batch
                                callType = callType,
                                timestamp = date,
                                duration = duration,
                                isBlocked = blockedNumbersSet.contains(number)
                            )
                        )
                        
                        if (number != "Unknown") {
                            phoneNumbers.add(number)
                        }
                        count++
                    }
                }
                
                // Batch load all contact names at once (much faster)
                val contactNamesMap = batchLoadContactNames(phoneNumbers.toList())
                
                // Update calls with contact names
                calls.forEach { call ->
                    val contactName = contactNamesMap[call.phoneNumber]
                    if (contactName != null) {
                        val index = calls.indexOf(call)
                        calls[index] = call.copy(contactName = contactName)
                    }
                }
                
                _callHistory.value = calls
                Log.d("CallManager", "Loaded ${calls.size} calls from call log")
            } catch (e: Exception) {
                Log.e("CallManager", "Failed to load call history: ${e.message}", e)
            }
        }
    }
    
    /**
     * Refresh call history from the system call log.
     * This is a non-suspend function that can be called from anywhere.
     * It will update the callHistory StateFlow, which will automatically trigger UI updates.
     */
    fun refreshCallHistory() {
        callManagerScope.launch {
            loadCallHistory()
        }
    }
    
    private fun batchLoadContactNames(phoneNumbers: List<String>): Map<String, String> {
        if (phoneNumbers.isEmpty() || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyMap()
        }
        
        val contactNamesMap = mutableMapOf<String, String>()
        
        try {
            // Process in chunks to avoid too many queries
            phoneNumbers.forEach { number ->
                try {
                    val lookupUri = Uri.withAppendedPath(
                        ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(number)
                    )
                    val cursor = context.contentResolver.query(
                        lookupUri,
                        arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )
                    
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                            if (nameIndex >= 0) {
                                val name = it.getString(nameIndex)
                                if (name != null) {
                                    contactNamesMap[number] = name
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Skip this number if lookup fails
                }
            }
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to batch load contact names: ${e.message}", e)
        }
        
        return contactNamesMap
    }
    
    private fun getContactName(phoneNumber: String): String? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        
        return try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                .appendPath(phoneNumber)
                .build()
            
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    it.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to get contact name: ${e.message}", e)
            null
        }
    }
}
