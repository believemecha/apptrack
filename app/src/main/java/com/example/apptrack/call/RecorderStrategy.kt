package com.example.apptrack.call

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.IOException

sealed class RecorderStrategy {
    abstract val name: String
    abstract val audioSource: Int
    abstract fun canUse(): Boolean
    abstract fun prepare(context: Context, recorder: MediaRecorder, filePath: String): Boolean
    abstract fun onStart(context: Context): Boolean
    abstract fun onStop(context: Context)
    
    object VoiceRecognition : RecorderStrategy() {
        override val name = "VOICE_RECOGNITION"
        override val audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION
        
        private var originalAudioMode = AudioManager.MODE_NORMAL
        private var audioManager: AudioManager? = null
        
        override fun canUse(): Boolean = true
        
        override fun prepare(context: Context, recorder: MediaRecorder, filePath: String): Boolean {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            return try {
                recorder.setAudioSource(audioSource)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128000)
                recorder.setAudioSamplingRate(44100)
                recorder.setOutputFile(filePath)
                recorder.prepare()
                true
            } catch (e: Exception) {
                Log.w("RecorderStrategy", "$name prepare failed: ${e.message}")
                false
            }
        }
        
        override fun onStart(context: Context): Boolean {
            return try {
                audioManager?.let { am ->
                    originalAudioMode = am.mode
                    // Set audio mode to IN_CALL to improve microphone quality during calls
                    am.mode = AudioManager.MODE_IN_CALL
                    Log.d("RecorderStrategy", "Audio mode set to MODE_IN_CALL for recording")
                }
                true
            } catch (e: Exception) {
                Log.e("RecorderStrategy", "Failed to set audio mode: ${e.message}")
                true // Continue anyway
            }
        }
        
        override fun onStop(context: Context) {
            try {
                audioManager?.let { am ->
                    if (am.mode != originalAudioMode) {
                        am.mode = originalAudioMode
                        Log.d("RecorderStrategy", "Audio mode restored to: $originalAudioMode")
                    }
                }
            } catch (e: Exception) {
                Log.e("RecorderStrategy", "Failed to restore audio mode: ${e.message}")
            }
        }
    }
    
    object VoiceCommunication : RecorderStrategy() {
        override val name = "VOICE_COMMUNICATION"
        override val audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
        
        override fun canUse(): Boolean = true
        
        override fun prepare(context: Context, recorder: MediaRecorder, filePath: String): Boolean {
            return try {
                recorder.setAudioSource(audioSource)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128000)
                recorder.setAudioSamplingRate(44100)
                recorder.setOutputFile(filePath)
                recorder.prepare()
                true
            } catch (e: Exception) {
                Log.w("RecorderStrategy", "$name prepare failed: ${e.message}")
                false
            }
        }
        
        override fun onStart(context: Context): Boolean = true
        override fun onStop(context: Context) {}
    }
    
    object VoiceCall : RecorderStrategy() {
        override val name = "VOICE_CALL"
        override val audioSource = MediaRecorder.AudioSource.VOICE_CALL
        
        override fun canUse(): Boolean = DeviceCapabilityDetector.supportsVoiceCall()
        
        override fun prepare(context: Context, recorder: MediaRecorder, filePath: String): Boolean {
            return try {
                recorder.setAudioSource(audioSource)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128000)
                recorder.setAudioSamplingRate(44100)
                recorder.setOutputFile(filePath)
                recorder.prepare()
                true
            } catch (e: Exception) {
                Log.w("RecorderStrategy", "$name prepare failed: ${e.message}")
                false
            }
        }
        
        override fun onStart(context: Context): Boolean = true
        override fun onStop(context: Context) {}
    }
    
    object VoiceDownlink : RecorderStrategy() {
        override val name = "VOICE_DOWNLINK"
        override val audioSource = MediaRecorder.AudioSource.VOICE_DOWNLINK
        
        override fun canUse(): Boolean = DeviceCapabilityDetector.supportsVoiceDownlink()
        
        override fun prepare(context: Context, recorder: MediaRecorder, filePath: String): Boolean {
            return try {
                recorder.setAudioSource(audioSource)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128000)
                recorder.setAudioSamplingRate(44100)
                recorder.setOutputFile(filePath)
                recorder.prepare()
                true
            } catch (e: Exception) {
                Log.w("RecorderStrategy", "$name prepare failed: ${e.message}")
                false
            }
        }
        
        override fun onStart(context: Context): Boolean = true
        override fun onStop(context: Context) {}
    }
    
    object VoiceUplink : RecorderStrategy() {
        override val name = "VOICE_UPLINK"
        override val audioSource = MediaRecorder.AudioSource.VOICE_UPLINK
        
        override fun canUse(): Boolean = DeviceCapabilityDetector.supportsVoiceUplink()
        
        override fun prepare(context: Context, recorder: MediaRecorder, filePath: String): Boolean {
            return try {
                recorder.setAudioSource(audioSource)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128000)
                recorder.setAudioSamplingRate(44100)
                recorder.setOutputFile(filePath)
                recorder.prepare()
                true
            } catch (e: Exception) {
                Log.w("RecorderStrategy", "$name prepare failed: ${e.message}")
                false
            }
        }
        
        override fun onStart(context: Context): Boolean = true
        override fun onStop(context: Context) {}
    }
    
    object MicWithSpeaker : RecorderStrategy() {
        override val name = "MIC_WITH_SPEAKER"
        override val audioSource = MediaRecorder.AudioSource.MIC
        
        private var originalSpeakerState = false
        private var originalAudioMode = AudioManager.MODE_NORMAL
        private var audioManager: AudioManager? = null
        
        override fun canUse(): Boolean {
            val context = RecorderEngine.getContext() ?: return false
            return DeviceCapabilityDetector.canControlSpeakerphone(context)
        }
        
        override fun prepare(context: Context, recorder: MediaRecorder, filePath: String): Boolean {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            return try {
                recorder.setAudioSource(audioSource)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128000)
                recorder.setAudioSamplingRate(44100)
                recorder.setOutputFile(filePath)
                recorder.prepare()
                true
            } catch (e: Exception) {
                Log.w("RecorderStrategy", "$name prepare failed: ${e.message}")
                false
            }
        }
        
        override fun onStart(context: Context): Boolean {
            return try {
                audioManager?.let { am ->
                    originalAudioMode = am.mode
                    originalSpeakerState = am.isSpeakerphoneOn
                    
                    // Set audio mode to IN_CALL
                    am.mode = AudioManager.MODE_IN_CALL
                    
                    // Enable speakerphone so microphone can pick up call audio
                    if (!originalSpeakerState) {
                        am.isSpeakerphoneOn = true
                        Log.d("RecorderStrategy", "Speakerphone enabled for recording")
                    }
                    Log.d("RecorderStrategy", "Audio mode set to MODE_IN_CALL, speakerphone: ${am.isSpeakerphoneOn}")
                }
                true
            } catch (e: Exception) {
                Log.e("RecorderStrategy", "Failed to configure audio: ${e.message}")
                false
            }
        }
        
        override fun onStop(context: Context) {
            try {
                audioManager?.let { am ->
                    if (am.isSpeakerphoneOn != originalSpeakerState) {
                        am.isSpeakerphoneOn = originalSpeakerState
                        Log.d("RecorderStrategy", "Speakerphone restored to original state: $originalSpeakerState")
                    }
                    if (am.mode != originalAudioMode) {
                        am.mode = originalAudioMode
                        Log.d("RecorderStrategy", "Audio mode restored to: $originalAudioMode")
                    }
                }
            } catch (e: Exception) {
                Log.e("RecorderStrategy", "Failed to restore audio settings: ${e.message}")
            }
        }
    }
    
    object RemoteSubmix : RecorderStrategy() {
        override val name = "REMOTE_SUBMIX"
        override val audioSource = MediaRecorder.AudioSource.REMOTE_SUBMIX
        
        private var originalAudioMode = AudioManager.MODE_NORMAL
        private var audioManager: AudioManager? = null
        
        override fun canUse(): Boolean {
            // REMOTE_SUBMIX requires system permissions, but we can try it
            // It may work on some devices even without system permissions
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        }
        
        override fun prepare(context: Context, recorder: MediaRecorder, filePath: String): Boolean {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            return try {
                recorder.setAudioSource(audioSource)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128000)
                recorder.setAudioSamplingRate(44100)
                recorder.setOutputFile(filePath)
                recorder.prepare()
                true
            } catch (e: SecurityException) {
                Log.w("RecorderStrategy", "$name requires system permissions: ${e.message}")
                false
            } catch (e: Exception) {
                Log.w("RecorderStrategy", "$name prepare failed: ${e.message}")
                false
            }
        }
        
        override fun onStart(context: Context): Boolean {
            return try {
                audioManager?.let { am ->
                    originalAudioMode = am.mode
                    am.mode = AudioManager.MODE_IN_CALL
                    Log.d("RecorderStrategy", "Audio mode set to MODE_IN_CALL for REMOTE_SUBMIX")
                }
                true
            } catch (e: Exception) {
                Log.e("RecorderStrategy", "Failed to set audio mode: ${e.message}")
                true // Continue anyway
            }
        }
        
        override fun onStop(context: Context) {
            try {
                audioManager?.let { am ->
                    if (am.mode != originalAudioMode) {
                        am.mode = originalAudioMode
                        Log.d("RecorderStrategy", "Audio mode restored to: $originalAudioMode")
                    }
                }
            } catch (e: Exception) {
                Log.e("RecorderStrategy", "Failed to restore audio mode: ${e.message}")
            }
        }
    }
}
