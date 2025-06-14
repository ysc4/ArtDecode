package com.example.artdecode.presentation.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.artdecode.R
import com.example.artdecode.ScanFrameOverlay
import com.example.artdecode.presentation.artworkinfo.ArtworkInfoActivity
import com.example.artdecode.utils.Event
import com.example.artdecode.data.model.ScanState // This import seems correct if ScanState is in data.model
import com.example.artdecode.data.model.Artwork


// Main Activity (View)
class ScanActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ScanActivity"
    }

    private val viewModel: ScanViewModel by viewModels()
    private lateinit var previewView: PreviewView
    private lateinit var scanOverlay: ScanFrameOverlay

    // Activity result launchers
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onCameraPermissionGranted()
        } else {
            viewModel.onCameraPermissionDenied()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        viewModel.onImageSelectedFromGallery(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Setup fullscreen
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        initializeViews()
        setupClickListeners()
        observeViewModel()
        checkInitialPermissions()
    }

    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        scanOverlay = findViewById(R.id.scanFrameOverlay)

        // Update scan frame when overlay is laid out
        scanOverlay.viewTreeObserver.addOnGlobalLayoutListener {
            viewModel.updateScanFrame(
                scanOverlay.width.toFloat(),
                scanOverlay.height.toFloat()
            )
        }
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.closeButton).setOnClickListener {
            viewModel.onCloseClicked()
        }

        findViewById<View>(R.id.galleryButton).setOnClickListener {
            viewModel.onGalleryClicked()
        }

        findViewById<View>(R.id.captureButton).setOnClickListener {
            viewModel.onCaptureClicked()
        }
    }

    private fun observeViewModel() {
        // Observe scan state
        viewModel.scanState.observe(this) { state ->
            updateUI(state)
        }

        // Handle navigation events - Now passing the full Artwork object
        viewModel.navigateToArtworkInfo.observe(this) { event ->
            event.getContentIfNotHandled()?.let { artwork ->
                navigateToArtworkInfo(artwork)
            }
        }

        // Handle messages
        viewModel.showMessage.observe(this, Event.EventObserver { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        })

        // Handle activity finish
        viewModel.finishActivity.observe(this, Event.EventObserver {
            finish()
        })

        // Handle permission requests
        viewModel.requestCameraPermission.observe(this, Event.EventObserver {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        })

        // Handle gallery opening
        viewModel.openGallery.observe(this, Event.EventObserver {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        })
    }

    private fun updateUI(state: ScanState) {
        when {
            state.isInitializing -> {
                Toast.makeText(this, "Initializing camera...", Toast.LENGTH_SHORT).show()
            }
            state.isScanning -> {
                Toast.makeText(this, "Capturing image...", Toast.LENGTH_SHORT).show()
            }
            state.hasCameraPermission && !state.isInitializing -> {
                startCameraPreview()
            }
            state.errorMessage != null -> {
                Toast.makeText(this, state.errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        // Update scan overlay if frame bounds are available
        state.scanFrame?.let { frame ->
            scanOverlay.updateFrame(frame)
        }
    }

    private fun checkInitialPermissions() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.checkCameraPermission(hasPermission)
    }

    private fun startCameraPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                viewModel.bindCameraUseCases(
                    cameraProvider,
                    this,
                    previewView.surfaceProvider
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Camera provider binding failed", exception)
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Updated to accept the complete Artwork object
    private fun navigateToArtworkInfo(artwork: Artwork) {
        val intent = Intent(this, ArtworkInfoActivity::class.java).apply {
            // Pass all relevant data as extras.
            // It's crucial that ArtworkInfoActivity.getArtworkFromIntent()
            // is updated to read these correctly.
            putExtra("ARTWORK_ID", artwork.id)
            putExtra("CAPTURED_IMAGE_URI", artwork.imageUri)
            putExtra("ART_STYLE", artwork.artStyle)
            putExtra("CONFIDENCE_SCORE", artwork.confidenceScore ?: 0f)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ScanActivity destroyed")
    }
}