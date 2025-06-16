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
import com.example.artdecode.data.model.Artwork
import com.example.artdecode.data.model.RecyclerViewItem

// Assuming this is actually your HomeAdapter
// You might want to rename this file and class to HomeAdapter for consistency
class ArtworkAdapter(
    private val onItemClick: (String?) -> Unit,
    // REMOVED: onDeleteClick lambda from constructor
    // private val onDeleteClick: (String?) -> Unit
) : ListAdapter<RecyclerViewItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ARTWORK = 1
        const val VIEW_TYPE_MESSAGE = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is RecyclerViewItem.Header -> VIEW_TYPE_HEADER
            is RecyclerViewItem.ArtworkItem -> VIEW_TYPE_ARTWORK
            is RecyclerViewItem.Message -> VIEW_TYPE_MESSAGE
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
                // REMOVED: onDeleteClick from ArtworkViewHolder constructor
                ArtworkViewHolder(view, onItemClick)
            }
            VIEW_TYPE_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message, parent, false)
                MessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is RecyclerViewItem.Header -> (holder as HeaderViewHolder).bind(item)
            is RecyclerViewItem.ArtworkItem -> (holder as ArtworkViewHolder).bind(item.artwork)
            is RecyclerViewItem.Message -> (holder as MessageViewHolder).bind(item)
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
        private val onItemClick: (String?) -> Unit,
        // REMOVED: onDeleteClick from ArtworkViewHolder constructor
        // private val onDeleteClick: (String?) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val artworkImage: ImageView = itemView.findViewById(R.id.artworkImage)
        private val artStyleTextView: TextView = itemView.findViewById(R.id.artStyle)
        private val confidenceTextView: TextView = itemView.findViewById(R.id.confidenceScore)
        // REMOVED: deleteButton ImageView reference
        // private val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)

        fun bind(artwork: Artwork) {
            artwork.imageUri?.let { uriString ->
                Glide.with(itemView.context)
                    .load(Uri.parse(uriString))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(artworkImage)
            } ?: run {
                artworkImage.setImageResource(R.drawable.placeholder_image)
            }

            artStyleTextView.text = artwork.artStyle ?: "Unknown Style"
            confidenceTextView.text = artwork.confidenceScore?.let {
                String.format("%.2f%%", it * 100)
            } ?: "N/A"

            itemView.setOnClickListener {
                onItemClick(artwork.id)
            }
        }
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)

        fun bind(message: RecyclerViewItem.Message) {
            messageText.text = message.text
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecyclerViewItem>() {
        override fun areItemsTheSame(oldItem: RecyclerViewItem, newItem: RecyclerViewItem): Boolean {
            return when {
                oldItem is RecyclerViewItem.Header && newItem is RecyclerViewItem.Header ->
                    oldItem.title == newItem.title
                oldItem is RecyclerViewItem.ArtworkItem && newItem is RecyclerViewItem.ArtworkItem ->
                    oldItem.artwork.id == newItem.artwork.id
                oldItem is RecyclerViewItem.Message && newItem is RecyclerViewItem.Message ->
                    oldItem.text == newItem.text
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: RecyclerViewItem, newItem: RecyclerViewItem): Boolean {
            return oldItem == newItem
        }
    }
}