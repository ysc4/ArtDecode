package com.example.artdecode.presentation.privacy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PrivacyPolicyViewModel : ViewModel() {

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean> = _navigateBack

    // Since content is in XML, we only need navigation logic
    fun onBackButtonClicked() {
        _navigateBack.value = true
    }

    fun onNavigationHandled() {
        _navigateBack.value = false
    }
}