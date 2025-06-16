package com.example.artdecode.data.repository

import com.example.artdecode.data.model.Artwork
import kotlinx.coroutines.flow.Flow

interface ArtworkRepository {
    fun getArtworks(): Flow<List<Artwork>>
    suspend fun saveArtwork(artwork: Artwork): Artwork
    suspend fun getArtworkById(id: String): Artwork?
    suspend fun deleteArtwork(artworkId: String)
    fun getSimilarArtworks(artStyle: String, excludeArtworkId: String?): Flow<List<Artwork>>
    fun updateArtworkInFlow(artwork: Artwork) // This utility function is useful for local flow updates

    // REMOVED: Methods for Favorite Status
    fun getArtworkFlowById(artworkId: String): Flow<Artwork?> // No longer strictly needed for a 'favorite' flow, but still useful for general live artwork data
    // suspend fun toggleFavorite(artworkId: String)
    // suspend fun getFavoriteState(artworkId: String): Boolean
    // suspend fun saveFavoriteState(artworkId: String, isFavorite: Boolean)
}