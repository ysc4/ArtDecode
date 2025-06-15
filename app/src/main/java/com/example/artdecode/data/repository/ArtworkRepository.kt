package com.example.artdecode.data.repository

import com.example.artdecode.data.model.Artwork
import kotlinx.coroutines.flow.Flow

interface ArtworkRepository {
    // Existing methods (updated to use Artwork or String ID for favorites)
    fun getArtworks(): Flow<List<Artwork>> // Still provides dummy local data initially
    suspend fun toggleFavorite(artworkId: String)
    suspend fun getFavoriteState(artworkId: String): Boolean
    suspend fun saveFavoriteState(artworkId: String, isFavorite: Boolean)

    // NEW method for swipe deletion
    suspend fun deleteArtwork(artworkId: String)

    // NEW methods for Firebase
    suspend fun saveArtwork(artwork: Artwork): Artwork
    suspend fun getArtworkById(id: String): Artwork?
    fun getSimilarArtworks(artStyle: String, excludeArtworkId: String?): Flow<List<Artwork>>
    fun updateArtworkInFlow(artwork: Artwork)
}