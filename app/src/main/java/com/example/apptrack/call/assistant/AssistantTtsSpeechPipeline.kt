package com.example.apptrack.call.assistant

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.apptrack.call.CallControlManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Earpiece loopback: TTS uses USAGE_VOICE_COMMUNICATION so it routes through earpiece (speaker OFF).
 * Mic captures earpiece output → uplink to caller while staying on call audio path.
 */
class AssistantTtsSpeechPipeline(
    private val context: Context,
    private val greeting: String,
    private val confirmation: String = "Got it. I'll notify them.",
    private val onComplete: (transcript: String) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val ttsAudioAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val ttsSpeakParams = Bundle().apply {
        putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL)
    }

    fun start() {
        AssistantOverlayStateHolder.setPhase(AssistantOverlayState.Phase.SPEAKING_GREETING)
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setAudioAttributes(ttsAudioAttrs)
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        when (utteranceId) {
                            UTTERANCE_GREETING -> {
                                scope.launch {
                                    delay(400)
                                    startListening()
                                }
                            }
                            UTTERANCE_CONFIRMATION -> {
                                scope.launch {
                                    delay(400)
                                    onComplete(AssistantOverlayStateHolder.state.value.liveTranscript)
                                    AssistantOverlayStateHolder.setPhase(AssistantOverlayState.Phase.DONE)
                                }
                            }
                        }
                    }
                    @Deprecated("Deprecated in API 21")
                    override fun onError(utteranceId: String?) {
                        AssistantOverlayStateHolder.setError("TTS error")
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        AssistantOverlayStateHolder.setError("TTS error")
                    }
                })
                // Don't request audio focus during Telecom call: focus changes can break call routing.
                CallControlManager.prepareAudioForCallAssistant()
                tts?.speak(greeting, TextToSpeech.QUEUE_FLUSH, ttsSpeakParams, UTTERANCE_GREETING)
            } else {
                Log.e(TAG, "TTS init failed: $status")
                AssistantOverlayStateHolder.setError("TTS failed to init")
            }
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            AssistantOverlayStateHolder.setError("Speech recognition not available")
            return
        }
        AssistantOverlayStateHolder.setPhase(AssistantOverlayState.Phase.LISTENING)
        AssistantOverlayStateHolder.setLiveTranscript("")
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {
                    AssistantOverlayStateHolder.setWaveformLevel(0.7f)
                }
                override fun onRmsChanged(rmsdB: Float) {
                    AssistantOverlayStateHolder.setWaveformLevel((rmsdB / 10f).coerceIn(0f, 1f))
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    AssistantOverlayStateHolder.setWaveformLevel(0f)
                }
                override fun onError(error: Int) {
                    AssistantOverlayStateHolder.setWaveformLevel(0f)
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        AssistantOverlayStateHolder.appendTranscript("(no speech detected)")
                    }
                    scope.launch {
                        delay(200)
                        speakConfirmation()
                    }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        AssistantOverlayStateHolder.setLiveTranscript(text)
                    }
                    speakConfirmation()
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { AssistantOverlayStateHolder.setLiveTranscript(it) }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 8000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun speakConfirmation() {
        if (AssistantOverlayStateHolder.state.value.phase != AssistantOverlayState.Phase.LISTENING) return
        AssistantOverlayStateHolder.setPhase(AssistantOverlayState.Phase.SPEAKING_CONFIRMATION)
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        CallControlManager.prepareAudioForCallAssistant()
        tts?.speak(confirmation, TextToSpeech.QUEUE_FLUSH, ttsSpeakParams, UTTERANCE_CONFIRMATION)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        speechRecognizer?.destroy()
        speechRecognizer = null
        AssistantOverlayStateHolder.reset()
    }

    companion object {
        private const val TAG = "AssistantTtsSpeech"
        private const val UTTERANCE_GREETING = "greeting"
        private const val UTTERANCE_CONFIRMATION = "confirmation"
    }
}
