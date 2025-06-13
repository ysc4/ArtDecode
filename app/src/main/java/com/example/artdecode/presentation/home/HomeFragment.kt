package com.example.artdecode.presentation.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.artdecode.R
import com.example.artdecode.data.repository.ArtworkRepositoryImpl
import com.example.artdecode.presentation.adapter.ArtworkAdapter
import com.example.artdecode.presentation.artworkinfo.ArtworkInfoActivity
import com.example.artdecode.utils.GridSpacingItemDecoration
//import com.example.artdecode.utils.GridSpacingItemDecoration
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArtworkAdapter

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
        // Manual dependency injection
        val repository = ArtworkRepositoryImpl(requireContext())
        val factory = HomeViewModelFactory(repository)
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

        val spacing = (18 * resources.displayMetrics.density).toInt()
        recyclerView.addItemDecoration(GridSpacingItemDecoration(2, spacing, true))

        adapter = ArtworkAdapter(
            onItemClick = { artworkId ->
                viewModel.onArtworkClick(artworkId)
            },
            onFavoriteClick = { artworkId ->
                viewModel.onFavoriteClick(artworkId)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                adapter.submitList(uiState.items)

                uiState.navigateToArtworkDetail?.let { artworkId ->
                    val intent = Intent(requireContext(), ArtworkInfoActivity::class.java)
                    intent.putExtra("ARTWORK_ID", artworkId)
                    startActivity(intent)
                    viewModel.onNavigationHandled()
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}

