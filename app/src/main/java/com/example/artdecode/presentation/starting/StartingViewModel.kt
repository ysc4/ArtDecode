package com.example.artdecode.presentation.starting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StartingViewModel : ViewModel() {

    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean> = _navigateToLogin

    fun onStartButtonClicked() {
        _navigateToLogin.value = true
    }

    fun onNavigationHandled() {
        _navigateToLogin.value = false
    }
}