// ScanActivity.kt
package com.example.artdecode

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.text.SimpleDateFormat

class Scan : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    companion object {
        private const val TAG = "ScanActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                val intent = Intent(this, ArtworkInfo::class.java).apply {
                    putExtra("SELECTED_IMAGE_URI", uri.toString()) // Pass URI as a string
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Hide status bar for fullscreen experience
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        previewView = findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        findViewById<View>(R.id.closeButton).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.galleryButton).setOnClickListener {
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        findViewById<View>(R.id.captureButton).setOnClickListener {
            takePhoto()
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()

        imageCapture = ImageCapture.Builder()
            .setTargetResolution(Size(1080, 1920)) // Or use .setTargetAspectRatio for flexibility
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Or MAX_QUALITY
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                preview,
                imageCapture // Pass the initialized imageCapture instance
            )
            Log.d(TAG, "Camera bound successfully")
        } catch (exc: Exception) {
            Log.e(TAG, "Failed to bind camera use cases: ${exc.message}", exc)
        }
    }

    private fun takePhoto() {
        val imageCapture = this.imageCapture ?: run {
            Log.e(TAG, "ImageCapture use case not initialized.")
            Toast.makeText(this, "Camera not ready.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create time-stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, java.util.Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ArtDecode-Photos")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(
                        baseContext,
                        "Photo capture failed: ${exc.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    if (savedUri != null) {
                        Log.d(TAG, "Photo capture succeeded: $savedUri")
                        navigateToArtworkInfo(savedUri)
                    } else {
                        Log.e(TAG, "Photo capture succeeded but URI is null")
                        Toast.makeText(baseContext, "Failed to get photo URI.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        )
    }

    private fun navigateToArtworkInfo(imageUri: Uri) {
        val intent = Intent(this, ArtworkInfo::class.java).apply {
            putExtra("CAPTURED_IMAGE_URI", imageUri.toString()) // Use a different key for clarity
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}