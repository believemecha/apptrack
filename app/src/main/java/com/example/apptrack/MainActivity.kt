package com.example.apptrack

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.example.apptrack.call.CallManager
import com.example.apptrack.ui.screens.CallHistoryScreen
import com.example.apptrack.ui.screens.CallManagementScreen
import com.example.apptrack.ui.screens.ContactProfileScreen
import com.example.apptrack.ui.screens.ContactsSearchScreen
import com.example.apptrack.ui.screens.DialerScreen
import com.example.apptrack.ui.theme.AppTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register phone account immediately on app start
        val callManager = CallManager.getInstance(this)
        callManager.registerPhoneAccount()
        
        enableEdgeToEdge()
        setContent {
            AppTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CallManagementApp(this@MainActivity)
                }
            }
        }
    }
}

@Composable
fun CallManagementApp(activity: ComponentActivity) {
    val context = LocalContext.current
    val callManager = remember { CallManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    val currentCall by callManager.currentCall.collectAsState()
    val callHistory by callManager.callHistory.collectAsState()
    
    // Navigation state
    var showDialer by remember { mutableStateOf(false) }
    var showSetDefaultDialog by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    
    // Track permission grant state - make it reactive
    var permissionsGranted by remember { mutableStateOf(false) }
    var overlayPermissionGranted by remember { mutableStateOf(false) }
    var isDefaultPhoneApp by remember { mutableStateOf(false) }
    var justOpenedSettings by remember { mutableStateOf(false) } // Flag to prevent immediate re-check
    
    // MIUI-specific permissions (these can't be checked via API, so we guide user to settings)
    // We'll show dialogs to guide users to grant these after overlay permission
    
    // Check if permissions are granted - re-check when needed
    fun checkPermissions(): Boolean {
        val hasPhoneStatePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasCallLogPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasCallPhonePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        
        return hasPhoneStatePermission && hasCallLogPermission && hasCallPhonePermission
    }
    
    // Check SYSTEM_ALERT_WINDOW permission (for showing call UI over lock screen)
    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Not needed on older versions
        }
    }
    
    // Check if app is set as default phone app
    fun checkDefaultPhoneApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            callManager.isDefaultPhoneApp()
        } else {
            true // Not needed on older versions
        }
    }
    
    // Function to check and show permission dialogs in sequence
    fun checkAndRequestPermissions() {
        permissionsGranted = checkPermissions()
        overlayPermissionGranted = checkOverlayPermission()
        isDefaultPhoneApp = checkDefaultPhoneApp()
        
        // Sequential permission requests:
        // 1. First check overlay permission (Display over other apps)
        // 2. Then check default phone app (only after overlay is granted)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!overlayPermissionGranted) {
                // Show overlay permission dialog first
                showOverlayPermissionDialog = true
                showSetDefaultDialog = false
            } else if (!isDefaultPhoneApp) {
                // Overlay is granted, now check default phone app
                showOverlayPermissionDialog = false
                showSetDefaultDialog = true
            } else {
                // All permissions granted
                showOverlayPermissionDialog = false
                showSetDefaultDialog = false
            }
        }
    }
    
    // Re-check permissions periodically and when app resumes
    LaunchedEffect(Unit) {
        checkAndRequestPermissions()
    }
    
    // Re-check permissions when activity resumes (user might have granted them in settings)
    LaunchedEffect(Unit) {
        activity.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: androidx.lifecycle.Lifecycle.Event) {
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    // Don't re-check immediately if we just opened settings - wait a bit
                    if (justOpenedSettings) {
                        // Clear the flag after a delay
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            justOpenedSettings = false
                            // Now re-check permissions
                            checkAndRequestPermissions()
                        }, 1000) // Wait 1 second after returning from settings
                    } else {
                        // Normal resume - re-check immediately
                        checkAndRequestPermissions()
                    }
                }
            }
        })
    }
    
    val allPermissionsGranted = remember(permissionsGranted) { permissionsGranted }
    
    // Helper functions for permission checks
    fun hasPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasCallPhonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // Permission launcher for multiple permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val phoneStateGranted = permissions[Manifest.permission.READ_PHONE_STATE] ?: false
        val callLogGranted = permissions[Manifest.permission.READ_CALL_LOG] ?: false
        val callPhoneGranted = permissions[Manifest.permission.CALL_PHONE] ?: false
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        
        if (phoneStateGranted) {
            callManager.startListening()
        }
        if (callLogGranted) {
            // Load call history when permission is granted
            scope.launch {
                callManager.loadCallHistory()
            }
        }
        if (callPhoneGranted) {
            Log.d("MainActivity", "CALL_PHONE permission granted")
        }
        if (recordAudioGranted) {
            Log.d("MainActivity", "RECORD_AUDIO permission granted")
        }
        
        // Re-check all permissions after grant
        permissionsGranted = checkPermissions()
    }
    
    // Request permissions if not granted
    LaunchedEffect(Unit) {
        permissionsGranted = checkPermissions()
        if (!permissionsGranted) {
            val permissionsToRequest = mutableListOf<String>()
            if (!hasPhoneStatePermission()) {
                permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
            }
            if (!hasCallLogPermission()) {
                permissionsToRequest.add(Manifest.permission.READ_CALL_LOG)
            }
            if (!hasContactsPermission()) {
                permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
            }
            if (!hasCallPhonePermission()) {
                permissionsToRequest.add(Manifest.permission.CALL_PHONE)
            }
            if (!hasRecordAudioPermission()) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }
            if (permissionsToRequest.isNotEmpty()) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }
    
    // Load call history when permissions are granted
    LaunchedEffect(allPermissionsGranted) {
        if (hasCallLogPermission()) {
            callManager.loadCallHistory()
        }
    }
    
    // Start/stop listening based on lifecycle and permission
    DisposableEffect(allPermissionsGranted) {
        if (hasPhoneStatePermission()) {
            callManager.startListening()
        }
        onDispose {
            callManager.stopListening()
        }
    }
    
    if (!allPermissionsGranted) {
        // Show permission request UI
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.headlineSmall
                )
    Text(
                    text = "This app needs phone, call log, and call permissions to manage calls",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = {
                    permissionsGranted = checkPermissions()
                    val permissionsToRequest = mutableListOf<String>()
                    if (!hasPhoneStatePermission()) {
                        permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
                    }
                    if (!hasCallLogPermission()) {
                        permissionsToRequest.add(Manifest.permission.READ_CALL_LOG)
                    }
                    if (!hasContactsPermission()) {
                        permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
                    }
                    if (!hasCallPhonePermission()) {
                        permissionsToRequest.add(Manifest.permission.CALL_PHONE)
                    }
                    if (!hasRecordAudioPermission()) {
                        permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (permissionsToRequest.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    } else {
                        // Re-check if all are already granted
                        permissionsGranted = checkPermissions()
                    }
                }) {
                    Text("Grant Permissions")
                }
            }
        }
    } else {
        if (showDialer) {
            // Handle system back button
            BackHandler(enabled = true) {
                showDialer = false
            }
            
            DialerScreen(
                onCall = { phoneNumber ->
                    // Check permission before making call
                    if (hasCallPhonePermission()) {
                        // Check if app is default phone app
                        if (callManager.isDefaultPhoneApp()) {
                            callManager.makeCall(phoneNumber)
                            showDialer = false
                        } else {
                            // Show dialog to set as default
                            showSetDefaultDialog = true
                        }
                    } else {
                        // Request permission if not granted
                        permissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                    }
                },
                onBack = { showDialer = false }
            )
            
            // Dialog to set app as default phone app (only show if overlay permission is granted)
            if (showSetDefaultDialog && overlayPermissionGranted) {
                AlertDialog(
                    onDismissRequest = { 
                        showSetDefaultDialog = false
                        // Re-check permissions
                        checkAndRequestPermissions()
                    },
                    title = { Text("Set as Default Phone App") },
                    text = { 
                        Text("This app needs to be set as the default phone app to handle incoming and outgoing calls. Please set it as default in settings.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                Log.d("MainActivity", "Request default dialer role")
                                // Close dialog FIRST to prevent immediate re-show
                                showSetDefaultDialog = false
                                
                                // Set flag to prevent immediate re-check
                                justOpenedSettings = true
                                
                                // Always try to open settings - more reliable approach
                                try {
                                    Log.d("MainActivity", "Attempting to open default apps settings")
                                    
                                    // First try: Use RoleManager (Android 10+) - this shows a system dialog
                                    val roleRequested = callManager.requestDefaultDialerRole()
                                    Log.d("MainActivity", "RoleManager request result: $roleRequested")
                                    
                                    // Also try to open settings directly as a backup
                                    // This ensures user can navigate to settings even if RoleManager dialog doesn't show
                                    try {
                                        val settingsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        
                                        // Check if intent can be resolved
                                        val resolveInfo = activity.packageManager.resolveActivity(settingsIntent, PackageManager.MATCH_DEFAULT_ONLY)
                                        if (resolveInfo != null) {
                                            Log.d("MainActivity", "Default apps settings intent can be resolved, opening...")
                                            activity.startActivity(settingsIntent)
                                            Log.d("MainActivity", "Opened default apps settings successfully")
                                        } else {
                                            Log.w("MainActivity", "Default apps settings intent cannot be resolved, trying app settings")
                                            // Last resort: Open app-specific settings
                                            val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", activity.packageName, null)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            activity.startActivity(appSettingsIntent)
                                            Log.d("MainActivity", "Opened app settings as fallback")
                                        }
                                    } catch (settingsException: Exception) {
                                        Log.e("MainActivity", "Failed to open settings: ${settingsException.message}", settingsException)
                                        // If settings can't be opened, clear flag and re-check
                                        justOpenedSettings = false
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            checkAndRequestPermissions()
                                        }, 1000)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to open settings: ${e.message}", e)
                                    e.printStackTrace()
                                    justOpenedSettings = false // Clear flag if settings can't be opened
                                    // Show error to user
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        checkAndRequestPermissions()
                                    }, 1000)
                                }
                            }
                        ) {
                            Text("Set as Default")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showSetDefaultDialog = false
                            // Re-check permissions - will show again if still not set
                            checkAndRequestPermissions()
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        } else {
            var showContacts by remember { mutableStateOf(false) }
            var showHistory by remember { mutableStateOf(false) }
            var showProfile by remember { mutableStateOf(false) }
            var selectedPhoneNumber by remember { mutableStateOf<String?>(null) }
            
            if (showProfile && selectedPhoneNumber != null) {
                ContactProfileScreen(
                    phoneNumber = selectedPhoneNumber!!,
                    callHistory = callHistory,
                    onBack = {
                        showProfile = false
                        selectedPhoneNumber = null
                    },
                    onMakeCall = { phoneNumber ->
                        if (hasCallPhonePermission()) {
                            if (callManager.isDefaultPhoneApp()) {
                                callManager.makeCall(phoneNumber)
                            } else {
                                showSetDefaultDialog = true
                            }
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                        }
                    }
                )
            } else if (showContacts) {
                // Handle system back button
                BackHandler(enabled = true) {
                    showContacts = false
                }
                
                ContactsSearchScreen(
                    onBack = { showContacts = false },
                    onContactSelected = { phoneNumber ->
                        showContacts = false
                        if (hasCallPhonePermission()) {
                            if (callManager.isDefaultPhoneApp()) {
                                callManager.makeCall(phoneNumber)
                            } else {
                                showSetDefaultDialog = true
                            }
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                        }
                    }
                )
            } else if (showHistory && selectedPhoneNumber != null) {
                // Handle system back button
                BackHandler(enabled = true) {
                    showHistory = false
                    selectedPhoneNumber = null
                }
                
                CallHistoryScreen(
                    phoneNumber = selectedPhoneNumber!!,
                    callHistory = callHistory,
                    onBack = { 
                        showHistory = false
                        selectedPhoneNumber = null
                    },
                    onMakeCall = { phoneNumber ->
                        if (hasCallPhonePermission()) {
                            if (callManager.isDefaultPhoneApp()) {
                                callManager.makeCall(phoneNumber)
                            } else {
                                showSetDefaultDialog = true
                            }
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                        }
                    }
                )
            } else {
                CallManagementScreen(
                    currentCall = currentCall,
                    callHistory = callHistory,
                    onBlockNumber = { callManager.blockNumber(it) },
                    onUnblockNumber = { callManager.unblockNumber(it) },
                    isBlocked = { callManager.isBlocked(it) },
                    onMakeCall = { phoneNumber ->
                        // Check permission before making call
                        if (hasCallPhonePermission()) {
                            // Check if app is default phone app
                            if (callManager.isDefaultPhoneApp()) {
                                callManager.makeCall(phoneNumber)
                            } else {
                                // Show dialog to set as default
                                showSetDefaultDialog = true
                            }
                        } else {
                            // Request permission if not granted
                            permissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                        }
                    },
                    onOpenDialer = { showDialer = true                     },
                    onOpenContacts = { showContacts = true },
                    onOpenProfile = { phoneNumber ->
                        selectedPhoneNumber = phoneNumber
                        showProfile = true
                    },
                    onOpenHistory = { phoneNumber ->
                        selectedPhoneNumber = phoneNumber
                        showHistory = true
                    },
                    onAnswerCall = { callManager.answerCall() },
                    onRejectCall = { callManager.rejectCall() }
                )
            }
            
            // Dialog to set app as default phone app (only show if overlay permission is granted)
            if (showSetDefaultDialog && overlayPermissionGranted) {
                AlertDialog(
                    onDismissRequest = { 
                        showSetDefaultDialog = false
                        // Re-check permissions
                        checkAndRequestPermissions()
                    },
                    title = { Text("Set as Default Phone App") },
                    text = { 
                        Text("This app needs to be set as the default phone app to handle incoming and outgoing calls. Please set it as default in settings.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                Log.d("MainActivity", "Request default dialer role")
                                // Close dialog FIRST to prevent immediate re-show
                                showSetDefaultDialog = false
                                
                                // Set flag to prevent immediate re-check
                                justOpenedSettings = true
                                
                                // Always try to open settings - more reliable approach
                                try {
                                    Log.d("MainActivity", "Attempting to open default apps settings")
                                    
                                    // First try: Use RoleManager (Android 10+) - this shows a system dialog
                                    val roleRequested = callManager.requestDefaultDialerRole()
                                    Log.d("MainActivity", "RoleManager request result: $roleRequested")
                                    
                                    // Also try to open settings directly as a backup
                                    // This ensures user can navigate to settings even if RoleManager dialog doesn't show
                                    try {
                                        val settingsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        
                                        // Check if intent can be resolved
                                        val resolveInfo = activity.packageManager.resolveActivity(settingsIntent, PackageManager.MATCH_DEFAULT_ONLY)
                                        if (resolveInfo != null) {
                                            Log.d("MainActivity", "Default apps settings intent can be resolved, opening...")
                                            activity.startActivity(settingsIntent)
                                            Log.d("MainActivity", "Opened default apps settings successfully")
                                        } else {
                                            Log.w("MainActivity", "Default apps settings intent cannot be resolved, trying app settings")
                                            // Last resort: Open app-specific settings
                                            val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", activity.packageName, null)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            activity.startActivity(appSettingsIntent)
                                            Log.d("MainActivity", "Opened app settings as fallback")
                                        }
                                    } catch (settingsException: Exception) {
                                        Log.e("MainActivity", "Failed to open settings: ${settingsException.message}", settingsException)
                                        // If settings can't be opened, clear flag and re-check
                                        justOpenedSettings = false
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            checkAndRequestPermissions()
                                        }, 1000)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to open settings: ${e.message}", e)
                                    e.printStackTrace()
                                    justOpenedSettings = false // Clear flag if settings can't be opened
                                    // Show error to user
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        checkAndRequestPermissions()
                                    }, 1000)
                                }
                            }
                        ) {
                            Text("Set as Default")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showSetDefaultDialog = false
                            // Re-check permissions - will show again if still not set
                            checkAndRequestPermissions()
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Dialog to request overlay permission (for showing call UI over lock screen)
            if (showOverlayPermissionDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AlertDialog(
                    onDismissRequest = { 
                        showOverlayPermissionDialog = false
                        // Re-check permissions - will show default phone app dialog if overlay is now granted
                        checkAndRequestPermissions()
                    },
                    title = { Text("Display Over Other Apps") },
                    text = { 
                        Text("This app needs permission to display the call screen over the lock screen and other apps. This is required for incoming calls to show even when your phone is locked.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                    showOverlayPermissionDialog = false
                                    // Re-check after a delay to see if user granted permission
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        checkAndRequestPermissions()
                                    }, 1000)
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to open overlay permission settings: ${e.message}", e)
                                    // Fallback to general settings
                                    try {
                                        val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(fallbackIntent)
                                    } catch (e2: Exception) {
                                        Log.e("MainActivity", "Failed to open settings: ${e2.message}", e2)
                                    }
                                }
                            }
                        ) {
                            Text("Open Settings")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showOverlayPermissionDialog = false
                            // Re-check permissions - will show default phone app dialog if overlay is now granted
                            checkAndRequestPermissions()
                        }) {
                            Text("Later")
                        }
                    }
                )
            }
            
            // Dialog to set app as default phone app (shown after overlay permission is granted)
            if (showSetDefaultDialog && overlayPermissionGranted) {
                AlertDialog(
                    onDismissRequest = { 
                        showSetDefaultDialog = false
                        // Re-check permissions
                        checkAndRequestPermissions()
                    },
                    title = { Text("Set as Default Phone App") },
                    text = { 
                        Text("This app needs to be set as the default phone app to handle incoming and outgoing calls. Please set it as default in settings.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                Log.d("MainActivity", "Request default dialer role")
                                // Close dialog FIRST to prevent immediate re-show
                                showSetDefaultDialog = false
                                
                                // Set flag to prevent immediate re-check
                                justOpenedSettings = true
                                
                                // Always try to open settings - more reliable approach
                                try {
                                    Log.d("MainActivity", "Attempting to open default apps settings")
                                    
                                    // First try: Use RoleManager (Android 10+) - this shows a system dialog
                                    val roleRequested = callManager.requestDefaultDialerRole()
                                    Log.d("MainActivity", "RoleManager request result: $roleRequested")
                                    
                                    // Also try to open settings directly as a backup
                                    // This ensures user can navigate to settings even if RoleManager dialog doesn't show
                                    try {
                                        val settingsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        
                                        // Check if intent can be resolved
                                        val resolveInfo = activity.packageManager.resolveActivity(settingsIntent, PackageManager.MATCH_DEFAULT_ONLY)
                                        if (resolveInfo != null) {
                                            Log.d("MainActivity", "Default apps settings intent can be resolved, opening...")
                                            activity.startActivity(settingsIntent)
                                            Log.d("MainActivity", "Opened default apps settings successfully")
                                        } else {
                                            Log.w("MainActivity", "Default apps settings intent cannot be resolved, trying app settings")
                                            // Last resort: Open app-specific settings
                                            val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", activity.packageName, null)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            activity.startActivity(appSettingsIntent)
                                            Log.d("MainActivity", "Opened app settings as fallback")
                                        }
                                    } catch (settingsException: Exception) {
                                        Log.e("MainActivity", "Failed to open settings: ${settingsException.message}", settingsException)
                                        // If settings can't be opened, clear flag and re-check
                                        justOpenedSettings = false
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            checkAndRequestPermissions()
                                        }, 1000)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to open settings: ${e.message}", e)
                                    e.printStackTrace()
                                    justOpenedSettings = false // Clear flag if settings can't be opened
                                    // Show error to user
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        checkAndRequestPermissions()
                                    }, 1000)
                                }
                            }
                        ) {
                            Text("Set as Default")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showSetDefaultDialog = false
                            // Re-check permissions
                            checkAndRequestPermissions()
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
        }
    }
}