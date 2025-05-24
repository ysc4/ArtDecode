package com.example.artdecode

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat

class ArtworkInfo : AppCompatActivity() {

    // Sample data for similar artworks (you can replace this with your actual data)
    private val similarArtworks = listOf(
        "artwork1", "artwork2", "artwork3", "artwork4" // Replace with your actual artwork data
    )

    // Track favorite state
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_artwork_info)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupSimilarArtworks()
        setupClickListeners()
        setupFavoriteButton()
    }

    private fun setupFavoriteButton() {
        val favoriteButton = findViewById<ImageButton>(R.id.favoriteButton)

        // Set initial state (you can load this from SharedPreferences or database)
        updateFavoriteButtonState()

        favoriteButton.setOnClickListener {
            toggleFavoriteState()
        }
    }

    private fun toggleFavoriteState() {
        isFavorite = !isFavorite
        updateFavoriteButtonState()

        // Here you can save the favorite state to SharedPreferences or database
        saveFavoriteState(isFavorite)
    }

    private fun updateFavoriteButtonState() {
        val favoriteButton = findViewById<ImageButton>(R.id.favoriteButton)

        if (isFavorite) {
            // Set filled red heart
            favoriteButton.setImageResource(R.drawable.active_heart) // This should be your filled red heart
        } else {
            // Set outlined heart
            favoriteButton.setImageResource(R.drawable.inactive_heart) // This should be your outlined heart
            favoriteButton.clearColorFilter()
        }
    }

    private fun saveFavoriteState(favorite: Boolean) {
        // Save to SharedPreferences
        val sharedPref = getSharedPreferences("artwork_preferences", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("is_favorite", favorite)
            apply()
        }

        // Or save to your database
        // YourDatabaseHelper.updateFavoriteStatus(artworkId, favorite)
    }

    private fun loadFavoriteState(): Boolean {
        // Load from SharedPreferences
        val sharedPref = getSharedPreferences("artwork_preferences", MODE_PRIVATE)
        return sharedPref.getBoolean("is_favorite", false)

        // Or load from your database
        // return YourDatabaseHelper.isFavorite(artworkId)
    }

    private fun setupSimilarArtworks() {
        val container = findViewById<LinearLayout>(R.id.similarArtworksContainer)
        container.removeAllViews()

        for (artwork in similarArtworks) {
            // Create the artwork card first
            val artworkCard: View = layoutInflater.inflate(R.layout.item_artwork, container, false)

            // Find the ImageView in the card
            val artworkImage = artworkCard.findViewById<ImageView>(R.id.similarArtworkImage)

            // Set your artwork image using Glide, Picasso, or similar
            // Glide.with(this).load(artwork.getImageUrl()).into(artworkImage)
            // For now, you can set a placeholder or drawable resource
            // artworkImage.setImageResource(R.drawable.your_placeholder_image)

            // Set margin for spacing
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 20, 0) // left, top, right, bottom margins
            artworkCard.layoutParams = params

            // Add click listener for individual artwork cards
            artworkCard.setOnClickListener {
                val artworkIntent = Intent(this, ArtworkInfo::class.java)
                // Open corresponding artwork information of clicked card
                startActivity(artworkIntent)
            }

            // Add the card to the container
            container.addView(artworkCard)
        }
    }

    private fun setupClickListeners() {
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
            finish()
        }

        val scanMoreButton = findViewById<Button>(R.id.scanMoreButton)
        scanMoreButton.setOnClickListener {
            val scanIntent = Intent(this, Scan::class.java)
            startActivity(scanIntent)
            finish()
        }

        val reportButton = findViewById<ImageButton>(R.id.reportButton)
        reportButton.setOnClickListener {
            val reportIntent = Intent(this, Report::class.java)
            startActivity(reportIntent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Load favorite state when activity resumes
        isFavorite = loadFavoriteState()
        updateFavoriteButtonState()
    }
}