package com.example.artdecode.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.artdecode.presentation.home.HomeViewModel
import com.example.artdecode.data.repository.ArtworkRepository

class HomeViewModelFactory(
    private val artworkRepository: ArtworkRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(artworkRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
