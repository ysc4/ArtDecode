package com.example.artdecode.presentation.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class SettingsViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _logoutComplete = MutableLiveData<Boolean>()
    val logoutComplete: LiveData<Boolean> = _logoutComplete

    fun logout() {
        auth.signOut()
        _logoutComplete.value = true
    }

    fun onLogoutHandled() {
        _logoutComplete.value = false
    }

    fun getCurrentUser() = auth.currentUser

    fun isUserSignedIn(): Boolean = auth.currentUser != null
}