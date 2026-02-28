package com.example.apptrack.call.assistant

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persists Call Assistant settings (enable, greeting, transcription, auto-block spam).
 */
class AssistantPreferences(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isAssistantEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    var greetingText: String
        get() = prefs.getString(KEY_GREETING, DEFAULT_GREETING) ?: DEFAULT_GREETING
        set(value) = prefs.edit { putString(KEY_GREETING, value) }

    var isTranscriptionEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRANSCRIPTION, true)
        set(value) = prefs.edit { putBoolean(KEY_TRANSCRIPTION, value) }

    var isAutoBlockSpamEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BLOCK_SPAM, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_BLOCK_SPAM, value) }

    companion object {
        private const val PREFS_NAME = "call_assistant_prefs"
        private const val KEY_ENABLED = "assistant_enabled"
        private const val KEY_GREETING = "greeting_text"
        private const val KEY_TRANSCRIPTION = "transcription_enabled"
        private const val KEY_AUTO_BLOCK_SPAM = "auto_block_spam"

        const val DEFAULT_GREETING = "Hi, the person you're calling is unavailable right now. May I take a message?"
    }
}
