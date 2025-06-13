package com.example.artdecode.data.repository

import com.example.artdecode.data.model.Artwork
import kotlinx.coroutines.flow.Flow

interface ArtworkRepository {
    fun getArtworks(): Flow<List<Artwork>>
    suspend fun toggleFavorite(artworkId: Int)
    suspend fun getFavoriteState(artworkId: Int): Boolean
    suspend fun saveFavoriteState(artworkId: Int, isFavorite: Boolean)
}