package com.example.artdecode.data.repository

import android.content.Context
import android.util.Log
import com.example.artdecode.data.model.Artwork
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ArtworkRepositoryImpl(
    private val context: Context
) : ArtworkRepository {

    private val databaseRef = FirebaseDatabase.getInstance().getReference("artworks")
    private val _allArtworksFlow = MutableStateFlow<List<Artwork>>(emptyList())
    private val _currentUserId = MutableStateFlow<String?>(null)

    init {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val artworks = snapshot.children.mapNotNull { childSnapshot ->
                    val artwork =
                        childSnapshot.getValue(Artwork::class.java)?.copy(id = childSnapshot.key)
                    artwork
                }
                _allArtworksFlow.value = artworks
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

    override fun setCurrentUserId(userId: String?) {
        _currentUserId.value = userId
        Log.d("ArtworkRepository", "Current user ID set to: $userId")
    }

    override fun getArtworks(): Flow<List<Artwork>> {
        return combine(_allArtworksFlow, _currentUserId) { allArtworks, userId ->
            if (userId == null) {
                Log.d("ArtworkRepository", "getArtworks: No user logged in. Returning empty list.")
                emptyList()
            } else {
                val userArtworks = allArtworks.filter { artwork ->
                    artwork.userId == userId
                }
                Log.d("ArtworkRepository", "getArtworks: Filtered ${allArtworks.size} artworks to ${userArtworks.size} for user $userId.")
                userArtworks
            }
        }
    }

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
        return suspendCoroutine { continuation ->
            val artworkRef: DatabaseReference
            val finalArtwork: Artwork

            if (artwork.id == null) {
                artworkRef = databaseRef.push()
                val newId = artworkRef.key ?: UUID.randomUUID().toString()
                finalArtwork = artwork.copy(id = newId)
                Log.d("ArtworkRepo", "Saving NEW artwork with generated ID: $newId")
            } else {
                artworkRef = databaseRef.child(artwork.id)
                finalArtwork = artwork
                Log.d("ArtworkRepo", "Saving EXISTING artwork with ID: ${artwork.id}")
            }

            artworkRef.setValue(finalArtwork)
                .addOnSuccessListener {
                    Log.d("ArtworkRepo", "Artwork saved successfully: ${finalArtwork.id}")
                    continuation.resume(finalArtwork)
                }
                .addOnFailureListener { e ->
                    Log.e("ArtworkRepo", "Error saving artwork: ${e.message}")
                    continuation.resumeWithException(e)
                }
        }
    }

    override suspend fun deleteArtwork(artworkId: String) {
        try {
            databaseRef.child(artworkId).removeValue().await()
            Log.d("ArtworkRepository", "Artwork deleted from Firebase: $artworkId")
            _allArtworksFlow.value = _allArtworksFlow.value.filter { it.id != artworkId }
        } catch (e: Exception) {
            Log.e("ArtworkRepository", "Error deleting artwork $artworkId: ${e.message}")
            throw e
        }
    }

    override fun updateArtworkInFlow(artwork: Artwork) {
        _allArtworksFlow.value = _allArtworksFlow.value.map {
            if (it.id == artwork.id) artwork else it
        }
        Log.d("ArtworkRepository", "Locally updated artwork ${artwork.id} in _allArtworksFlow.")
    }

    override fun getSimilarArtworks(
        artStyle: String,
        excludeArtworkId: String?
    ): Flow<List<Artwork>> {
        return combine(_allArtworksFlow, _currentUserId) { allArtworks, userIdFromFlow ->
            if (userIdFromFlow == null) {
                Log.d(
                    "ArtworkRepository",
                    "getSimilarArtworks: No user logged in. Returning empty list."
                )
                emptyList()
            } else {
                val similarArtworks = allArtworks.filter { artwork ->
                    artwork.userId == userIdFromFlow &&
                            artwork.artStyle == artStyle &&
                            artwork.id != excludeArtworkId
                }
                Log.d(
                    "ArtworkRepository",
                    "getSimilarArtworks: Filtered ${allArtworks.size} artworks to ${similarArtworks.size} for user $userIdFromFlow, style '$artStyle' (excluding $excludeArtworkId)."
                )
                similarArtworks
            }
        }
    }
}