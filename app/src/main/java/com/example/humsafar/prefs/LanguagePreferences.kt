// app/src/main/java/com/example/humsafar/prefs/LanguagePreferences.kt
// NEW FILE

package com.example.humsafar.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Typed SharedPreferences wrapper for voice language selection.
 *
 * Sarvam AI BCP-47 codes:
 *   English  → "en-IN"
 *   Hindi    → "hi-IN"
 *   Hinglish → "hi-IN"  (same STT model; LLM prompt controls output style)
 *
 * The distinction between HINDI and HINGLISH is communicated to the backend
 * via the `lang_name` field, not the BCP-47 code. The backend then uses it
 * to build the appropriate LLM system prompt.
 */
class LanguagePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var selectedLanguage: Language
        get() {
            val name = prefs.getString(KEY_LANGUAGE, Language.ENGLISH.name) ?: Language.ENGLISH.name
            return runCatching { Language.valueOf(name) }.getOrDefault(Language.ENGLISH)
        }
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value.name) }

    enum class Language(
        val bcp47Code:     String,   // Sent to Sarvam STT + TTS
        val displayName:   String,   // Shown in SettingsScreen
        val ttsVoice:      String    // Sarvam TTS speaker
    ) {
        ENGLISH(
            bcp47Code   = "en-IN",
            displayName = "English",
            ttsVoice    = "meera"
        ),
        HINDI(
            bcp47Code   = "hi-IN",
            displayName = "हिंदी",
            ttsVoice    = "meera"
        ),
        HINGLISH(
            bcp47Code   = "hi-IN",
            displayName = "Hinglish",
            ttsVoice    = "meera"
        );
    }

    companion object {
        private const val PREFS_NAME   = "humsafar_prefs"
        private const val KEY_LANGUAGE = "voice_language"
    }
}