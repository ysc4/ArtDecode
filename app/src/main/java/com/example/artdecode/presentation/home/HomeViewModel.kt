package com.example.artdecode.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artdecode.data.model.RecyclerViewItem
import com.example.artdecode.data.repository.ArtworkRepository
import com.example.artdecode.data.model.Artwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map // Keep map if you're mapping to RecyclerViewItem
import kotlinx.coroutines.flow.collectLatest // Use collectLatest to cancel previous collections if new data comes in quickly
import kotlinx.coroutines.launch

class HomeViewModel(
    private val artworkRepository: ArtworkRepository // Made private val as it's not exposed
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    init {
        collectArtworksFromRepository()
    }


    fun restoreArtwork(artwork: Artwork) {
        viewModelScope.launch {
            try {
                artworkRepository.saveArtwork(artwork)
                Log.d("HomeViewModel", "Successfully restored artwork: ${artwork.id}")
                // The repository's flow will update automatically, refreshing the UI
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error restoring artwork: ${e.message}")
            }
        }
    }
    private fun collectArtworksFromRepository() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true) // Show loading on initial collection

            // Collect directly from the repository's getArtworks() flow.
            // This flow is already designed to combine with the repository's _currentUserId
            // and filter the results.
            artworkRepository.getArtworks()
                .map { artworks ->
                    Log.d("HomeViewModel", "Received ${artworks.size} artworks from repository (already filtered by user).")

                    val items = mutableListOf<RecyclerViewItem>()
                    items.add(RecyclerViewItem.Header("My Collections"))
                    if (artworks.isEmpty()) {
                        items.add(RecyclerViewItem.Message("No artworks found in your collection."))
                    } else {
                        artworks.forEach { artwork ->
                            items.add(RecyclerViewItem.ArtworkItem(artwork))
                        }
                    }
                    items
                }
                .collectLatest { items -> // Use collectLatest to cancel previous collections if repository re-emits quickly
                    _uiState.value = _uiState.value.copy(
                        items = items,
                        isLoading = false
                    )
                    Log.d("HomeViewModel", "UI State updated with ${items.size} items.")
                }
        }
    }

    fun onArtworkClick(artworkId: String?) {
        _uiState.value = _uiState.value.copy(navigateToArtworkDetail = artworkId)
    }

    fun deleteArtwork(artworkId: String?) {
        viewModelScope.launch {
            artworkId?.let { id ->
                try {
                    // Make sure the artworkRepository's _currentUserId is set before this is called
                    // (which it should be if you're using the singleton correctly).
                    artworkRepository.deleteArtwork(id)
                    Log.d("HomeViewModel", "Successfully sent request to delete artwork: $id")
                    // The repository's listener will update _allArtworksFlow,
                    // which in turn will cause getArtworks() to re-emit,
                    // and this ViewModel's collectLatest will update the UI.
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error deleting artwork: ${e.message}")
                }
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(navigateToArtworkDetail = null)
    }
}