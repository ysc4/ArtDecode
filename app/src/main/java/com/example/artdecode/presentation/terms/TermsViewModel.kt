package com.example.artdecode.presentation.terms

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TermsViewModel : ViewModel() {

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean> = _navigateBack

    fun onBackButtonClicked() {
        _navigateBack.value = true
    }

    fun onNavigationHandled() {
        _navigateBack.value = false
    }
}