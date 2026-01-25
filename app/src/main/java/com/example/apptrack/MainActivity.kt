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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.apptrack.ui.screens.CallManagementScreen
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
    
    // Track permission grant state - make it reactive
    var permissionsGranted by remember { mutableStateOf(false) }
    
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
    
    // Re-check permissions periodically and when app resumes
    LaunchedEffect(Unit) {
        permissionsGranted = checkPermissions()
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
            
            // Dialog to set app as default phone app
            if (showSetDefaultDialog) {
                AlertDialog(
                    onDismissRequest = { showSetDefaultDialog = false },
                    title = { Text("Set as Default Phone App") },
                    text = { 
                        Text("This app needs to be set as the default phone app to make calls. Please set it as default in settings.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                Log.d("MainActivity", "Open Settings button clicked")
                                try {
                                    // Open default apps settings directly - more reliable
                                    val settingsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    Log.d("MainActivity", "Opening default apps settings")
                                    
                                    // Check if intent can be resolved
                                    val resolveInfo = activity.packageManager.resolveActivity(settingsIntent, PackageManager.MATCH_DEFAULT_ONLY)
                                    if (resolveInfo != null) {
                                        Log.d("MainActivity", "Default apps settings intent can be resolved")
                                        activity.startActivity(settingsIntent)
                                        Log.d("MainActivity", "Default apps settings opened")
                                    } else {
                                        Log.w("MainActivity", "Default apps settings cannot be resolved, trying app settings")
                                        // Fallback to app settings
                                        val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", activity.packageName, null)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        activity.startActivity(appSettingsIntent)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to open settings: ${e.message}", e)
                                    e.printStackTrace()
                                }
                                showSetDefaultDialog = false
                            }
                        ) {
                            Text("Open Settings")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSetDefaultDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
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
                onOpenDialer = { showDialer = true },
                onAnswerCall = { callManager.answerCall() },
                onRejectCall = { callManager.rejectCall() }
            )
            
            // Dialog to set app as default phone app
            if (showSetDefaultDialog) {
                AlertDialog(
                    onDismissRequest = { showSetDefaultDialog = false },
                    title = { Text("Set as Default Phone App") },
                    text = { 
                        Text("This app needs to be set as the default phone app to make calls. Click 'Set as Default' to open the system dialog.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                Log.d("MainActivity", "Request default dialer role")
                                // Use RoleManager to request dialer role - this will show the app in the list
                                val requested = callManager.requestDefaultDialerRole()
                                if (!requested) {
                                    // Fallback to opening settings
                                    try {
                                        val settingsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        activity.startActivity(settingsIntent)
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Failed to open settings: ${e.message}", e)
                                    }
                                }
                                showSetDefaultDialog = false
                            }
                        ) {
                            Text("Set as Default")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSetDefaultDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}