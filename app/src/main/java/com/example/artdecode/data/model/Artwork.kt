package com.example.artdecode.data.model

data class Artwork(
    // Firebase uses String for IDs
    val id: String? = null,
    val imageUri: String? = null,
    val artStyle: String? = null,
    val confidenceScore: Float? = null,
    val userId: String? = null,
    val capturedAt: Long? = null,
    val userEmail: String? = null,
    val username: String? = null
) {
    constructor() : this(null, null, null, null, null, null, null, null)
}