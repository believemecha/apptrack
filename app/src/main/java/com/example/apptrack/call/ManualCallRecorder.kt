package com.example.apptrack.call

import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.util.Log
import androidx.core.content.ContextCompat

class ManualCallRecorder private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ManualCallRecorder"
        @Volatile
        private var instance: ManualCallRecorder? = null
        
        fun getInstance(context: Context): ManualCallRecorder {
            return instance ?: synchronized(this) {
                instance ?: ManualCallRecorder(context.applicationContext).also {
                    instance = it
                    RecorderEngine.initialize(context.applicationContext)
                }
            }
        }
    }
    
    private var currentRecorder: android.media.MediaRecorder? = null
    private var currentStrategy: RecorderStrategy? = null
    private var currentFilePath: String? = null
    private var isRecordingState = false
    
    private val strategies = listOf(
        // Try REMOTE_SUBMIX first (captures both sides if available)
        RecorderStrategy.RemoteSubmix,
        // Then try call-specific sources
        RecorderStrategy.VoiceCall,
        RecorderStrategy.VoiceDownlink,
        RecorderStrategy.VoiceUplink,
        // Fallback to microphone-based sources
        RecorderStrategy.VoiceRecognition,
        RecorderStrategy.VoiceCommunication,
        RecorderStrategy.MicWithSpeaker
    )
    
    fun startRecording(phoneNumber: String): Boolean {
        if (isRecordingState) {
            Log.w(TAG, "Recording already in progress")
            return false
        }
        
        // Check RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return false
        }
        
        // Ensure we're in an active call context - recording should only work during active calls
        // This prevents accidentally triggering system dialer
        val callManager = CallManager.getInstance(context)
        if (!callManager.isDefaultPhoneApp()) {
            Log.w(TAG, "App is not default phone app - recording may not work properly")
            // Still allow recording, but log warning
        }
        
        // For Android 13+ (API 33+), start recording in foreground service
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Android 13+ detected, starting foreground service for recording")
            return CallRecordingService.startService(context, phoneNumber)
        }
        
        // For older Android versions, record directly
        return startRecordingDirectly(phoneNumber)
    }
    
    internal fun startRecordingDirectly(phoneNumber: String): Boolean {
        if (isRecordingState) {
            Log.w(TAG, "Recording already in progress")
            return false
        }
        
        Log.d(TAG, "Starting manual call recording for: $phoneNumber")
        Log.d(TAG, DeviceCapabilityDetector.getDeviceInfo())
        
        // Generate file path
        val filePath = RecordingFileUtils.getRecordingFilePath(context, phoneNumber)
        currentFilePath = filePath
        Log.d(TAG, "Recording file path: $filePath")
        
        // Try each strategy in order
        for (strategy in strategies) {
            val result = RecorderEngine.tryStrategy(strategy, filePath)
            
            when (result) {
                is RecorderEngine.RecordingResult.Success -> {
                    currentRecorder = result.recorder
                    currentStrategy = result.strategy
                    isRecordingState = true
                    Log.d(TAG, "Recording started successfully with ${result.strategy.name}")
                    return true
                }
                is RecorderEngine.RecordingResult.Failure -> {
                    Log.w(TAG, "Strategy ${strategy.name} failed: ${result.message}")
                    // Continue to next strategy
                }
                is RecorderEngine.RecordingResult.Skipped -> {
                    Log.d(TAG, "Strategy ${result.strategy} skipped (not available on this device)")
                    // Continue to next strategy
                }
            }
        }
        
        // All strategies failed
        Log.e(TAG, "All recording strategies failed")
        currentFilePath = null
        return false
    }
    
    fun stopRecording(): String? {
        // For Android 13+, stop the foreground service
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Android 13+ detected, stopping foreground service")
            CallRecordingService.stopService(context)
            // The service will handle stopping the recording
            return currentFilePath
        }
        
        // For older Android versions, stop recording directly
        return stopRecordingDirectly()
    }
    
    internal fun stopRecordingDirectly(): String? {
        if (!isRecordingState || currentRecorder == null || currentStrategy == null) {
            Log.w(TAG, "No recording in progress")
            return null
        }
        
        val recorder = currentRecorder!!
        val strategy = currentStrategy!!
        val filePath = currentFilePath
        
        Log.d(TAG, "Stopping recording...")
        
        val success = RecorderEngine.stopRecording(recorder, strategy)
        
        // Cleanup
        currentRecorder = null
        currentStrategy = null
        isRecordingState = false
        
        if (success && filePath != null) {
            val fileSize = RecordingFileUtils.getRecordingFileSize(filePath)
            Log.d(TAG, "Recording stopped successfully. File: $filePath, Size: $fileSize bytes")
            
            // Delete file if it's too small (likely empty/silent)
            if (fileSize < 1000) {
                Log.w(TAG, "Recording file is too small ($fileSize bytes), likely empty. Deleting...")
                RecordingFileUtils.deleteRecording(filePath)
                return null
            }
            
            return filePath
        } else {
            Log.e(TAG, "Failed to stop recording properly")
            // Try to delete the file if recording failed
            filePath?.let { RecordingFileUtils.deleteRecording(it) }
            return null
        }
    }
    
    fun isRecording(): Boolean {
        // For Android 13+, check if service is running
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return CallRecordingService.isServiceRunning()
        }
        return isRecordingState
    }
    
    fun getCurrentFilePath(): String? {
        return currentFilePath
    }
    
    fun cleanup() {
        if (isRecordingState) {
            Log.w(TAG, "Cleanup called while recording. Stopping recording first...")
            stopRecording()
        }
        currentRecorder = null
        currentStrategy = null
        currentFilePath = null
        isRecordingState = false
    }
}
