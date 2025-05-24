package com.example.artdecode

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ArtworkAdapter(
    private val onItemClick: (RecyclerViewItem) -> Unit
) : ListAdapter<RecyclerViewItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ARTWORK = 1
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
                    .inflate(R.layout.header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_ARTWORK -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_collection, parent, false)
                ArtworkViewHolder(view, onItemClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is RecyclerViewItem.Header -> (holder as HeaderViewHolder).bind(item)
            is RecyclerViewItem.ArtworkItem -> (holder as ArtworkViewHolder).bind(item)
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
        private val onItemClick: (RecyclerViewItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(artworkItem: RecyclerViewItem.ArtworkItem) {
            val artwork = artworkItem.artwork

            val artStyleTextView = itemView.findViewById<TextView>(R.id.artStyle)
            val confidenceTextView = itemView.findViewById<TextView>(R.id.confidenceScore)
            val favoriteIcon = itemView.findViewById<ImageView>(R.id.favoriteButton)
            val artworkImage = itemView.findViewById<ImageView>(R.id.artworkImage)

            artStyleTextView?.text = artwork.style
            confidenceTextView?.text = "${artwork.confidence}% Confidence"

            // Set the favorite icon based on state
            favoriteIcon?.setImageResource(
                if (artwork.isFavorite) R.drawable.active_heart
                else R.drawable.inactive_heart
            )

            // Load artwork image if needed
            // Glide.with(itemView.context).load(artwork.imageUrl).into(artworkImage)

            // Favorite button toggle logic
            favoriteIcon?.setOnClickListener {
                val newFavoriteState = !artwork.isFavorite
                artwork.isFavorite = newFavoriteState

                // Update the icon
                favoriteIcon.setImageResource(
                    if (newFavoriteState) R.drawable.active_heart
                    else R.drawable.inactive_heart
                )

                // Persist change using SharedPreferences
                val sharedPref = itemView.context.getSharedPreferences("artwork_preferences", android.content.Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putBoolean("favorite_${artwork.id}", newFavoriteState)
                    apply()
                }

                // Optional: Notify adapter if you want to refresh the whole list
                // notifyItemChanged(adapterPosition) // <-- if needed
            }

            itemView.setOnClickListener { onItemClick(artworkItem) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecyclerViewItem>() {
        override fun areItemsTheSame(oldItem: RecyclerViewItem, newItem: RecyclerViewItem): Boolean {
            return when {
                oldItem is RecyclerViewItem.Header && newItem is RecyclerViewItem.Header ->
                    oldItem.title == newItem.title
                oldItem is RecyclerViewItem.ArtworkItem && newItem is RecyclerViewItem.ArtworkItem ->
                    oldItem.artwork.id == newItem.artwork.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: RecyclerViewItem, newItem: RecyclerViewItem): Boolean {
            return oldItem == newItem
        }
    }
}