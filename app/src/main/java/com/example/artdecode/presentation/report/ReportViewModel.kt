package com.example.artdecode.presentation.report

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.artdecode.utils.Event
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import java.util.UUID

class ReportViewModel : ViewModel() {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    private val _navigateToArtworkInfo = MutableLiveData<Event<Bundle>>()
    val navigateToArtworkInfo: LiveData<Event<Bundle>> = _navigateToArtworkInfo

    private val _showSuccessDialogAndFinish = MutableLiveData<Event<Unit>>()
    val showSuccessDialogAndFinish: LiveData<Event<Unit>> = _showSuccessDialogAndFinish

    private val _showError = MutableLiveData<Event<String>>()
    val showError: LiveData<Event<String>> = _showError

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun onBackClicked(artworkId: String?, capturedImageUri: String?, artStyle: String?, confidenceScore: Float?) {
        val bundle = Bundle().apply {
            artworkId?.let { putString("ARTWORK_ID", it) }
            capturedImageUri?.let { putString("CAPTURED_IMAGE_URI", it) }
            artStyle?.let { putString("ART_STYLE", it) }
            confidenceScore?.let { putFloat("CONFIDENCE_SCORE", it) }
        }
        _navigateToArtworkInfo.value = Event(bundle)
    }

    fun onSubmitClicked(reportInput: String) {
        if (reportInput.trim().isEmpty()) {
            _showError.value = Event("Report cannot be empty")
            return
        }

        _isLoading.value = true

        val reportId = UUID.randomUUID().toString()

        val reportData = mapOf(
            "reportID" to reportId,
            "reportInput" to reportInput.trim(),
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        database.child("reports").child(reportId)
            .setValue(reportData)
            .addOnCompleteListener { task ->
                _isLoading.value = false

                if (task.isSuccessful) {
                    _showSuccessDialogAndFinish.value = Event(Unit)
                } else {
                    val errorMessage = task.exception?.message ?: "Failed to submit report. Please try again."
                    _showError.value = Event(errorMessage)
                }
            }
    }
}