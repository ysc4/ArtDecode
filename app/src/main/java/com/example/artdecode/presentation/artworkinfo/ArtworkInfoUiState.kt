// src/main/java/com/example/artdecode/data/model/ArtworkInfoUiState.kt (or wherever you defined it)

package com.example.artdecode.presentation.artworkinfo

import com.example.artdecode.data.model.Artwork
import com.example.artdecode.utils.Event

data class ArtworkInfoUiState(
    val artwork: Artwork? = null,
    val similarArtworks: List<Artwork> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: Event<String>? = null, // Changed to Event<String>
    val navigateToScan: Event<Unit>? = null,
    val navigateToReport: Event<Unit>? = null,
    val navigateBack: Event<Unit>? = null,
    val navigateToSimilarArtwork: Event<String?>? = null // Event for String?
)