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
import com.example.artdecode.ArtDecode
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
        val application = requireActivity().application as ArtDecode
        val repository = application.artworkRepository // Get the singleton instance

        userUid?.let { uid ->
            repository.setCurrentUserId(uid)
            Log.d("HomeFragment", "Set UID $uid on singleton ArtworkRepository from HomeFragment.")
        } ?: run {
            Log.e("HomeFragment", "userUid is NULL in HomeFragment setupViewModel! Cannot set repository UID.")
        }

        val factory = HomeViewModelFactory(requireContext().applicationContext)
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
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
        recyclerView.itemAnimator = null
        val spacing = (18 * resources.displayMetrics.density).toInt()
        recyclerView.addItemDecoration(GridSpacingItemDecoration(2, spacing, true))

        adapter = ArtworkAdapter(
            onItemClick = { artworkId: String? ->
                viewModel.onArtworkClick(artworkId)
            }
        )
        recyclerView.adapter = adapter

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

            if (item is RecyclerViewItem.ArtworkItem) {
                val artworkToDelete = item.artwork
                val artworkId = artworkToDelete.id

                val snackbar = Snackbar.make(
                    recyclerView,
                    "Artwork deleted",
                    Snackbar.LENGTH_LONG
                ).setAction("UNDO") {
                    viewModel.restoreArtwork(artworkToDelete)
                }

                viewModel.deleteArtwork(artworkId)

                snackbar.show()
            } else {
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
            return if (viewHolder.itemViewType == ArtworkAdapter.VIEW_TYPE_HEADER) {
                0
            } else {
                super.getSwipeDirs(recyclerView, viewHolder)
            }
        }
    }
}