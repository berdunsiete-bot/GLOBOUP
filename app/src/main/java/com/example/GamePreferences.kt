package com.example

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("globoup_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_TOTAL_STARS = "total_stars"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_GAMES_PLAYED = "games_played"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_SHIELDS_COUNT = "shields_count"
    }

    var shieldsCount: Int
        get() = prefs.getInt(KEY_SHIELDS_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_SHIELDS_COUNT, value).apply()

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(value) = prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()

    var totalStars: Int
        get() = prefs.getInt(KEY_TOTAL_STARS, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_STARS, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()

    var gamesPlayed: Int
        get() = prefs.getInt(KEY_GAMES_PLAYED, 0)
        set(value) = prefs.edit().putInt(KEY_GAMES_PLAYED, value).apply()

    var languageSetting: String
        get() = prefs.getString(KEY_LANGUAGE, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    fun resetStats() {
        prefs.edit()
            .putInt(KEY_HIGH_SCORE, 0)
            .putInt(KEY_TOTAL_STARS, 0)
            .putInt(KEY_GAMES_PLAYED, 0)
            .apply()
    }
}
