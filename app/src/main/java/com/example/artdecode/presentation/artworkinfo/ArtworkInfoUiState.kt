package com.example.artdecode.presentation.artworkinfo

import com.example.artdecode.data.model.Artwork
import com.example.artdecode.utils.Event

data class ArtworkInfoUiState(
    val artwork: Artwork? = null,
    val similarArtworks: List<Artwork> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: Event<String>? = null,
    val navigateToScan: Event<Unit>? = null,
    val navigateToReport: Event<Unit>? = null,
    val navigateBack: Event<Unit>? = null,
    val navigateToSimilarArtwork: Event<String?>? = null
)