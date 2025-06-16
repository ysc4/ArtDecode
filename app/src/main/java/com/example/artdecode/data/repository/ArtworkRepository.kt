package com.example.artdecode.data.repository

import com.example.artdecode.data.model.Artwork
import kotlinx.coroutines.flow.Flow

interface ArtworkRepository {
    fun getArtworks(): Flow<List<Artwork>>
    suspend fun saveArtwork(artwork: Artwork): Artwork
    suspend fun getArtworkById(id: String): Artwork?
    suspend fun deleteArtwork(artworkId: String)
    fun getSimilarArtworks(artStyle: String, excludeArtworkId: String?): Flow<List<Artwork>>
    fun updateArtworkInFlow(artwork: Artwork)
    fun setCurrentUserId(userId: String?)

    fun getArtworkFlowById(artworkId: String): Flow<Artwork?>
}