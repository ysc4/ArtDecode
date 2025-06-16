package com.example.artdecode.data.repository

import android.content.Context

class SettingsRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_CAMERA_ONLY_MODE = "camera_only_mode"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCameraOnlyMode(): Boolean {
        return sharedPrefs.getBoolean(KEY_CAMERA_ONLY_MODE, false)
    }

    fun setCameraOnlyMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_CAMERA_ONLY_MODE, enabled).apply()
    }
}
