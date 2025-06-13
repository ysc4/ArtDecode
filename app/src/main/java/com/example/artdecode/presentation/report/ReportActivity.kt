package com.example.artdecode.presentation.report

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // Import for by viewModels()
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.artdecode.R
import com.example.artdecode.ReportViewModel
import com.example.artdecode.presentation.artworkinfo.ArtworkInfoActivity

class ReportActivity : AppCompatActivity() {

    private val viewModel: ReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backButton: ImageButton = findViewById(R.id.backButton)
        val submitButton: Button = findViewById(R.id.submitButton)

        backButton.setOnClickListener {
            viewModel.onBackClicked()
        }

        submitButton.setOnClickListener {
            // In a real app, you might first gather data from EditTexts and pass to ViewModel
            // e.g., viewModel.submitReport(titleEditText.text.toString(), descriptionEditText.text.toString())
            viewModel.onSubmitClicked()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.navigateToArtworkInfo.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                // Navigate back to ArtworkInfo (or simply finish if ArtworkInfo is always the previous screen)
                // If ArtworkInfo is not guaranteed to be the direct parent, starting a new Intent is safer.
                // However, if it's always just "back", then finish() might be enough.
                // For this example, we'll keep the explicit navigation to be clear.
                val intent = Intent(this, ArtworkInfoActivity::class.java)
                // intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP // Optional: if you want to clear stack above ArtworkInfo
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
                        finish() // Finish the activity after the dialog is dismissed
                    }
                    .setOnCancelListener {
                        finish() // Also finish if the dialog is cancelled (e.g., by pressing back)
                    }
                    .show()
            }
        }
    }
}