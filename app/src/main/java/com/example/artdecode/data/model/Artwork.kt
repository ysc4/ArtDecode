package com.example.artdecode.data.model

data class Artwork(
    // Firebase uses String for IDs
    val id: String? = null,
    val imageUri: String? = null, // Store as String URI
    val artStyle: String? = null,
    val confidenceScore: Float? = null,
    val isFavorite: Boolean = false, // Default to false
    val userId: String? = null, // ID of the user who captured this artwork
    val capturedAt: Long? = null, // Timestamp when the artwork was captured
    val userEmail: String? = null, // Optional: store user email for display purposes
    val username: String? = null // Optional: store username for display purposes
) {
    // No-argument constructor needed for Firebase Realtime Database automatic data mapping
    constructor() : this(null, null, null, null, false, null, null, null, null)
}