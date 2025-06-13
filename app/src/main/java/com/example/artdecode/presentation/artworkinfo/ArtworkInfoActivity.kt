package com.example.artdecode.presentation.artworkinfo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.artdecode.R
import com.example.artdecode.data.repository.ArtworkRepositoryImpl
import com.example.artdecode.presentation.main.MainActivity
import com.example.artdecode.presentation.scan.ScanActivity
import com.example.artdecode.presentation.report.ReportActivity
import kotlinx.coroutines.launch

class ArtworkInfoActivity : AppCompatActivity() {

    private lateinit var viewModel: ArtworkInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artwork_info)

        setupViewModel()

        val imageUri = getImageUriFromIntent()
        viewModel.loadArtworkInfo(imageUri)

        setupViews()
        observeViewModel()
    }

    private fun setupViewModel() {
        // Manual dependency injection
        val repository = ArtworkRepositoryImpl(this)
        val factory = ArtworkInfoViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ArtworkInfoViewModel::class.java]
    }

    private fun getImageUriFromIntent(): Uri? {
        val capturedImageUriString = intent.getStringExtra("CAPTURED_IMAGE_URI")
        val selectedImageUriString = intent.getStringExtra("SELECTED_IMAGE_URI")

        return when {
            capturedImageUriString != null -> Uri.parse(capturedImageUriString)
            selectedImageUriString != null -> Uri.parse(selectedImageUriString)
            else -> null
        }
    }

    private fun setupViews() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            viewModel.onBackClick()
        }

        findViewById<Button>(R.id.scanMoreButton).setOnClickListener {
            viewModel.onScanMoreClick()
        }

        findViewById<ImageButton>(R.id.reportButton).setOnClickListener {
            viewModel.onReportClick()
        }

        findViewById<ImageButton>(R.id.favoriteButton).setOnClickListener {
            viewModel.toggleFavorite()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                updateUI(uiState)
                handleNavigation(uiState)
            }
        }
    }

    private fun updateUI(uiState: ArtworkInfoUiState) {
        // Update image
        uiState.imageUri?.let { uri ->
            findViewById<ImageView>(R.id.artworkImage).setImageURI(uri)
            Toast.makeText(this, "Image loaded: $uri", Toast.LENGTH_LONG).show()
        } ?: run {
            Toast.makeText(this, "No image URI found in Intent.", Toast.LENGTH_LONG).show()
        }

        // Update favorite button
        val favoriteButton = findViewById<ImageButton>(R.id.favoriteButton)
        favoriteButton.setImageResource(
            if (uiState.isFavorite) R.drawable.active_heart
            else R.drawable.inactive_heart
        )

        // Update similar artworks
        setupSimilarArtworks(uiState.similarArtworks)
    }

    private fun setupSimilarArtworks(similarArtworks: List<String>) {
        val container = findViewById<LinearLayout>(R.id.similarArtworksContainer)
        container.removeAllViews()

        for (artwork in similarArtworks) {
            val artworkCard: View = layoutInflater.inflate(R.layout.item_artwork, container, false)
            val artworkImage = artworkCard.findViewById<ImageView>(R.id.similarArtworkImage)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 20, 0)
            artworkCard.layoutParams = params

            artworkCard.setOnClickListener {
                viewModel.onSimilarArtworkClick(artwork)
            }

            container.addView(artworkCard)
        }
    }

    private fun handleNavigation(uiState: ArtworkInfoUiState) {
        when {
            uiState.navigateBack -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                viewModel.onNavigationHandled()
            }
            uiState.navigateToScan -> {
                startActivity(Intent(this, ScanActivity::class.java))
                finish()
                viewModel.onNavigationHandled()
            }
            uiState.navigateToReport -> {
                startActivity(Intent(this, ReportActivity::class.java))
                finish()
                viewModel.onNavigationHandled()
            }
            uiState.navigateToSimilarArtwork != null -> {
                val intent = Intent(this, ArtworkInfoActivity::class.java)
                startActivity(intent)
                viewModel.onNavigationHandled()
            }
        }
    }
}