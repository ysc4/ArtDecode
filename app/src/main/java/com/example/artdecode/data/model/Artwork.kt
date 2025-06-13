package com.example.artdecode.data.model

data class Artwork(
    val id: Int,
    val imageUrl: String,
    val style: String,
    val confidence: Float,
    var isFavorite: Boolean = false
)