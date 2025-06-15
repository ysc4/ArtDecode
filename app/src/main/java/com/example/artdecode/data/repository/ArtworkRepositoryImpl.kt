package com.example.artdecode.data.repository

import android.content.Context
import android.util.Log
import com.example.artdecode.data.model.Artwork
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow // Import for MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow // Keep for now if needed elsewhere, but getArtworks will change
import kotlinx.coroutines.tasks.await // For await() on Firebase Tasks
import java.util.UUID // Import UUID for generating new IDs if needed

class ArtworkRepositoryImpl(
    private val context: Context
) : ArtworkRepository {

    // Reference to the "artworks" node in Firebase Realtime Database
    private val databaseRef = FirebaseDatabase.getInstance().getReference("artworks")

    // Use a MutableStateFlow to hold and emit the list of artworks
    // This allows for immediate local updates before Firebase listeners might fire
    private val _allArtworksFlow = MutableStateFlow<List<Artwork>>(emptyList())

    // Initialize the Firebase listener to continuously update _allArtworksFlow
    init {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val artworks = snapshot.children.mapNotNull { childSnapshot ->
                    // Directly map to Artwork class; isFavorite should be part of the Firebase data
                    childSnapshot.getValue(Artwork::class.java)?.copy(id = childSnapshot.key)
                }
                _allArtworksFlow.value = artworks // Update the internal flow
                Log.d(
                    "ArtworkRepository",
                    "Firebase listener updated _allArtworksFlow with ${artworks.size} artworks."
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "ArtworkRepository",
                    "Failed to read artworks from Firebase: ${error.message}",
                    error.toException()
                )
            }
        })
    }

    // Now, getArtworks directly exposes the internal _allArtworksFlow
    override fun getArtworks(): Flow<List<Artwork>> = _allArtworksFlow

    override suspend fun getArtworkById(artworkId: String): Artwork? {
        return try {
            val snapshot = databaseRef.child(artworkId).get().await()
            // isFavorite will be loaded directly into the Artwork object from Firebase
            snapshot.getValue(Artwork::class.java)?.copy(id = snapshot.key)
        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error getting artwork by ID $artworkId: ${e.message}")
            null
        }
    }

    // Save/Update artwork in Firebase
    // Changed return type to Artwork (non-nullable) as it throws on error
    override suspend fun saveArtwork(artwork: Artwork): Artwork {
        val artworkId = artwork.id ?: databaseRef.push().key ?: UUID.randomUUID()
            .toString() // Generate unique ID if not present
        val artworkRef = databaseRef.child(artworkId)
        val artworkToSave = artwork.copy(id = artworkId) // Ensure ID is part of the object saved

        return try {
            artworkRef.setValue(artworkToSave).await() // Save to Firebase
            Log.d("ArtworkRepository", "Artwork saved to Firebase: ${artworkToSave.id}")
            artworkToSave // Return the saved artwork with its definitive ID
        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error saving artwork: ${e.message}")
            throw e // Rethrow error to be handled by ViewModel/caller
        }
    }

    // NEW METHOD: Delete artwork from Firebase and update local flow
    override suspend fun deleteArtwork(artworkId: String) {
        try {
            databaseRef.child(artworkId).removeValue().await()
            Log.d("ArtworkRepository", "Artwork deleted from Firebase: $artworkId")

            // Update local flow immediately for faster UI response
            _allArtworksFlow.value = _allArtworksFlow.value.filter { it.id != artworkId }
            Log.d("ArtworkRepository", "Locally removed artwork $artworkId from _allArtworksFlow")
        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error deleting artwork $artworkId: ${e.message}")
            throw e
        }
    }

    override suspend fun toggleFavorite(artworkId: String) {
        try {
            val snapshot = databaseRef.child(artworkId).child("isFavorite").get().await()
            val currentFavoriteState = snapshot.getValue(Boolean::class.java) ?: false
            val newFavoriteState = !currentFavoriteState

            databaseRef.child(artworkId).child("isFavorite").setValue(newFavoriteState).await()
            Log.d("ArtworkRepository", "Toggled favorite for $artworkId to $newFavoriteState")

            // Optionally, update the local _allArtworksFlow immediately for faster UI response
            // The Firebase listener will eventually update it, but this is for instant feedback.
            val updatedArtwork = _allArtworksFlow.value.find { it.id == artworkId }
                ?.copy(isFavorite = newFavoriteState)
            updatedArtwork?.let {
                updateArtworkInFlow(it)
            }
        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error toggling favorite for $artworkId: ${e.message}")
            throw e // Rethrow or handle error appropriately
        }
    }

    override suspend fun getFavoriteState(artworkId: String): Boolean {
        return try {
            val snapshot = databaseRef.child(artworkId).child("isFavorite").get().await()
            snapshot.getValue(Boolean::class.java) ?: false
        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error getting favorite state for $artworkId: ${e.message}")
            false // Default to false on error
        }
    }

    override suspend fun saveFavoriteState(artworkId: String, isFavorite: Boolean) {
        try {
            databaseRef.child(artworkId).child("isFavorite").setValue(isFavorite).await()
            Log.d("ArtworkRepository", "Saved favorite state for $artworkId to $isFavorite")
        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error saving favorite state for $artworkId: ${e.message}")
            throw e // Rethrow or handle error appropriately
        }
    }

    // This method updates the local _allArtworksFlow with a modified artwork
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
                    // Exclude the current artwork from the similar list
                    it.id != excludeArtworkId
                }
                trySend(similarArtworks).isSuccess // Emit the list of similar artworks
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "ArtworkRepository",
                    "Failed to read similar artworks for style $artStyle: ${error.message}"
                )
                close(error.toException()) // Close the flow with an exception
            }
        }

        query.addValueEventListener(valueEventListener)

        // Remove the listener when the flow is cancelled
        awaitClose { query.removeEventListener(valueEventListener) }
    }
}