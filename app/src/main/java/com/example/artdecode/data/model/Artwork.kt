package com.example.artdecode.data.model

import android.net.Uri

data class Artwork(
    // Firebase uses String for IDs
    val id: String? = null,
    val imageUri: String? = null, // Store as String URI
    val artStyle: String? = null,
    val confidenceScore: Float? = null,
    val isFavorite: Boolean = false // Default to false
) {
    // No-argument constructor needed for Firebase Realtime Database automatic data mapping
    constructor() : this(null, null, null, null, false)
}