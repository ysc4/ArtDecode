package com.example.artdecode.presentation.artworkinfo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun loadArtworkInfo(imageUri: Uri?) {
        _uiState.value = _uiState.value.copy(
            imageUri = imageUri,
            similarArtworks = getSimilarArtworks()
        )
        loadFavoriteState()
    }

    private fun loadFavoriteState() {
        viewModelScope.launch {
            val isFavorite = artworkRepository.getFavoriteState(1)
            _uiState.value = _uiState.value.copy(isFavorite = isFavorite)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentState = _uiState.value.isFavorite
            val newState = !currentState

            artworkRepository.saveFavoriteState(1, newState)
            _uiState.value = _uiState.value.copy(isFavorite = newState)
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

    fun onSimilarArtworkClick(artworkId: String) {
        _uiState.value = _uiState.value.copy(navigateToSimilarArtwork = artworkId)
    }

    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(
            navigateBack = false,
            navigateToScan = false,
            navigateToReport = false,
            navigateToSimilarArtwork = null
        )
    }

    private fun getSimilarArtworks(): List<String> {
        return listOf("artwork1", "artwork2", "artwork3", "artwork4")
    }
}

data class ArtworkInfoUiState(
    val imageUri: Uri? = null,
    val isFavorite: Boolean = false,
    val similarArtworks: List<String> = emptyList(),
    val navigateBack: Boolean = false,
    val navigateToScan: Boolean = false,
    val navigateToReport: Boolean = false,
    val navigateToSimilarArtwork: String? = null
)