package com.example.artdecode.presentation.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.artdecode.R
import com.example.artdecode.data.model.Artwork // Import the combined Artwork data class
import com.example.artdecode.data.model.RecyclerViewItem

class ArtworkAdapter(
    private val onItemClick: (String?) -> Unit,
    private val onFavoriteClick: (String?) -> Unit
) : ListAdapter<RecyclerViewItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ARTWORK = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is RecyclerViewItem.Header -> VIEW_TYPE_HEADER
            is RecyclerViewItem.ArtworkItem -> VIEW_TYPE_ARTWORK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.header, parent, false) // Assuming this is your header layout
                HeaderViewHolder(view)
            }
            VIEW_TYPE_ARTWORK -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_collection, parent, false) // Your artwork card layout
                ArtworkViewHolder(view, onItemClick, onFavoriteClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is RecyclerViewItem.Header -> (holder as HeaderViewHolder).bind(item)
            is RecyclerViewItem.ArtworkItem -> (holder as ArtworkViewHolder).bind(item.artwork)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.headerTitle)

        fun bind(header: RecyclerViewItem.Header) {
            titleText.text = header.title
        }
    }

    class ArtworkViewHolder(
        itemView: View,
        // Ensure callbacks use String? for IDs
        private val onItemClick: (String?) -> Unit,
        private val onFavoriteClick: (String?) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val artworkImage: ImageView = itemView.findViewById(R.id.artworkImage)
        // Corrected TextView IDs to match item_collection.xml
        private val artStyleTextView: TextView = itemView.findViewById(R.id.artStyle)
        private val confidenceTextView: TextView = itemView.findViewById(R.id.confidenceScore)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favoritedButton)

        fun bind(artwork: Artwork) { // Bind with the combined Artwork object
            // Load image using Glide, handling Uri String
            artwork.imageUri?.let { uriString ->
                Glide.with(itemView.context)
                    .load(Uri.parse(uriString)) // Parse the string back to Uri for Glide
                    .placeholder(R.drawable.placeholder_image) // Ensure you have this drawable
                    .error(R.drawable.placeholder_image) // Ensure you have this drawable
                    .into(artworkImage)
            } ?: run {
                artworkImage.setImageResource(R.drawable.placeholder_image) // Fallback for null URI
            }

            // Set artwork details using the unified Artwork properties
            artStyleTextView.text = artwork.artStyle ?: "Unknown Style"
            confidenceTextView.text = artwork.confidenceScore?.let {
                String.format("%.2f%% Confidence", it * 100) // Format to percentage
            } ?: "N/A Confidence"

            // Set the favorite icon based on state
            favoriteIcon.setImageResource(
                if (artwork.isFavorite) R.drawable.active_heart // Assuming these drawables exist
                else R.drawable.inactive_heart
            )

            // Set click listeners - now passing String? ID
            favoriteIcon.setOnClickListener {
                onFavoriteClick(artwork.id)
            }

            itemView.setOnClickListener {
                onItemClick(artwork.id)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecyclerViewItem>() {
        override fun areItemsTheSame(oldItem: RecyclerViewItem, newItem: RecyclerViewItem): Boolean {
            return when {
                oldItem is RecyclerViewItem.Header && newItem is RecyclerViewItem.Header ->
                    oldItem.title == newItem.title
                oldItem is RecyclerViewItem.ArtworkItem && newItem is RecyclerViewItem.ArtworkItem ->
                    // Compare String? IDs
                    oldItem.artwork.id == newItem.artwork.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: RecyclerViewItem, newItem: RecyclerViewItem): Boolean {
            // This checks for deep equality of data classes, which is usually sufficient
            return oldItem == newItem
        }
    }
}