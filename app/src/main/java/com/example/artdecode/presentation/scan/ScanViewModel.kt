package com.example.artdecode.presentation.scan

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.artdecode.data.model.ScanState
import com.example.artdecode.data.repository.ScanRepository
import com.example.artdecode.utils.Event
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ScanViewModel"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private val repository = ScanRepository()
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var currentScanFrame: RectF? = null

    // State management
    private val _scanState = MutableLiveData<ScanState>(ScanState())
    val scanState: LiveData<ScanState> = _scanState

    // Events for UI navigation and actions
    private val _navigateToArtworkInfo = MutableLiveData<Event<Uri>>()
    val navigateToArtworkInfo: LiveData<Event<Uri>> = _navigateToArtworkInfo

    private val _showMessage = MutableLiveData<Event<String>>()
    val showMessage: LiveData<Event<String>> = _showMessage

    private val _finishActivity = MutableLiveData<Event<Unit>>()
    val finishActivity: LiveData<Event<Unit>> = _finishActivity

    private val _requestCameraPermission = MutableLiveData<Event<Unit>>()
    val requestCameraPermission: LiveData<Event<Unit>> = _requestCameraPermission

    private val _openGallery = MutableLiveData<Event<Unit>>()
    val openGallery: LiveData<Event<Unit>> = _openGallery

    // Camera permission handling
    fun checkCameraPermission(isGranted: Boolean) {
        if (isGranted) {
            updateState { it.copy(hasCameraPermission = true) }
            startCamera()
        } else {
            _requestCameraPermission.value = Event(Unit)
        }
    }

    fun onCameraPermissionGranted() {
        updateState { it.copy(hasCameraPermission = true) }
        startCamera()
    }

    fun onCameraPermissionDenied() {
        updateState { it.copy(hasCameraPermission = false) }
        _showMessage.value = Event("Camera permission is required to scan artwork")
    }

    // Camera setup
    private fun startCamera() {
        updateState { it.copy(isInitializing = true) }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())
        cameraProviderFuture.addListener({
            try {
                updateState { it.copy(isInitializing = false) }
            } catch (exception: Exception) {
                Log.e(TAG, "Camera initialization failed", exception)
                updateState {
                    it.copy(
                        isInitializing = false,
                        errorMessage = "Failed to initialize camera"
                    )
                }
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    fun bindCameraUseCases(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider
    ) {
        val config = repository.getCameraConfiguration()

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            Log.d(TAG, "Camera use cases bound successfully")
        } catch (exception: Exception) {
            Log.e(TAG, "Use case binding failed", exception)
            updateState { it.copy(errorMessage = "Camera binding failed") }
        }
    }

    // Scan frame management
    fun updateScanFrame(viewWidth: Float, viewHeight: Float) {
        currentScanFrame = repository.getFrameDimensions(viewWidth, viewHeight)
        updateState { it.copy(scanFrame = currentScanFrame) }
    }

    // Action handlers
    fun onCaptureClicked() {
        captureImageInFrame()
    }

    fun onGalleryClicked() {
        _openGallery.value = Event(Unit)
    }

    fun onCloseClicked() {
        _finishActivity.value = Event(Unit)
    }

    fun onImageSelectedFromGallery(uri: Uri?) {
        if (uri != null && repository.validateImageUri(uri)) {
            _navigateToArtworkInfo.value = Event(uri)
        } else {
            _showMessage.value = Event("Invalid image selected")
        }
    }

    // Capture image within frame bounds
    private fun captureImageInFrame() {
        val imageCapture = this.imageCapture
        val scanFrame = currentScanFrame

        if (imageCapture == null) {
            _showMessage.value = Event("Camera not ready")
            return
        }

        if (scanFrame == null) {
            _showMessage.value = Event("Scan frame not initialized")
            return
        }

        updateState { it.copy(isScanning = true) }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ArtDecode")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            getApplication<Application>().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(getApplication()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    if (savedUri != null) {
                        // Crop the image to the scan frame bounds
                        cropImageToFrame(savedUri, scanFrame) { croppedUri ->
                            updateState {
                                it.copy(
                                    isScanning = false,
                                    capturedImageUri = croppedUri
                                )
                            }
                            _navigateToArtworkInfo.value = Event(croppedUri)
                        }
                    } else {
                        updateState {
                            it.copy(
                                isScanning = false,
                                errorMessage = "Failed to save image"
                            )
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exception)
                    updateState {
                        it.copy(
                            isScanning = false,
                            errorMessage = "Capture failed: ${exception.message}"
                        )
                    }
                }
            }
        )
    }

    // Crop captured image to scan frame bounds
    private fun cropImageToFrame(
        originalUri: Uri,
        scanFrame: RectF,
        callback: (Uri) -> Unit
    ) {
        cameraExecutor.execute {
            try {
                val inputStream = getApplication<Application>().contentResolver
                    .openInputStream(originalUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    // Calculate crop bounds relative to the original image
                    val scaleX = originalBitmap.width.toFloat() / scanFrame.width()
                    val scaleY = originalBitmap.height.toFloat() / scanFrame.height()

                    val cropWidth = (scanFrame.width() * scaleX).toInt()
                    val cropHeight = (scanFrame.height() * scaleY).toInt()
                    val cropX = ((originalBitmap.width - cropWidth) / 2).coerceAtLeast(0)
                    val cropY = ((originalBitmap.height - cropHeight) / 2).coerceAtLeast(0)

                    val croppedBitmap = Bitmap.createBitmap(
                        originalBitmap,
                        cropX,
                        cropY,
                        cropWidth.coerceAtMost(originalBitmap.width - cropX),
                        cropHeight.coerceAtMost(originalBitmap.height - cropY)
                    )

                    // Save cropped image
                    val croppedUri = saveCroppedImage(croppedBitmap)
                    originalBitmap.recycle()
                    croppedBitmap.recycle()

                    ContextCompat.getMainExecutor(getApplication()).execute {
                        callback(croppedUri)
                    }
                } else {
                    ContextCompat.getMainExecutor(getApplication()).execute {
                        callback(originalUri) // Fallback to original if cropping fails
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Image cropping failed", exception)
                ContextCompat.getMainExecutor(getApplication()).execute {
                    callback(originalUri) // Fallback to original if cropping fails
                }
            }
        }
    }

    private fun saveCroppedImage(bitmap: Bitmap): Uri {
        val name = "cropped_${SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ArtDecode")
            }
        }

        val uri = getApplication<Application>().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        uri?.let { savedUri ->
            getApplication<Application>().contentResolver.openOutputStream(savedUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }
        }

        return uri ?: throw Exception("Failed to save cropped image")
    }

    // Helper function to update state
    private fun updateState(update: (ScanState) -> ScanState) {
        _scanState.value = update(_scanState.value ?: ScanState())
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
    }
}