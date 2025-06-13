package com.example.artdecode.data.model

sealed class RecyclerViewItem {
    data class Header(val title: String) : RecyclerViewItem()
    data class ArtworkItem(val artwork: Artwork) : RecyclerViewItem()
}