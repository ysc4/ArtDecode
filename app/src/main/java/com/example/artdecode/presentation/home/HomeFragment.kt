package com.example.artdecode.presentation.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.artdecode.R
import com.example.artdecode.data.model.RecyclerViewItem
import com.example.artdecode.data.repository.ArtworkRepositoryImpl
import com.example.artdecode.presentation.adapter.ArtworkAdapter
import com.example.artdecode.presentation.artworkinfo.ArtworkInfoActivity
import com.example.artdecode.presentation.login.LoginActivity
import com.example.artdecode.utils.GridSpacingItemDecoration
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArtworkAdapter

    // User information
    private var userEmail: String? = null
    private var userUsername: String? = null
    private var userUid: String? = null

    companion object {
        private const val ARG_USER_EMAIL = "user_email"
        private const val ARG_USER_USERNAME = "user_username"
        private const val ARG_USER_UID = "user_uid"

        @JvmStatic
        fun newInstance(userEmail: String?, userUsername: String?, userUid: String?) = HomeFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_USER_EMAIL, userEmail)
                putString(ARG_USER_USERNAME, userUsername)
                putString(ARG_USER_UID, userUid)
            }
        }

        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract user information from arguments
        arguments?.let {
            userEmail = it.getString(ARG_USER_EMAIL)
            userUsername = it.getString(ARG_USER_USERNAME)
            userUid = it.getString(ARG_USER_UID)
        }

        Log.d("HomeFragment", "User info - UID: $userUid, Email: $userEmail, Username: $userUsername")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        setupViewModel()
        setupRecyclerView(view)
        observeViewModel()

        return view
    }

    private fun setupViewModel() {
        val repository = ArtworkRepositoryImpl(requireContext())
        val factory = HomeViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        // Set the current user ID in the ViewModel to filter artworks
        userUid?.let { uid ->
            viewModel.setCurrentUserId(uid)
        }
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.collectionRecyclerView)

        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == 0) 2 else 1
            }
        }
        recyclerView.layoutManager = gridLayoutManager

        val spacing = (18 * resources.displayMetrics.density).toInt()
        recyclerView.addItemDecoration(GridSpacingItemDecoration(2, spacing, true))

        adapter = ArtworkAdapter(
            onItemClick = { artworkId: String? ->
                viewModel.onArtworkClick(artworkId)
            },
            onFavoriteClick = { artworkId: String? ->
                viewModel.onFavoriteClick(artworkId)
            }
        )
        recyclerView.adapter = adapter

        // Add swipe-to-delete functionality
        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = SwipeToDeleteCallback { position ->
            handleSwipeDelete(position)
        }
        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun handleSwipeDelete(position: Int) {
        val currentItems = viewModel.uiState.value.items
        if (position < currentItems.size) {
            val item = currentItems[position]

            // Only delete if it's an artwork item (not header)
            if (item is RecyclerViewItem.ArtworkItem) {
                val artworkToDelete = item.artwork
                val artworkId = artworkToDelete.id

                // Show confirmation with Snackbar and undo option
                val snackbar = Snackbar.make(
                    recyclerView,
                    "Artwork deleted",
                    Snackbar.LENGTH_LONG
                ).setAction("UNDO") {
                    // Restore the artwork if user clicks undo
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            viewModel.artworkRepository.saveArtwork(artworkToDelete)
                            Log.d("HomeFragment", "Artwork restored successfully")
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error restoring artwork: ${e.message}")
                            Snackbar.make(recyclerView, "Failed to restore artwork", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }

                // Delete the artwork
                viewModel.deleteArtwork(artworkId)

                snackbar.show()
            } else {
                // If somehow a header was swiped, refresh the adapter
                adapter.notifyItemChanged(position)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                adapter.submitList(uiState.items)

                uiState.navigateToArtworkDetail?.let { artworkId ->
                    val intent = Intent(requireContext(), ArtworkInfoActivity::class.java).apply {
                        putExtra("ARTWORK_ID", artworkId)
                        // Pass user information to ArtworkInfoActivity as well
                        putExtra(LoginActivity.EXTRA_USER_EMAIL, userEmail)
                        putExtra(LoginActivity.EXTRA_USER_USERNAME, userUsername)
                        putExtra(LoginActivity.EXTRA_USER_UID, userUid)
                    }
                    startActivity(intent)
                    viewModel.onNavigationHandled()
                }
            }
        }
    }

    // Inner class for swipe-to-delete callback
    private inner class SwipeToDeleteCallback(
        private val onSwipeDelete: (position: Int) -> Unit
    ) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            onSwipeDelete(position)
        }

        override fun getSwipeDirs(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            // Don't allow swiping on header items
            return if (viewHolder.itemViewType == ArtworkAdapter.VIEW_TYPE_HEADER) {
                0 // No swipe for headers
            } else {
                super.getSwipeDirs(recyclerView, viewHolder)
            }
        }
    }
}