package com.example.apptrack.call

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log

object DeviceCapabilityDetector {
    private const val TAG = "DeviceCapabilityDetector"
    
    fun supportsVoiceCall(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }
    
    fun supportsVoiceDownlink(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
    
    fun supportsVoiceUplink(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
    
    fun canControlSpeakerphone(context: Context): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager != null
        } catch (e: Exception) {
            Log.w(TAG, "Cannot control speakerphone: ${e.message}")
            false
        }
    }
    
    fun isMIUI(): Boolean {
        return try {
            val miuiVersion = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                    System.getProperty("ro.miui.ui.version.name") != null
            Log.d(TAG, "Is MIUI: $miuiVersion")
            miuiVersion
        } catch (e: Exception) {
            false
        }
    }
    
    fun isSamsung(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }
    
    fun getDeviceInfo(): String {
        return "Manufacturer: ${Build.MANUFACTURER}, Model: ${Build.MODEL}, SDK: ${Build.VERSION.SDK_INT}"
    }
}
