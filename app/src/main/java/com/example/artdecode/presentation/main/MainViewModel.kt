package com.example.artdecode.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class MainViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _currentFragmentType = MutableLiveData<String>()
    val currentFragmentType: LiveData<String> = _currentFragmentType

    private val _navigateToScan = MutableLiveData<Boolean>()
    val navigateToScan: LiveData<Boolean> = _navigateToScan

    init {
        _currentFragmentType.value = "home"
    }

    fun showHomeFragment() {
        _currentFragmentType.value = "home"
    }

    fun showSettingsFragment() {
        _currentFragmentType.value = "settings"
    }

    fun navigateToScanActivity() {
        _navigateToScan.value = true
    }

    fun onScanNavigationHandled() {
        _navigateToScan.value = false
    }

    fun getCurrentUser() = auth.currentUser

    fun isUserAuthenticated(): Boolean = auth.currentUser != null
}