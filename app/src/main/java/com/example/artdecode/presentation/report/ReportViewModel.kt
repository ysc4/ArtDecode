package com.example.artdecode

import android.os.Bundle // Import Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.artdecode.utils.Event
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import java.util.UUID

class ReportViewModel : ViewModel() {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    // Navigation events - now carries a Bundle
    private val _navigateToArtworkInfo = MutableLiveData<Event<Bundle>>()
    val navigateToArtworkInfo: LiveData<Event<Bundle>> = _navigateToArtworkInfo

    private val _showSuccessDialogAndFinish = MutableLiveData<Event<Unit>>()
    val showSuccessDialogAndFinish: LiveData<Event<Unit>> = _showSuccessDialogAndFinish

    // Error handling
    private val _showError = MutableLiveData<Event<String>>()
    val showError: LiveData<Event<String>> = _showError

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Called when the back button is clicked or after successful submission.
     * Passes back the identifying data of the artwork to navigate to the correct ArtworkInfoActivity.
     */
    fun onBackClicked(artworkId: String?, capturedImageUri: String?, artStyle: String?, confidenceScore: Float?) {
        val bundle = Bundle().apply {
            artworkId?.let { putString("ARTWORK_ID", it) }
            capturedImageUri?.let { putString("CAPTURED_IMAGE_URI", it) }
            artStyle?.let { putString("ART_STYLE", it) }
            confidenceScore?.let { putFloat("CONFIDENCE_SCORE", it) }
        }
        _navigateToArtworkInfo.value = Event(bundle)
    }

    /**
     * Called when the submit button is clicked.
     * Stores the report in Firebase Realtime Database.
     */
    fun onSubmitClicked(reportInput: String) {
        if (reportInput.trim().isEmpty()) {
            _showError.value = Event("Report cannot be empty")
            return
        }

        _isLoading.value = true

        // Generate unique report ID
        val reportId = UUID.randomUUID().toString()

        // Create report data structure
        val reportData = mapOf(
            "reportID" to reportId,
            "reportInput" to reportInput.trim(),
            "status" to "pending", // You can use "pending", "reviewed", "resolved", etc.
            "timestamp" to System.currentTimeMillis() // Optional: add timestamp
        )

        // Store in Firebase under "reports" node
        database.child("reports").child(reportId)
            .setValue(reportData)
            .addOnCompleteListener { task ->
                _isLoading.value = false

                if (task.isSuccessful) {
                    _showSuccessDialogAndFinish.value = Event(Unit) // Trigger success dialog
                } else {
                    val errorMessage = task.exception?.message ?: "Failed to submit report. Please try again."
                    _showError.value = Event(errorMessage)
                }
            }
    }
}