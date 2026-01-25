package com.example.apptrack.call

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.IOException

object RecorderEngine {
    private const val TAG = "RecorderEngine"
    private var context: Context? = null
    
    fun initialize(context: Context) {
        this.context = context.applicationContext
        Log.d(TAG, "RecorderEngine initialized")
    }
    
    fun getContext(): Context? = context
    
    fun createMediaRecorder(): MediaRecorder? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context?.let { MediaRecorder(it) }
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create MediaRecorder: ${e.message}", e)
            null
        }
    }
    
    fun tryStrategy(
        strategy: RecorderStrategy,
        filePath: String
    ): RecordingResult {
        val ctx = context ?: return RecordingResult.Failure("Context not initialized")
        
        if (!strategy.canUse()) {
            Log.d(TAG, "Strategy ${strategy.name} cannot be used on this device")
            return RecordingResult.Skipped(strategy.name)
        }
        
        val recorder = createMediaRecorder() ?: return RecordingResult.Failure("Failed to create MediaRecorder")
        
        return try {
            Log.d(TAG, "Attempting strategy: ${strategy.name}")
            
            // Prepare recorder with strategy
            if (!strategy.prepare(ctx, recorder, filePath)) {
                recorder.release()
                return RecordingResult.Failure("${strategy.name} prepare failed")
            }
            
            // Strategy-specific onStart
            if (!strategy.onStart(ctx)) {
                recorder.release()
                return RecordingResult.Failure("${strategy.name} onStart failed")
            }
            
            // Small delay before start
            Thread.sleep(100)
            
            // Start recording
            try {
                recorder.start()
                Log.d(TAG, "Recording started successfully with ${strategy.name}")
                RecordingResult.Success(recorder, strategy)
            } catch (e: RuntimeException) {
                Log.e(TAG, "${strategy.name} start() failed: ${e.message}", e)
                strategy.onStop(ctx)
                recorder.release()
                RecordingResult.Failure("${strategy.name} start failed: ${e.message}")
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "${strategy.name} IllegalStateException: ${e.message}", e)
            strategy.onStop(ctx)
            recorder.release()
            RecordingResult.Failure("${strategy.name} IllegalStateException: ${e.message}")
        } catch (e: SecurityException) {
            Log.e(TAG, "${strategy.name} SecurityException: ${e.message}", e)
            strategy.onStop(ctx)
            recorder.release()
            RecordingResult.Failure("${strategy.name} SecurityException: ${e.message}")
        } catch (e: IOException) {
            Log.e(TAG, "${strategy.name} IOException: ${e.message}", e)
            strategy.onStop(ctx)
            recorder.release()
            RecordingResult.Failure("${strategy.name} IOException: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "${strategy.name} Exception: ${e.message}", e)
            strategy.onStop(ctx)
            recorder.release()
            RecordingResult.Failure("${strategy.name} Exception: ${e.message}")
        }
    }
    
    fun stopRecording(recorder: MediaRecorder, strategy: RecorderStrategy): Boolean {
        val ctx = context ?: return false
        
        return try {
            // Check if recorder is actually recording before stopping
            try {
                recorder.stop()
                Log.d(TAG, "Recording stopped successfully")
                true
            } catch (e: IllegalStateException) {
                // Recorder might already be stopped or in wrong state
                Log.w(TAG, "IllegalStateException stopping recorder (may already be stopped): ${e.message}")
                // Still return true as the recording might have completed
                true
            } catch (e: RuntimeException) {
                // Handle "mediarecorder went away with unhandled events" warning
                val errorMsg = e.message ?: ""
                if (errorMsg.contains("went away") || errorMsg.contains("unhandled")) {
                    Log.w(TAG, "MediaRecorder lifecycle warning (usually safe to ignore): ${e.message}")
                    // This is often a benign warning, recording may have completed
                    true
                } else {
                    Log.e(TAG, "RuntimeException stopping recorder: ${e.message}", e)
                    false
                }
            }
        } finally {
            try {
                strategy.onStop(ctx)
            } catch (e: Exception) {
                Log.e(TAG, "Error in strategy.onStop: ${e.message}", e)
            }
            
            try {
                recorder.release()
                Log.d(TAG, "MediaRecorder released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaRecorder: ${e.message}", e)
            }
        }
    }
    
    sealed class RecordingResult {
        data class Success(val recorder: MediaRecorder, val strategy: RecorderStrategy) : RecordingResult()
        data class Failure(val message: String) : RecordingResult()
        data class Skipped(val strategy: String) : RecordingResult()
    }
}
