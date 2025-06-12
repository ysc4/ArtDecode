package com.example.artdecode

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A simple [Fragment] subclass.
 * Use the [Home.newInstance] factory method to
 * create an instance of this fragment.
 */
class Home : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArtworkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        setupRecyclerView(view)
        loadArtworks()
        return view
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.collectionRecyclerView)

        // Grid with 2 columns, but header spans full width
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == 0) 2 else 1 // Header takes full width, artworks take 1 column each
            }
        }
        recyclerView.layoutManager = gridLayoutManager

        // Add spacing between items (16dp converted to pixels)
        val spacing = (18 * resources.displayMetrics.density).toInt()
        recyclerView.addItemDecoration(GridSpacingItemDecoration(2, spacing, true))

        adapter = ArtworkAdapter { item ->
            when (item) {
                is RecyclerViewItem.ArtworkItem -> {
                    // Handle artwork click
                    startActivity(Intent(requireContext(), ArtworkInfo::class.java))
                }

                is RecyclerViewItem.Header -> {
                    // Handle header click if needed
                }
            }
        }
        recyclerView.adapter = adapter
    }

    private fun loadArtworks() {
        // Create list with header and artworks
        val items = mutableListOf<RecyclerViewItem>()

        // Add header
        items.add(RecyclerViewItem.Header("My Collections"))

        // Add artworks
        val sharedPref =
            requireContext().getSharedPreferences("artwork_preferences", Context.MODE_PRIVATE)

        val artworks = listOf(
            Artwork(
                1,
                "artwork1.jpg",
                "Impressionism",
                89.91f,
                sharedPref.getBoolean("favorite_1", false)
            ),
            Artwork(
                2,
                "artwork2.jpg",
                "Expressionism",
                74.55f,
                sharedPref.getBoolean("favorite_2", false)
            ),
            Artwork(
                3,
                "artwork3.jpg",
                "Baroque",
                98.04f,
                sharedPref.getBoolean("favorite_3", false)
            ),
            Artwork(
                4,
                "artwork4.jpg",
                "Abstract",
                90.11f,
                sharedPref.getBoolean("favorite_4", false)
            ),
            Artwork(
                5,
                "artwork5.jpg",
                "Abstract",
                93.12f,
                sharedPref.getBoolean("favorite_5", false)
            )
        )

        artworks.forEach { artwork ->
            items.add(RecyclerViewItem.ArtworkItem(artwork))
        }

        adapter.submitList(items)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment.
         *
         * @return A new instance of fragment Home.
         */
        @JvmStatic
        fun newInstance() = Home()
    }
}

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val layoutManager = parent.layoutManager as? GridLayoutManager
        val spanSize = layoutManager?.spanSizeLookup?.getSpanSize(position) ?: 1

        // If item spans full width (like header), reduce bottom spacing
        if (spanSize == spanCount) {
            outRect.left = 0
            outRect.right = 0
            if (position == 0) {
                outRect.top = 0
            }
            // Reduce spacing after header to half the normal spacing
            outRect.bottom = spacing / 2
            return
        }

        // For regular grid items, calculate column position considering header
        val gridPosition = position - 1 // Subtract 1 to account for header
        val column = gridPosition % spanCount

        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            // Reduce top spacing for first row of artworks (positions 1 and 2)
            if (gridPosition < spanCount) {
                outRect.top = spacing / 2 // Half the normal spacing
            } else {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (gridPosition >= spanCount) {
                outRect.top = spacing
            }
        }
    }
}

// Data classes for RecyclerView items
sealed class RecyclerViewItem {
    data class Header(val title: String) : RecyclerViewItem()
    data class ArtworkItem(val artwork: Artwork) : RecyclerViewItem()
}

// Data class for artwork
data class Artwork(
    val id: Int,
    val imageUrl: String,
    val style: String,
    val confidence: Float,
    var isFavorite: Boolean
)