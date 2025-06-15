package com.example.artdecode.presentation.artworkinfo

import ArtworkInfoUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artdecode.data.model.Artwork
import com.example.artdecode.data.repository.ArtworkRepository
import com.example.artdecode.utils.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine // Import combine
import kotlinx.coroutines.flow.launchIn // Import launchIn
import kotlinx.coroutines.flow.onEach // Import onEach
import kotlinx.coroutines.launch

class ArtworkInfoViewModel(
    private val artworkRepository: ArtworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtworkInfoUiState())
    val uiState: StateFlow<ArtworkInfoUiState> = _uiState.asStateFlow()

    private var currentArtworkId: String? = null
    // These are for unsaved, newly scanned artworks
    private var capturedImageUri: String? = null
    private var predictedArtStyle: String? = null
    private var predictedConfidence: Float? = null

    private var currentUserId: String? = null

    fun setCurrentUserId(userId: String) {
        currentUserId = userId
    }

    fun loadArtworkInfo(
        artworkId: String?,
        capturedImageUri: String?,
        artStyle: String?,
        confidenceScore: Float?
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        currentArtworkId = artworkId
        this.capturedImageUri = capturedImageUri
        this.predictedArtStyle = artStyle
        this.predictedConfidence = confidenceScore

        viewModelScope.launch {
            if (artworkId != null) {
                // If we have an artwork ID, observe its live data from the repository
                artworkRepository.getArtworkFlowById(artworkId) // NOW OBSERVING A FLOW!
                    .combine(artworkRepository.getSimilarArtworks(artStyle ?: "", artworkId)) { artwork, similarArtworks ->
                        // Filter similar artworks by current user
                        val filteredSimilarArtworks = if (currentUserId != null) {
                            similarArtworks.filter { it.userId == currentUserId }
                        } else {
                            similarArtworks
                        }

                        // Combine the live artwork object and similar artworks into the UI state
                        ArtworkInfoUiState(
                            artwork = artwork, // This 'artwork' is now a live object
                            similarArtworks = filteredSimilarArtworks,
                            isLoading = false
                        )
                    }
                    .onEach { newState ->
                        _uiState.value = newState // Update UI state when either flow emits a new value
                    }
                    .launchIn(viewModelScope) // Collect this flow within the ViewModel's scope
            } else {
                // This path handles newly scanned artworks not yet saved to DB
                // Create a temporary Artwork object for display
                val tempArtwork = Artwork(
                    id = null, // No ID yet, indicates it's not saved
                    imageUri = capturedImageUri,
                    artStyle = artStyle,
                    confidenceScore = confidenceScore,
                    isFavorite = false, // Default for new scans, can be toggled later
                    userId = currentUserId // Associate with current user
                )

                // Load similar artworks based on the predicted style for new scans
                artworkRepository.getSimilarArtworks(artStyle ?: "", null)
                    .onEach { similarArtworks ->
                        val filteredSimilarArtworks = if (currentUserId != null) {
                            similarArtworks.filter { it.userId == currentUserId }
                        } else {
                            similarArtworks
                        }
                        _uiState.value = _uiState.value.copy(
                            artwork = tempArtwork, // Set the temporary artwork
                            similarArtworks = filteredSimilarArtworks,
                            isLoading = false
                        )
                    }
                    .launchIn(viewModelScope)
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentArtwork = _uiState.value.artwork
            if (currentArtwork != null) {
                if (currentArtwork.id != null) {
                    // Artwork is already in DB, toggle its favorite status
                    try {
                        artworkRepository.toggleFavorite(currentArtwork.id)
                        // UI will update automatically because ArtworkInfoViewModel is now observing
                        // the live Artwork Flow from the repository.
                        _uiState.value = _uiState.value.copy(errorMessage = null) // Clear any previous error
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(errorMessage = "Failed to toggle favorite: ${e.message}")
                    }
                } else {
                    // Newly scanned artwork not yet saved. Save it first, then it will appear as favorite.
                    try {
                        // Create a new Artwork object with the favorite status toggled and user ID set
                        val artworkToSave = currentArtwork.copy(isFavorite = !currentArtwork.isFavorite, userId = currentUserId)
                        val savedArtwork = artworkRepository.saveArtwork(artworkToSave)
                        // After saving, reload info using the new ID so it's observed live
                        _uiState.value = _uiState.value.copy(errorMessage = "Artwork saved to favorites and will now update live.")
                        // This will trigger the loadArtworkInfo for the saved artwork's ID
                        loadArtworkInfo(savedArtwork.id, null, null, null)
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(errorMessage = "Failed to save artwork to favorites: ${e.message}")
                    }
                }
            }
        }
    }

    fun onScanMoreClick() {
        _uiState.value = _uiState.value.copy(navigateToScan = Event(Unit))
    }

    fun onReportClick() {
        _uiState.value = _uiState.value.copy(navigateToReport = Event(Unit))
    }

    fun onBackClick() {
        _uiState.value = _uiState.value.copy(navigateBack = Event(Unit))
    }

    fun onSimilarArtworkClick(artworkId: String?) {
        _uiState.value = _uiState.value.copy(navigateToSimilarArtwork = Event(artworkId))
    }

    fun onNavigationHandled() {
        // Reset navigation flags and error message after they've been consumed by the Activity
        _uiState.value = _uiState.value.copy(
            navigateBack = null,
            navigateToScan = null,
            navigateToReport = null,
            navigateToSimilarArtwork = null,
            errorMessage = null
        )
    }
}