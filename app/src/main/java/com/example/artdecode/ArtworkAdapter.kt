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

            // Bind artwork data to views
            // Update these IDs to match your actual item_collection.xml layout
            itemView.findViewById<TextView>(R.id.artStyle)?.text = artwork.style
            itemView.findViewById<TextView>(R.id.confidenceScore)?.text = "${artwork.confidence}% Confidence"

            // Handle favorite icon if you have one
            val favoriteIcon = itemView.findViewById<ImageView>(R.id.favoriteButton)
            favoriteIcon?.setImageResource(
                if (artwork.isFavorite) R.drawable.active_heart
                else R.drawable.inactive_heart
            )

            // Load artwork image if you have an ImageView
            val artworkImage = itemView.findViewById<ImageView>(R.id.artworkImage)
            // You'll need to implement image loading here, for example with Glide:
            // Glide.with(itemView.context).load(artwork.imageUrl).into(artworkImage)

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