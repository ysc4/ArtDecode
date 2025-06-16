package com.example.artdecode.presentation.report

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.artdecode.R
import com.example.artdecode.presentation.artworkinfo.ArtworkInfoActivity
import com.google.android.material.snackbar.Snackbar // Import Snackbar

class ReportActivity : AppCompatActivity() {

    private val viewModel: ReportViewModel by viewModels()

    private var artworkId: String? = null
    private var capturedImageUri: String? = null
    private var artStyle: String? = null
    private var confidenceScore: Float? = null

    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report)

        rootView = findViewById(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        artworkId = intent.getStringExtra("ARTWORK_ID")
        capturedImageUri = intent.getStringExtra("CAPTURED_IMAGE_URI")
        artStyle = intent.getStringExtra("ART_STYLE")
        confidenceScore = intent.getFloatExtra("CONFIDENCE_SCORE", -1f).takeIf { it != -1f }


        val backButton: ImageButton = findViewById(R.id.backButton)
        val submitButton: Button = findViewById(R.id.submitButton)
        val reportInput: EditText = findViewById(R.id.reportInput)

        backButton.setOnClickListener {
            viewModel.onBackClicked(artworkId, capturedImageUri, artStyle, confidenceScore)
        }

        submitButton.setOnClickListener {
            val reportText = reportInput.text.toString().trim()

            if (reportText.isEmpty()) {
                Snackbar.make(rootView, "Please enter a report description", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (reportText.length < 10) {
                Snackbar.make(rootView, "Report must be at least 10 characters long", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            submitButton.isEnabled = false
            viewModel.onSubmitClicked(reportText)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.navigateToArtworkInfo.observe(this) { event ->
            event.getContentIfNotHandled()?.let { artworkInfoBundle ->
                val intent = Intent(this, ArtworkInfoActivity::class.java).apply {
                    putExtras(artworkInfoBundle)
                }
                startActivity(intent)
                finish()
            }
        }

        viewModel.showSuccessDialogAndFinish.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                Snackbar.make(rootView, "Report submitted successfully", Snackbar.LENGTH_LONG)
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                            super.onDismissed(transientBottomBar, event)
                            viewModel.onBackClicked(artworkId, capturedImageUri, artStyle, confidenceScore)
                        }
                    })
                    .show()
            }
        }

        viewModel.showError.observe(this) { event ->
            event.getContentIfNotHandled()?.let { errorMessage ->
                findViewById<Button>(R.id.submitButton).isEnabled = true
                Snackbar.make(rootView, "Error: $errorMessage", Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            findViewById<Button>(R.id.submitButton).isEnabled = !isLoading
        }
    }
}