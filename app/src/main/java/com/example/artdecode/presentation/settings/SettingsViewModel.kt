package com.example.artdecode.presentation.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_CAMERA_ONLY_MODE = "camera_only_mode"
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val sharedPrefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _logoutComplete = MutableLiveData<Boolean>()
    val logoutComplete: LiveData<Boolean> = _logoutComplete

    private val _cameraOnlyMode = MutableLiveData<Boolean>()
    val cameraOnlyMode: LiveData<Boolean> = _cameraOnlyMode

    fun logout() {
        auth.signOut()
        _logoutComplete.value = true
    }

    fun onLogoutHandled() {
        _logoutComplete.value = false
    }

    fun getCurrentUser() = auth.currentUser

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    fun setCameraOnlyMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_CAMERA_ONLY_MODE, enabled).apply()
        _cameraOnlyMode.value = enabled
    }

    fun getCameraOnlyMode(): Boolean {
        return sharedPrefs.getBoolean(KEY_CAMERA_ONLY_MODE, false)
    }

    fun loadSettings() {
        _cameraOnlyMode.value = getCameraOnlyMode()
    }
}
