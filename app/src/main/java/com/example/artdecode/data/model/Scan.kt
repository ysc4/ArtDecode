package com.example.artdecode.data.model

import android.graphics.RectF
import android.net.Uri

data class ScanState(
    val isScanning: Boolean = false,
    val isInitializing: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val errorMessage: String? = null,
    val capturedImageUri: Uri? = null,
    val scanFrame: RectF? = null
)

data class CameraConfiguration(
    val targetWidth: Int = 1080,
    val targetHeight: Int = 1920,
    val quality: Int = 100
)