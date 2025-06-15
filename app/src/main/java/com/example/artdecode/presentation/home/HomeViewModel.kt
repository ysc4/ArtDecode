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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeViewModel(
    val artworkRepository: ArtworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        loadArtworks()
    }

    fun setCurrentUserId(userId: String) {
        currentUserId = userId
        Log.d("HomeViewModel", "Current user ID set to: $userId")
        // Reload artworks with the new user filter
        loadArtworks()
    }

    private fun loadArtworks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            artworkRepository.getArtworks()
                .map { artworks ->
                    // Filter artworks by current user ID
                    val filteredArtworks = if (currentUserId != null) {
                        artworks.filter { artwork ->
                            artwork.userId == currentUserId
                        }
                    } else {
                        artworks // Show all if no user ID is set
                    }

                    Log.d("HomeViewModel", "Total artworks: ${artworks.size}, Filtered for user $currentUserId: ${filteredArtworks.size}")

                    val items = mutableListOf<RecyclerViewItem>()
                    items.add(RecyclerViewItem.Header("My Collections"))
                    filteredArtworks.forEach { artwork ->
                        items.add(RecyclerViewItem.ArtworkItem(artwork))
                    }
                    items
                }
                .collect { items ->
                    _uiState.value = _uiState.value.copy(
                        items = items,
                        isLoading = false
                    )
                }
        }
    }

    fun onArtworkClick(artworkId: String?) {
        _uiState.value = _uiState.value.copy(navigateToArtworkDetail = artworkId)
    }

    fun onFavoriteClick(artworkId: String?) {
        viewModelScope.launch {
            artworkId?.let { id ->
                artworkRepository.toggleFavorite(id)
            }
        }
    }

    fun deleteArtwork(artworkId: String?) {
        viewModelScope.launch {
            artworkId?.let { id ->
                try {
                    artworkRepository.deleteArtwork(id)
                    Log.d("HomeViewModel", "Successfully deleted artwork: $id")
                    // The repository will update the flow, which will automatically update the UI
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

data class HomeUiState(
    val items: List<RecyclerViewItem> = emptyList(),
    val isLoading: Boolean = false,
    val navigateToArtworkDetail: String? = null
)