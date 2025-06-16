package com.example.artdecode.presentation.report

/**
 * Data class representing a report in Firebase Realtime Database
 */
data class Report(
    val reportID: String = "",
    val reportInput: String = "",
    val status: String = "pending",
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "pending", 0L)
}
