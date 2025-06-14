package com.example.artdecode.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artdecode.data.model.RecyclerViewItem
import com.example.artdecode.data.repository.ArtworkRepository
import com.example.artdecode.data.model.Artwork // Import the combined Artwork data class
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeViewModel(
    private val artworkRepository: ArtworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadArtworks()
    }

    private fun loadArtworks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            artworkRepository.getArtworks()
                .map { artworks ->
                    val items = mutableListOf<RecyclerViewItem>()
                    items.add(RecyclerViewItem.Header("My Collections"))
                    artworks.forEach { artwork ->
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

    // Changed artworkId type to String?
    fun onArtworkClick(artworkId: String?) {
        _uiState.value = _uiState.value.copy(navigateToArtworkDetail = artworkId)
    }

    // Changed artworkId type to String?
    fun onFavoriteClick(artworkId: String?) {
        viewModelScope.launch {
            // Null check for ID
            artworkId?.let { id ->
                artworkRepository.toggleFavorite(id)
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
    val navigateToArtworkDetail: String? = null // Changed ID type to String?
)