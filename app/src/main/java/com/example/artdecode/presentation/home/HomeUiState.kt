package com.example.artdecode.presentation.home

import com.example.artdecode.data.model.RecyclerViewItem

data class HomeUiState(
    val items: List<RecyclerViewItem> = emptyList(),
    val isLoading: Boolean = false,
    val navigateToArtworkDetail: String? = null
)