package com.example.artdecode.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.artdecode.data.model.Artwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class ArtworkRepositoryImpl(
    private val context: Context
) : ArtworkRepository {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("artwork_preferences", Context.MODE_PRIVATE)
    }

    private val artworksFlow = MutableStateFlow(getInitialArtworks())

    private fun getInitialArtworks(): List<Artwork> {
        return listOf(
            Artwork(1, "artwork1.jpg", "Impressionism", 89.91f),
            Artwork(2, "artwork2.jpg", "Expressionism", 74.55f),
            Artwork(3, "artwork3.jpg", "Baroque", 98.04f),
            Artwork(4, "artwork4.jpg", "Abstract", 90.11f),
            Artwork(5, "artwork5.jpg", "Abstract", 93.12f)
        ).map { artwork ->
            artwork.copy(isFavorite = getFavoriteStateSync(artwork.id))
        }
    }

    override fun getArtworks(): Flow<List<Artwork>> = artworksFlow

    override suspend fun toggleFavorite(artworkId: Int) {
        val currentArtworks = artworksFlow.value.toMutableList()
        val artworkIndex = currentArtworks.indexOfFirst { it.id == artworkId }

        if (artworkIndex != -1) {
            val artwork = currentArtworks[artworkIndex]
            val updatedArtwork = artwork.copy(isFavorite = !artwork.isFavorite)
            currentArtworks[artworkIndex] = updatedArtwork

            saveFavoriteState(artworkId, updatedArtwork.isFavorite)
            artworksFlow.value = currentArtworks
        }
    }

    override suspend fun getFavoriteState(artworkId: Int): Boolean {
        return sharedPreferences.getBoolean("favorite_$artworkId", false)
    }

    override suspend fun saveFavoriteState(artworkId: Int, isFavorite: Boolean) {
        sharedPreferences.edit()
            .putBoolean("favorite_$artworkId", isFavorite)
            .apply()
    }

    private fun getFavoriteStateSync(artworkId: Int): Boolean {
        return sharedPreferences.getBoolean("favorite_$artworkId", false)
    }
}