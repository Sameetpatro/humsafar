package com.example.humsafar.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.humsafar.ui.theme.Accent
import com.example.humsafar.ui.theme.DefaultAccent
import com.example.humsafar.ui.theme.accentByName

/**
 * App-wide UI preferences (onboarding completion + chosen accent).
 *
 * Modeled on [LanguagePreferences] — same SharedPreferences file, distinct
 * keys, no observers (we read once at app start and update via the accent
 * picker, which lifts state into MainActivity via a callback).
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    var accentName: String
        get() = prefs.getString(KEY_ACCENT_NAME, DefaultAccent.name) ?: DefaultAccent.name
        set(value) = prefs.edit { putString(KEY_ACCENT_NAME, value) }

    fun getAccent(): Accent = accentByName(accentName)

    fun setAccent(accent: Accent) {
        accentName = accent.name
    }

    companion object {
        private const val PREFS_NAME = "humsafar_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_complete"
        private const val KEY_ACCENT_NAME = "accent_name"
    }
}
