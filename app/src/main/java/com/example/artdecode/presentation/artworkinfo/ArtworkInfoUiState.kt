package com.example.artdecode.presentation.artworkinfo

import com.example.artdecode.data.model.Artwork

data class ArtworkInfoUiState(
    val artwork: Artwork? = null, // Holds the full artwork data
    val similarArtworks: List<Artwork> = emptyList(), // List of Artwork objects
    val navigateBack: Boolean = false,
    val navigateToScan: Boolean = false,
    val navigateToReport: Boolean = false,
    val navigateToSimilarArtwork: String? = null, // Holds String? ID of artwork to navigate to
    val errorMessage: String? = null // For displaying transient errors
)