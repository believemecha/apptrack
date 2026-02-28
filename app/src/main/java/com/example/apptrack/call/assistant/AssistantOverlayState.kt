package com.example.apptrack.call.assistant

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state for the Call Assistant overlay. Updated by TTS/Speech pipeline.
 */
data class AssistantOverlayState(
    val phase: Phase = Phase.IDLE,
    val liveTranscript: String = "",
    val waveformLevel: Float = 0f,
    val errorMessage: String? = null
) {
    enum class Phase {
        IDLE,
        SHOWING,
        ANSWERING,
        SPEAKING_GREETING,
        LISTENING,
        SPEAKING_CONFIRMATION,
        DONE,
        FAILED
    }
}

object AssistantOverlayStateHolder {
    private val _state = MutableStateFlow(AssistantOverlayState())
    val state: StateFlow<AssistantOverlayState> = _state.asStateFlow()

    fun setPhase(phase: AssistantOverlayState.Phase) {
        _state.value = _state.value.copy(phase = phase)
    }

    fun setLiveTranscript(text: String) {
        _state.value = _state.value.copy(liveTranscript = text)
    }

    fun appendTranscript(text: String) {
        val current = _state.value.liveTranscript
        _state.value = _state.value.copy(
            liveTranscript = if (current.isEmpty()) text else "$current $text"
        )
    }

    fun setWaveformLevel(level: Float) {
        _state.value = _state.value.copy(waveformLevel = level.coerceIn(0f, 1f))
    }

    fun setError(message: String?) {
        _state.value = _state.value.copy(errorMessage = message, phase = AssistantOverlayState.Phase.FAILED)
    }

    fun reset() {
        _state.value = AssistantOverlayState()
    }
}
