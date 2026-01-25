package com.example.apptrack.call

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object RecordingFileUtils {
    private const val TAG = "RecordingFileUtils"
    private const val RECORDINGS_DIR = "AppTrack Recordings"
    
    fun getRecordingsDirectory(context: Context): File {
        val recordingsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: Use app-specific external storage (still accessible via file manager)
            File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), RECORDINGS_DIR)
        } else {
            // Pre-Android 10: Use public Music directory
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), RECORDINGS_DIR)
        }
        
        if (!recordingsDir.exists()) {
            val created = recordingsDir.mkdirs()
            Log.d(TAG, "Recordings directory created: $created at ${recordingsDir.absolutePath}")
        }
        return recordingsDir
    }
    
    fun generateFileName(phoneNumber: String): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val cleanNumber = phoneNumber.replace(Regex("[^\\d]"), "")
        return "call_${timestamp}_$cleanNumber.mp4"
    }
    
    fun getRecordingFilePath(context: Context, phoneNumber: String): String {
        val recordingsDir = getRecordingsDirectory(context)
        val fileName = generateFileName(phoneNumber)
        val file = File(recordingsDir, fileName)
        return file.absolutePath
    }
    
    fun deleteRecording(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Recording file deleted: $deleted - $filePath")
                deleted
            } else {
                Log.w(TAG, "Recording file does not exist: $filePath")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recording file: $filePath", e)
            false
        }
    }
    
    fun getRecordingFileSize(filePath: String): Long {
        return try {
            val file = File(filePath)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file size: $filePath", e)
            0L
        }
    }
}
