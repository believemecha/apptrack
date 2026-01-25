package com.example.apptrack.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.apptrack.MainActivity

class CallRecordingService : Service() {
    
    companion object {
        private const val TAG = "CallRecordingService"
        private const val CHANNEL_ID = "call_recording_channel"
        private const val NOTIFICATION_ID = 1001
        
        @Volatile
        private var isServiceRunning = false
        
        fun isServiceRunning(): Boolean = isServiceRunning
        
        fun startService(context: Context, phoneNumber: String): Boolean {
            if (isServiceRunning) {
                Log.w(TAG, "Service already running")
                return false
            }
            
            // Create intent with explicit component to avoid MIUI intercepting it as a call action
            val intent = Intent(context, CallRecordingService::class.java).apply {
                putExtra("phoneNumber", phoneNumber)
                // Explicitly set component to avoid intent resolution issues
                setClass(context, CallRecordingService::class.java)
                // Add flags to prevent system from treating this as a call action
                addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            }
            
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "CallRecordingService started")
                true
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException starting service (may need FOREGROUND_SERVICE_MICROPHONE permission): ${e.message}", e)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service: ${e.message}", e)
                false
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java)
            context.stopService(intent)
            Log.d(TAG, "CallRecordingService stop requested")
        }
    }
    
    private var phoneNumber: String = ""
    private var recorder: ManualCallRecorder? = null
    private var recordingFilePath: String? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        isServiceRunning = true
        Log.d(TAG, "CallRecordingService onCreate")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        phoneNumber = intent?.getStringExtra("phoneNumber") ?: ""
        
        if (phoneNumber.isEmpty()) {
            Log.e(TAG, "No phone number provided, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }
        
        // CRITICAL: Start foreground service with notification IMMEDIATELY
        // Must be called within 5 seconds of startForegroundService() or app will crash
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d(TAG, "Foreground service started with notification")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing FOREGROUND_SERVICE or FOREGROUND_SERVICE_MICROPHONE permission: ${e.message}", e)
            // Stop service immediately to prevent crash
            stopSelf()
            return START_NOT_STICKY
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Start recording directly (service is already foreground)
        // No delay needed - startForeground() was already called
        recorder = ManualCallRecorder.getInstance(this)
        val recordingStarted = recorder?.startRecordingDirectly(phoneNumber) ?: false
        
        if (!recordingStarted) {
            Log.e(TAG, "Failed to start recording, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }
        
        recordingFilePath = recorder?.getCurrentFilePath()
        Log.d(TAG, "Recording started in foreground service for: $phoneNumber")
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CallRecordingService onDestroy")
        
        // Stop recording
        val filePath = recorder?.stopRecordingDirectly()
        if (filePath != null) {
            Log.d(TAG, "Recording saved: $filePath")
        }
        recorder = null
        isServiceRunning = false
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing call recording"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        // Don't use PendingIntent that might trigger call actions
        // Just show notification without clickable action to avoid MIUI intercepting it
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Call")
            .setContentText("Recording: $phoneNumber")
            .setSmallIcon(android.R.drawable.ic_media_play) // Notification icon
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSilent(true) // Don't make sound
            .build()
    }
}
