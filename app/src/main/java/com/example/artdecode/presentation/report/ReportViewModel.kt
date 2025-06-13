package com.example.artdecode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.artdecode.utils.Event // Assuming Event class is in utils

class ReportViewModel : ViewModel() {

    // To signal navigation back to ArtworkInfo
    private val _navigateToArtworkInfo = MutableLiveData<Event<Unit>>()
    val navigateToArtworkInfo: LiveData<Event<Unit>> = _navigateToArtworkInfo

    // To signal that the report submission was successful and the success dialog should be shown
    private val _showSuccessDialogAndFinish = MutableLiveData<Event<Unit>>()
    val showSuccessDialogAndFinish: LiveData<Event<Unit>> = _showSuccessDialogAndFinish

    // In a real app, you'd likely have some data for the report
    // For example:
    // val reportTitle = MutableLiveData<String>()
    // val reportDescription = MutableLiveData<String>()
    // ... and methods to update them from EditTexts in the Activity

    /**
     * Called when the back button is clicked.
     */
    fun onBackClicked() {
        _navigateToArtworkInfo.value = Event(Unit)
    }

    /**
     * Called when the submit button is clicked.
     * In a real app, this would involve sending data to a backend or local database.
     */
    fun onSubmitClicked() {
        // --- Simulate Report Submission Logic ---
        // 1. Validate input (if any)
        // 2. Perform submission (e.g., API call, database write)
        // 3. On success:
        _showSuccessDialogAndFinish.value = Event(Unit)
        // 4. On failure:
        //    _showError.value = Event("Failed to submit report. Please try again.")
    }
}