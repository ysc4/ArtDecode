package com.example.artdecode.presentation.report

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.artdecode.R
import com.example.artdecode.ReportViewModel
import com.example.artdecode.presentation.artworkinfo.ArtworkInfoActivity

class ReportActivity : AppCompatActivity() {

    private val viewModel: ReportViewModel by viewModels()

    // Variables to hold the artwork data passed from ArtworkInfoActivity
    private var artworkId: String? = null
    private var capturedImageUri: String? = null
    private var artStyle: String? = null
    private var confidenceScore: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve artwork data from the Intent
        artworkId = intent.getStringExtra("ARTWORK_ID")
        capturedImageUri = intent.getStringExtra("CAPTURED_IMAGE_URI")
        artStyle = intent.getStringExtra("ART_STYLE")
        confidenceScore = intent.getFloatExtra("CONFIDENCE_SCORE", -1f).takeIf { it != -1f }


        val backButton: ImageButton = findViewById(R.id.backButton)
        val submitButton: Button = findViewById(R.id.submitButton)
        val reportInput: EditText = findViewById(R.id.reportInput)

        backButton.setOnClickListener {
            // Pass the retrieved artwork data to the ViewModel when back is clicked
            viewModel.onBackClicked(artworkId, capturedImageUri, artStyle, confidenceScore)
        }

        submitButton.setOnClickListener {
            val reportText = reportInput.text.toString().trim()
            if (reportText.isEmpty()) {
                Toast.makeText(this, "Please enter a report description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable submit button to prevent multiple submissions
            submitButton.isEnabled = false
            viewModel.onSubmitClicked(reportText)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.navigateToArtworkInfo.observe(this) { event ->
            event.getContentIfNotHandled()?.let { artworkInfoBundle ->
                val intent = Intent(this, ArtworkInfoActivity::class.java).apply {
                    putExtras(artworkInfoBundle) // Put the Bundle back into the Intent
                }
                startActivity(intent)
                finish()
            }
        }

        viewModel.showSuccessDialogAndFinish.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                AlertDialog.Builder(this)
                    .setTitle("Success")
                    .setMessage("Report submitted successfully")
                    .setPositiveButton("OK") { _, _ ->
                        // After success, also navigate back to the original ArtworkInfoActivity
                        viewModel.onBackClicked(artworkId, capturedImageUri, artStyle, confidenceScore)
                    }
                    .setOnCancelListener {
                        // Also navigate back if dialog is cancelled
                        viewModel.onBackClicked(artworkId, capturedImageUri, artStyle, confidenceScore)
                    }
                    .show()
            }
        }

        viewModel.showError.observe(this) { event ->
            event.getContentIfNotHandled()?.let { errorMessage ->
                findViewById<Button>(R.id.submitButton).isEnabled = true // Re-enable submit button on error
                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(errorMessage)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            findViewById<Button>(R.id.submitButton).isEnabled = !isLoading
        }
    }
}