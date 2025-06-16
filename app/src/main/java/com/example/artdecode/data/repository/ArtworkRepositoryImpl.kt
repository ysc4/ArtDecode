package com.example.artdecode.data.repository

import android.content.Context
import android.util.Log
import com.example.artdecode.data.model.Artwork
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ArtworkRepositoryImpl(
    private val context: Context
) : ArtworkRepository {

    private val databaseRef = FirebaseDatabase.getInstance().getReference("artworks")
    private val _allArtworksFlow = MutableStateFlow<List<Artwork>>(emptyList())

    init {
        // This listener keeps _allArtworksFlow up-to-date with Firebase changes
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val artworks = snapshot.children.mapNotNull { childSnapshot ->
                    val artwork = childSnapshot.getValue(Artwork::class.java)?.copy(id = childSnapshot.key)
                    artwork
                }
                _allArtworksFlow.value = artworks
                Log.d("ArtworkRepository", "Firebase listener updated _allArtworksFlow with ${artworks.size} artworks.")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ArtworkRepository", "Failed to read artworks from Firebase: ${error.message}", error.toException())
            }
        })
    }

    override fun getArtworks(): Flow<List<Artwork>> = _allArtworksFlow

    // Kept this as it's used by ArtworkInfoViewModel for general artwork observation
    override fun getArtworkFlowById(artworkId: String): Flow<Artwork?> {
        return _allArtworksFlow.map { artworks ->
            artworks.find { it.id == artworkId }
        }
    }

    override suspend fun getArtworkById(artworkId: String): Artwork? {
        return try {
            val snapshot = databaseRef.child(artworkId).get().await()
            snapshot.getValue(Artwork::class.java)?.copy(id = snapshot.key)
        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error getting artwork by ID $artworkId: ${e.message}")
            null
        }
    }

    override suspend fun saveArtwork(artwork: Artwork): Artwork {
        val artworkId = artwork.id ?: databaseRef.push().key ?: UUID.randomUUID().toString()
        val artworkToSave = artwork.copy(id = artworkId) // Ensure no isFavorite field is copied here if Artwork data class is updated

        return try {
            databaseRef.child(artworkId).setValue(artworkToSave).await()
            Log.d("ArtworkRepository", "Artwork saved to Firebase: ${artworkToSave.id}")
            artworkToSave
        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error saving artwork: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteArtwork(artworkId: String) {
        try {
            databaseRef.child(artworkId).removeValue().await()
            Log.d("ArtworkRepository", "Artwork deleted from Firebase: $artworkId")
            // Local flow update for immediate UI response. The Firebase listener will reconcile.
            _allArtworksFlow.value = _allArtworksFlow.value.filter { it.id != artworkId }
        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error deleting artwork $artworkId: ${e.message}")
            throw e
        }
    }

    // REMOVED: toggleFavorite function
    /*
    override suspend fun toggleFavorite(artworkId: String) {
        try {
            val snapshot = databaseRef.child(artworkId).child("isFavorite").get().await()
            val currentFavoriteState = snapshot.getValue(Boolean::class.java) ?: false
            val newFavoriteState = !currentFavoriteState

            databaseRef.child(artworkId).child("isFavorite").setValue(newFavoriteState).await()
            Log.d("ArtworkRepository", "Toggled favorite for $artworkId to $newFavoriteState (using isFavorite field)")

            // --- OPTIMISTIC LOCAL UI UPDATE ---
            val updatedList = _allArtworksFlow.value.map { artwork ->
                if (artwork.id == artworkId) {
                    artwork.copy(isFavorite = newFavoriteState)
                } else {
                    artwork
                }
            }
            _allArtworksFlow.value = updatedList
            Log.d("ArtworkRepository", "Locally updated favorite status for $artworkId to $newFavoriteState in _allArtworksFlow.")

        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error toggling favorite for $artworkId: ${e.message}")
            throw e
        }
    }
    */

    // REMOVED: getFavoriteState function
    /*
    override suspend fun getFavoriteState(artworkId: String): Boolean {
        return try {
            val snapshot = databaseRef.child(artworkId).child("isFavorite").get().await()
            snapshot.getValue(Boolean::class.java) ?: false
        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error getting favorite state for $artworkId: ${e.message}")
            false
        }
    }
    */

    // REMOVED: saveFavoriteState function
    /*
    override suspend fun saveFavoriteState(artworkId: String, isFavorite: Boolean) {
        try {
            databaseRef.child(artworkId).child("isFavorite").setValue(isFavorite).await()
            Log.d("ArtworkRepository", "Saved favorite state for $artworkId to $isFavorite (using isFavorite field)")
        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error saving favorite state for $artworkId: ${e.message}")
            throw e
        }
    }
    */

    override fun updateArtworkInFlow(artwork: Artwork) {
        _allArtworksFlow.value = _allArtworksFlow.value.map {
            if (it.id == artwork.id) artwork else it
        }
        Log.d("ArtworkRepository", "Locally updated artwork ${artwork.id} in _allArtworksFlow.")
    }

    override fun getSimilarArtworks(
        artStyle: String,
        excludeArtworkId: String?
    ): Flow<List<Artwork>> = callbackFlow {
        val query = databaseRef.orderByChild("artStyle").equalTo(artStyle)

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val similarArtworks = snapshot.children.mapNotNull { childSnapshot ->
                    childSnapshot.getValue(Artwork::class.java)?.copy(id = childSnapshot.key)
                }.filter {
                    it.id != excludeArtworkId
                }
                trySend(similarArtworks).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ArtworkRepository", "Failed to read similar artworks for style $artStyle: ${error.message}")
                close(error.toException())
            }
        }

        query.addValueEventListener(valueEventListener)
        awaitClose { query.removeEventListener(valueEventListener) }
    }
}