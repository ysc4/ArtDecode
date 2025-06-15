package com.example.artdecode.presentation.artworkinfo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artdecode.data.model.Artwork
import com.example.artdecode.data.repository.ArtworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArtworkInfoViewModel(
    private val artworkRepository: ArtworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtworkInfoUiState())
    val uiState: StateFlow<ArtworkInfoUiState> = _uiState.asStateFlow()

    // Add current user ID to filter similar artworks
    private var currentUserId: String? = null

    fun setCurrentUserId(userId: String) {
        currentUserId = userId
    }

    fun loadArtworkInfo(
        artworkId: String? = null,
        capturedImageUri: String? = null,
        artStyle: String? = null,
        confidenceScore: Float? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = null) // Clear previous errors

            val currentArtwork: Artwork? = if (artworkId != null) {
                artworkRepository.getArtworkById(artworkId)
            } else if (capturedImageUri != null && artStyle != null && confidenceScore != null) {
                // This is a newly scanned artwork. Assuming it was already saved in ScanViewModel
                // and we're just displaying it. We create a temporary one for display
                // (it won't be saved again here unless you explicitly call saveArtwork).
                Artwork(
                    id = null, // ID should ideally be from the DB if already saved
                    imageUri = capturedImageUri,
                    artStyle = artStyle,
                    confidenceScore = confidenceScore,
                    isFavorite = false, // Default for new artwork
                    userId = currentUserId // Set the current user ID
                )
            } else {
                null
            }

            if (currentArtwork != null) {
                _uiState.value = _uiState.value.copy(artwork = currentArtwork) // Update artwork first

                // NOW FETCH SIMILAR ARTWORKS BASED ON THE LOADED ARTWORK'S STYLE
                // BUT ONLY FOR THE CURRENT USER
                currentArtwork.artStyle?.let { style ->
                    artworkRepository.getSimilarArtworks(style, currentArtwork.id)
                        .collect { allSimilarArtworks ->
                            // Filter similar artworks by current user ID
                            val filteredSimilarArtworks = if (currentUserId != null) {
                                allSimilarArtworks.filter { artwork ->
                                    artwork.userId == currentUserId
                                }
                            } else {
                                allSimilarArtworks
                            }
                            _uiState.value = _uiState.value.copy(similarArtworks = filteredSimilarArtworks)
                        }
                }

            } else {
                _uiState.value = _uiState.value.copy(
                    artwork = null,
                    similarArtworks = emptyList(), // Clear similar artworks too if main artwork is null
                    errorMessage = "Artwork not found or no data available."
                )
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            _uiState.value.artwork?.let { currentArtwork ->
                currentArtwork.id?.let { artworkId ->
                    val newFavoriteState = !currentArtwork.isFavorite
                    artworkRepository.saveFavoriteState(artworkId, newFavoriteState)
                    _uiState.value = _uiState.value.copy(
                        artwork = currentArtwork.copy(isFavorite = newFavoriteState)
                    )
                    artworkRepository.updateArtworkInFlow(currentArtwork.copy(isFavorite = newFavoriteState))
                }
            }
        }
    }

    fun onBackClick() {
        _uiState.value = _uiState.value.copy(navigateBack = true)
    }

    fun onScanMoreClick() {
        _uiState.value = _uiState.value.copy(navigateToScan = true)
    }

    fun onReportClick() {
        _uiState.value = _uiState.value.copy(navigateToReport = true)
    }

    fun onSimilarArtworkClick(artworkId: String?) {
        // When a similar artwork is clicked, load its details as the main artwork
        _uiState.value = _uiState.value.copy(navigateToSimilarArtwork = artworkId)
    }

    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(
            navigateBack = false,
            navigateToScan = false,
            navigateToReport = false,
            navigateToSimilarArtwork = null,
            errorMessage = null
        )
    }
}