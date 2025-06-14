package com.example.artdecode.data.model

import android.graphics.RectF
import android.net.Uri

data class ScanState(
    val hasCameraPermission: Boolean = false,
    val isInitializing: Boolean = false,
    val isScanning: Boolean = false,
    val isProcessing: Boolean = false,
    val scanFrame: RectF? = null,
    val capturedImageUri: Uri? = null,
    val errorMessage: String? = null
)