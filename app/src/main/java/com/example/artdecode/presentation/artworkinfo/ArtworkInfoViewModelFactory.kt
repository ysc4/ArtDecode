package com.example.artdecode.presentation.artworkinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.artdecode.data.repository.ArtworkRepository

class ArtworkInfoViewModelFactory(
    private val repository: ArtworkRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArtworkInfoViewModel::class.java)) {
            return ArtworkInfoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
