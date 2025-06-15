package com.example.artdecode.data.repository

import com.example.artdecode.data.model.Artwork
import kotlinx.coroutines.flow.Flow

interface ArtworkRepository {
    fun getArtworks(): Flow<List<Artwork>>
    suspend fun saveArtwork(artwork: Artwork): Artwork
    suspend fun getArtworkById(id: String): Artwork? // This remains for one-time fetches if needed
    suspend fun deleteArtwork(artworkId: String)
    fun getSimilarArtworks(artStyle: String, excludeArtworkId: String?): Flow<List<Artwork>>
    fun updateArtworkInFlow(artwork: Artwork) // This utility function is useful for local flow updates

    // --- Changes for Favorite Status Consistency ---
    // New: Method to get a single artwork as a Flow for live updates
    fun getArtworkFlowById(artworkId: String): Flow<Artwork?>

    // Ensure toggleFavorite always uses 'isFavorite'
    suspend fun toggleFavorite(artworkId: String)

    // These might become redundant if getArtworkFlowById is used consistently for display,
    // but kept for now as they might be used in other logic. Ensure they also use 'isFavorite'.
    suspend fun getFavoriteState(artworkId: String): Boolean
    suspend fun saveFavoriteState(artworkId: String, isFavorite: Boolean)
}