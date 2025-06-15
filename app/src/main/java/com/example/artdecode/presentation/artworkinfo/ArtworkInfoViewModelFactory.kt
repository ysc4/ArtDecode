package com.example.artdecode.presentation.artworkinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.artdecode.data.repository.ArtworkRepository

class ArtworkInfoViewModelFactory(private val repository: ArtworkRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArtworkInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArtworkInfoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
