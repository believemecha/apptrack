package com.example.apptrack.call.assistant

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.example.apptrack.MainActivity
import com.example.apptrack.call.CallControlManager
import com.example.apptrack.call.CallManager
import com.example.apptrack.ui.theme.AppTrackTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that shows the Call Assistant overlay using ComposeView.
 * Extends LifecycleService and implements SavedStateRegistryOwner so ComposeView
 * can resolve lifecycle/savedstate from the view tree (set via ViewTree* APIs).
 * Uses single-arg setContent { } only; no Recomposer (not in public API).
 */
class CallAssistantOverlayService : LifecycleService(), SavedStateRegistryOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var pipeline: AssistantTtsSpeechPipeline? = null
    private var transcriptRepo: AssistantTranscriptRepository? = null
    private var prefs: AssistantPreferences? = null

    private var phoneNumber: String = ""
    private var contactName: String? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        transcriptRepo = AssistantTranscriptRepository(this)
        prefs = AssistantPreferences(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        phoneNumber = intent?.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        contactName = intent?.getStringExtra(EXTRA_CONTACT_NAME)

        if (phoneNumber.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        AssistantOverlayStateHolder.reset()
        AssistantOverlayStateHolder.setPhase(AssistantOverlayState.Phase.SHOWING)
        showOverlay()

        serviceScope.launch {
            delay(800)
            val hasRecordAudio = ContextCompat.checkSelfPermission(this@CallAssistantOverlayService, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (!hasRecordAudio) {
                Log.e(TAG, "RECORD_AUDIO not granted - Call Assistant cannot use microphone.")
                AssistantOverlayStateHolder.setPhase(AssistantOverlayState.Phase.DONE)
                delay(2000)
                stopAssistant()
                return@launch
            }
            // Set audio mode and routing BEFORE answer so TTS uses VOICE_CALL path (earpiece)
            CallControlManager.prepareAudioForCallAssistant()
            val answered = CallAssistantController.answerCall()
            if (answered) {
                // Re-apply after answer (Telecom may change mode); earpiece so mic picks up TTS for uplink
                CallControlManager.prepareAudioForCallAssistant()
                CallControlManager.setSpeaker(false)
                delay(1200)
                AssistantOverlayStateHolder.setPhase(AssistantOverlayState.Phase.SPEAKING_GREETING)
                delay(300)
                if (prefs?.isTranscriptionEnabled == true) {
                    pipeline = AssistantTtsSpeechPipeline(
                        context = this@CallAssistantOverlayService,
                        greeting = prefs!!.greetingText,
                        confirmation = "Got it. I'll notify them.",
                        onComplete = { transcript ->
                            saveTranscriptAndNotify(transcript)
                        }
                    )
                    pipeline!!.start()
                } else {
                    AssistantOverlayStateHolder.setPhase(AssistantOverlayState.Phase.DONE)
                }
            } else {
                AssistantOverlayStateHolder.setError("Could not answer call")
            }
        }

        return START_NOT_STICKY
    }

    private fun saveTranscriptAndNotify(transcript: String) {
        if (transcript.isBlank()) return
        serviceScope.launch {
            transcriptRepo?.insert(phoneNumber, transcript, contactName)
            AssistantNotificationHelper.showTranscriptNotification(
                this@CallAssistantOverlayService,
                phoneNumber,
                contactName,
                transcript
            )
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return
        val wm = windowManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted")
            stopSelf()
            return
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        val composeView = ComposeView(this).apply {
            setViewTreeOwners(this)  // this = ComposeView (the view to attach owners to)
            setContent {
                AppTrackTheme {
                    AssistantOverlay(
                        phoneNumber = phoneNumber,
                        contactName = contactName,
                        onStopAssistant = { stopAssistant() },
                        onBlockNumber = { blockAndStop() },
                        onMarkSpam = { markSpamAndStop() },
                        onAnswerManually = { answerManually() }
                    )
                }
            }
        }

        overlayView = composeView
        wm.addView(composeView, params)
        Log.d(TAG, "Overlay shown for $phoneNumber")
    }

    private fun stopAssistant() {
        pipeline?.release()
        pipeline = null
        CallAssistantController.endCall()
        removeOverlay()
        stopSelf()
    }

    private fun blockAndStop() {
        CallManager.getInstance(this).blockNumber(phoneNumber)
        stopAssistant()
    }

    private fun markSpamAndStop() {
        CallManager.getInstance(this).blockNumber(phoneNumber)
        stopAssistant()
    }

    private fun answerManually() {
        pipeline?.release()
        pipeline = null
        CallAssistantController.clearIncoming()
        removeOverlay()
        stopSelf()
        val openInCall = Intent(this, com.example.apptrack.ui.screens.InCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra("phoneNumber", phoneNumber)
            putExtra("contactName", contactName)
            putExtra("callType", "INCOMING")
        }
        startActivity(openInCall)
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        val wm = windowManager
        try {
            wm?.removeView(view)
        } catch (e: Exception) {
            Log.e(TAG, "removeView failed: ${e.message}")
        }
        overlayView = null
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Assistant Active")
            .setContentText("Screening call from $phoneNumber")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        pipeline?.release()
        pipeline = null
        removeOverlay()
        CallAssistantController.clearIncoming()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    /**
     * Set LifecycleOwner and SavedStateRegistryOwner on the view via reflection so ComposeView
     * can resolve them (avoids compile-time dependency on ViewTree* classes that may not resolve).
     */
    private fun setViewTreeOwners(view: View) {
        try {
            Class.forName("androidx.lifecycle.ViewTreeLifecycleOwner")
                .getMethod("set", View::class.java, Class.forName("androidx.lifecycle.LifecycleOwner"))
                .invoke(null, view, this)
        } catch (e: Exception) {
            Log.w(TAG, "ViewTreeLifecycleOwner.set failed: ${e.message}")
        }
        try {
            Class.forName("androidx.savedstate.ViewTreeSavedStateRegistryOwner")
                .getMethod("set", View::class.java, Class.forName("androidx.savedstate.SavedStateRegistryOwner"))
                .invoke(null, view, this)
        } catch (e: Exception) {
            Log.w(TAG, "ViewTreeSavedStateRegistryOwner.set failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "AssistantOverlaySvc"
        private const val CHANNEL_ID = "call_assistant"
        private const val NOTIFICATION_ID = 9001
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_CONTACT_NAME = "contact_name"
    }
}
