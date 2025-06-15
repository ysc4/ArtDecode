package com.example.artdecode.presentation.artworkinfo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.artdecode.R
import com.example.artdecode.data.model.Artwork
import com.example.artdecode.data.repository.ArtworkRepositoryImpl
import com.example.artdecode.presentation.main.MainActivity
import com.example.artdecode.presentation.scan.ScanActivity
import com.example.artdecode.presentation.report.ReportActivity
import com.example.artdecode.presentation.login.LoginActivity
import com.example.artdecode.utils.ArtStyleDescriptionProvider

import kotlinx.coroutines.launch

class ArtworkInfoActivity : AppCompatActivity() {

    private lateinit var viewModel: ArtworkInfoViewModel

    // UI elements
    private lateinit var artworkImageView: ImageView
    private lateinit var favoriteButton: ImageButton
    private lateinit var artworkStyleTextView: TextView
    private lateinit var confidenceScoreTextView: TextView
    private lateinit var similarArtworksContainer: LinearLayout
    private lateinit var scanMoreButton: Button
    private lateinit var reportButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var styleDescriptionTextView: TextView
    private lateinit var artStyleImageView: ImageView

    // User information
    private var userEmail: String? = null
    private var userUsername: String? = null
    private var userUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artwork_info)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        // Extract user information from intent
        extractUserInfoFromIntent()

        setupViewModel()
        initializeViews()
        setupClickListeners()

        val artworkId = intent.getStringExtra("ARTWORK_ID")
        val capturedImageUri = intent.getStringExtra("CAPTURED_IMAGE_URI")
        val artStyle = intent.getStringExtra("ART_STYLE")
        val confidenceScore = intent.getFloatExtra("CONFIDENCE_SCORE", -1f).takeIf { it != -1f }

        viewModel.loadArtworkInfo(
            artworkId = artworkId,
            capturedImageUri = capturedImageUri,
            artStyle = artStyle,
            confidenceScore = confidenceScore
        )

        observeViewModel()
    }

    private fun extractUserInfoFromIntent() {
        userEmail = intent.getStringExtra(LoginActivity.EXTRA_USER_EMAIL)
        userUsername = intent.getStringExtra(LoginActivity.EXTRA_USER_USERNAME)
        userUid = intent.getStringExtra(LoginActivity.EXTRA_USER_UID)
    }

    private fun setupViewModel() {
        val repository = ArtworkRepositoryImpl(this)
        val factory = ArtworkInfoViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ArtworkInfoViewModel::class.java]

        // Set the current user ID in the ViewModel to filter similar artworks
        userUid?.let { uid ->
            viewModel.setCurrentUserId(uid)
        }
    }

    private fun initializeViews() {
        artworkImageView = findViewById<ImageView>(R.id.artworkImage)
        favoriteButton = findViewById<ImageButton>(R.id.favoritedButton)
        artworkStyleTextView = findViewById<TextView>(R.id.artworkStyle)
        confidenceScoreTextView = findViewById<TextView>(R.id.confScore)
        similarArtworksContainer = findViewById<LinearLayout>(R.id.similarArtworksContainer)
        scanMoreButton = findViewById<Button>(R.id.scanMoreButton)
        reportButton = findViewById<ImageButton>(R.id.reportButton)
        backButton = findViewById<ImageButton>(R.id.backButton)
        styleDescriptionTextView = findViewById<TextView>(R.id.styleDescription)
        artStyleImageView = findViewById<ImageView>(R.id.artStyleImage)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            viewModel.onBackClick()
        }
        favoriteButton.setOnClickListener {
            viewModel.toggleFavorite()
        }
        scanMoreButton.setOnClickListener {
            viewModel.onScanMoreClick()
        }
        reportButton.setOnClickListener {
            viewModel.onReportClick()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                updateUI(uiState)
                handleNavigation(uiState)
                uiState.errorMessage?.let { msg ->
                    Toast.makeText(this@ArtworkInfoActivity, msg, Toast.LENGTH_LONG).show()
                    // Clear the error message after showing
                    viewModel.onNavigationHandled()
                }
            }
        }
    }

    private fun updateUI(uiState: ArtworkInfoUiState) {
        uiState.artwork?.let { artwork ->
            // Load the captured artwork image
            artwork.imageUri?.let { uriString ->
                Glide.with(this)
                    .load(Uri.parse(uriString))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(artworkImageView)
            } ?: run {
                artworkImageView.setImageResource(R.drawable.placeholder_image)
            }

            // Set the predicted artwork style name and confidence
            artworkStyleTextView.text = artwork.artStyle ?: "N/A"
            confidenceScoreTextView.text = artwork.confidenceScore?.let {
                String.format("%.2f%%", it * 100)
            } ?: "N/A Confidence"

            // Set the art style description
            styleDescriptionTextView.text =
                ArtStyleDescriptionProvider.getStyleDescription(artwork.artStyle)

            // Set the art style image
            val styleImageResId = ArtStyleDescriptionProvider.getStyleImageResId(artwork.artStyle)
            artStyleImageView.setImageResource(styleImageResId)

            // Set favorite button state
            favoriteButton.setImageResource(if (artwork.isFavorite) R.drawable.active_heart else R.drawable.inactive_heart)

        } ?: run {
            // Handle case where artwork data is null
            artworkImageView.setImageResource(R.drawable.placeholder_image)
            artworkStyleTextView.text = "N/A"
            confidenceScoreTextView.text = "N/A"
            styleDescriptionTextView.text = "Artwork details not available."
            artStyleImageView.setImageResource(R.drawable.default_art_style_image)
            favoriteButton.setImageResource(R.drawable.inactive_heart)
        }

        // Similar artworks - now filtered by user
        similarArtworksContainer.removeAllViews()
        val inflater = layoutInflater

        if (uiState.similarArtworks.isEmpty()) {
            val noSimilarText = TextView(this).apply {
                text = "No similar artworks found."
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(20, 0, 20, 0)
            }
            similarArtworksContainer.addView(noSimilarText)
            return
        }

        uiState.similarArtworks.forEach { similarArtwork ->
            val artworkCard = inflater.inflate(R.layout.item_artwork, similarArtworksContainer, false)

            val artworkImage = artworkCard.findViewById<ImageView>(R.id.similarArtworkImage)

            similarArtwork.imageUri?.let { uriString ->
                Glide.with(this)
                    .load(Uri.parse(uriString))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(artworkImage)
            } ?: run {
                artworkImage.setImageResource(R.drawable.placeholder_image)
            }

            val params = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.similar_artwork_card),
                resources.getDimensionPixelSize(R.dimen.similar_artwork_card)
            )
            params.setMargins(0, 0, 20, 0)
            artworkCard.layoutParams = params

            artworkCard.setOnClickListener {
                viewModel.onSimilarArtworkClick(similarArtwork.id)
            }

            similarArtworksContainer.addView(artworkCard)
        }
    }

    private fun handleNavigation(uiState: ArtworkInfoUiState) {
        when {
            uiState.navigateBack -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra(LoginActivity.EXTRA_USER_EMAIL, userEmail)
                    putExtra(LoginActivity.EXTRA_USER_USERNAME, userUsername)
                    putExtra(LoginActivity.EXTRA_USER_UID, userUid)
                }
                startActivity(intent)
                finish()
                viewModel.onNavigationHandled()
            }
            uiState.navigateToScan -> {
                val intent = Intent(this, ScanActivity::class.java).apply {
                    putExtra(ScanActivity.EXTRA_USER_EMAIL, userEmail)
                    putExtra(ScanActivity.EXTRA_USER_USERNAME, userUsername)
                    putExtra(ScanActivity.EXTRA_USER_UID, userUid)
                }
                startActivity(intent)
                finish()
                viewModel.onNavigationHandled()
            }
            uiState.navigateToReport -> {
                val intent = Intent(this, ReportActivity::class.java).apply {
                    val currentArtwork = uiState.artwork

                    currentArtwork?.let {
                        if (it.id != null) {
                            putExtra("ARTWORK_ID", it.id)
                        } else {
                            putExtra("CAPTURED_IMAGE_URI", it.imageUri)
                            putExtra("ART_STYLE", it.artStyle)
                            it.confidenceScore?.let { score ->
                                putExtra("CONFIDENCE_SCORE", score)
                            }
                        }
                    }

                    // Pass user information
                    putExtra(LoginActivity.EXTRA_USER_EMAIL, userEmail)
                    putExtra(LoginActivity.EXTRA_USER_USERNAME, userUsername)
                    putExtra(LoginActivity.EXTRA_USER_UID, userUid)
                }
                startActivity(intent)
                viewModel.onNavigationHandled()
            }
            uiState.navigateToSimilarArtwork != null -> {
                val intent = Intent(this, ArtworkInfoActivity::class.java).apply {
                    putExtra("ARTWORK_ID", uiState.navigateToSimilarArtwork)
                    // Pass user information to the next ArtworkInfoActivity
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