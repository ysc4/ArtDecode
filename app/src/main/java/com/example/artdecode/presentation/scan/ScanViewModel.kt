// src/main/java/com/example/artdecode/presentation/scan/ScanViewModel.kt

package com.example.artdecode.presentation.scan

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.artdecode.data.model.ArtStyleClassifier
import com.example.artdecode.data.model.Artwork
import com.example.artdecode.data.model.ScanState
import com.example.artdecode.data.repository.ArtworkRepositoryImpl
import com.example.artdecode.data.repository.ScanRepository
import com.example.artdecode.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import com.example.artdecode.data.repository.SettingsRepository

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ScanViewModel"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private val artworkRepository = ArtworkRepositoryImpl(application)
    private val repository = ScanRepository() // Your ScanRepository instance
    private val artStyleClassifier = ArtStyleClassifier(application)
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    var imageCapture: ImageCapture? = null // Made public so fragment can access resolutionInfo
    private var currentScanFrame: RectF? = null
    private var lastPreviewWidth: Float = 0f
    private var lastPreviewHeight: Float = 0f

    // New properties to hold camera and preview configuration
    private var cameraOutputAspectRatio: Float = 0f
    private var previewViewScaleType: String = "FILL_CENTER" // Default to FILL_CENTER

    // Store current user ID
    private var currentUserId: String? = null

    private val _scanState = MutableLiveData<ScanState>(ScanState())
    val scanState: LiveData<ScanState> = _scanState

    private val _navigateToArtworkInfo = MutableLiveData<Event<Artwork>>()
    val navigateToArtworkInfo: LiveData<Event<Artwork>> = _navigateToArtworkInfo

    private val _showMessage = MutableLiveData<Event<String>>()
    val showMessage: LiveData<Event<String>> = _showMessage

    private val _finishActivity = MutableLiveData<Event<Unit>>()
    val finishActivity: LiveData<Event<Unit>> = _finishActivity

    private val _requestCameraPermission = MutableLiveData<Event<Unit>>()
    val requestCameraPermission: LiveData<Event<Unit>> = _requestCameraPermission

    private val _openGallery = MutableLiveData<Event<Unit>>()
    val openGallery: LiveData<Event<Unit>> = _openGallery

    private lateinit var settingsRepository: SettingsRepository

    init {
        settingsRepository = SettingsRepository(getApplication())
    }

    fun isGalleryEnabled(): Boolean {
        return !settingsRepository.isCameraOnlyMode()
    }

    fun setCurrentUserId(userId: String) {
        currentUserId = userId
        Log.d(TAG, "Current user ID set to: $userId")
        artworkRepository.setCurrentUserId(userId)
    }

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

    private fun startCamera() {
        updateState { it.copy(isInitializing = true) }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())
        cameraProviderFuture.addListener({
            try {
                cameraProviderFuture.get()
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
            // After binding, attempt to get the camera's output aspect ratio.
            // This might be more reliably available after the preview use case is active.
            imageCapture?.resolutionInfo?.let { resolutionInfo -> // Changed 'resolution' to 'resolutionInfo' for clarity
                cameraOutputAspectRatio = resolutionInfo.resolution.width.toFloat() / resolutionInfo.resolution.height.toFloat() // Access .resolution.width/height
                Log.d(TAG, "Camera Output Aspect Ratio detected: $cameraOutputAspectRatio")
            } ?: Log.w(TAG, "Could not determine camera output aspect ratio from ImageCapture.")

        } catch (exception: Exception) {
            Log.e(TAG, "Use case binding failed", exception)
            updateState { it.copy(errorMessage = "Camera binding failed") }
        }
    }

    // New setters to be called from the Fragment
    fun setPreviewViewScaleType(scaleType: String) {
        this.previewViewScaleType = scaleType
        Log.d(TAG, "PreviewView Scale Type set to: $scaleType")
    }

    fun updateScanFrame(viewWidth: Float, viewHeight: Float) {
        // Ensure aspect ratio is set, use a fallback if not yet available
        if (cameraOutputAspectRatio == 0f) {
            Log.w(TAG, "Camera output aspect ratio not set, using default 4:3 for scan frame calculation.")
            cameraOutputAspectRatio = 4f / 3f // Common fallback for camera preview
        }

        currentScanFrame = repository.getFrameDimensions(
            viewWidth,
            viewHeight,
            cameraOutputAspectRatio,
            previewViewScaleType
        )
        lastPreviewWidth = viewWidth
        lastPreviewHeight = viewHeight
        updateState { it.copy(scanFrame = currentScanFrame) }
        Log.d(TAG, "Scan Frame updated: ${currentScanFrame?.toShortString()}. Preview dimensions: ${viewWidth}x${viewHeight}")
    }

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
            val uniqueId = UUID.randomUUID().toString()
            cropGalleryImageToFrame(uri, uniqueId) { croppedUri ->
                classifyAndSaveImage(croppedUri, uniqueId)
            }
        } else {
            _showMessage.value = Event("Invalid image selected")
        }
    }

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

        val uniqueId = UUID.randomUUID().toString()
        val name = "scanned_${uniqueId}_${SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())}"

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
                        cropImageToFrame(savedUri, scanFrame, uniqueId, lastPreviewWidth, lastPreviewHeight) { croppedUri ->
                            updateState {
                                it.copy(
                                    isScanning = false,
                                    capturedImageUri = croppedUri
                                )
                            }
                            classifyAndSaveImage(croppedUri, uniqueId)
                        }
                    } else {
                        updateState {
                            it.copy(
                                isScanning = false,
                                errorMessage = "Failed to save image"
                            )
                        }
                        _showMessage.value = Event("Failed to save image")
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
                    _showMessage.value = Event("Capture failed: ${exception.message}")
                }
            }
        )
    }

    private fun classifyAndSaveImage(imageUri: Uri, artworkId: String) {
        updateState { it.copy(isProcessing = true) }

        viewModelScope.launch {
            try {
                val classificationResult = withContext(Dispatchers.Default) {
                    artStyleClassifier.classifyImage(imageUri)
                }

                if (classificationResult != null) {
                    val artworkToSave = Artwork(
                        id = artworkId,
                        imageUri = imageUri.toString(),
                        artStyle = classificationResult.artStyle,
                        confidenceScore = classificationResult.confidence,
                        userId = currentUserId,
                        capturedAt = System.currentTimeMillis()
                    )

                    Log.d(TAG, "Saving artwork with user ID: ${currentUserId}")
                    val savedArtwork = artworkRepository.saveArtwork(artworkToSave)

                    updateState { it.copy(isProcessing = false) }
                    _navigateToArtworkInfo.value = Event(savedArtwork)
                    _showMessage.value = Event("Artwork classified as ${classificationResult.artStyle}")

                } else {
                    updateState {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "Failed to classify image"
                        )
                    }
                    _showMessage.value = Event("Failed to classify artwork")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during classification and saving", e)
                updateState {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Processing failed: ${e.message}"
                    )
                }
                _showMessage.value = Event("Processing failed")
            }
        }
    }

    // Crop captured image to scan frame bounds
    private fun cropImageToFrame(
        originalUri: Uri,
        scanFrame: RectF, // This RectF is now relative to the visible camera feed, calculated by ScanRepository
        artworkId: String,
        previewViewWidth: Float, // The width of the PreviewView (the actual view's pixel width)
        previewViewHeight: Float, // The height of the PreviewView (the actual view's pixel height)
        callback: (Uri) -> Unit
    ) {
        cameraExecutor.execute {
            try {
                val inputStream = getApplication<Application>().contentResolver
                    .openInputStream(originalUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val rotationDegrees = getExifOrientation(getApplication(), originalUri)
                    val orientedBitmap = rotateBitmap(originalBitmap, rotationDegrees)

                    // Calculate the actual display rectangle of the camera feed within the PreviewView.
                    // This is still needed here to map the `scanFrame` (which is in preview coords)
                    // to the `orientedBitmap` (which is the full captured image).
                    val displayRect = repository.calculateCameraDisplayRect(
                        previewViewWidth,
                        previewViewHeight,
                        orientedBitmap.width.toFloat() / orientedBitmap.height.toFloat(), // Use captured image aspect ratio
                        previewViewScaleType
                    )

                    Log.d(TAG, "--- Cropping Debug Info (Camera) ---")
                    Log.d(TAG, "1. Original Captured Bitmap (after EXIF rotation): ${orientedBitmap.width}x${orientedBitmap.height}")
                    Log.d(TAG, "2. Preview View Actual Dimensions: ${previewViewWidth}x${previewViewHeight}")
                    Log.d(TAG, "3. Scan Frame (from Repo, relative to visible feed): L:${scanFrame.left}, T:${scanFrame.top}, R:${scanFrame.right}, B:${scanFrame.bottom}")
                    Log.d(TAG, "   Scan Frame Width: ${scanFrame.width()}, Height: ${scanFrame.height()}")
                    Log.d(TAG, "4. Calculated Camera Display Rect (within Preview View): L:${displayRect.left}, T:${displayRect.top}, R:${displayRect.right}, B:${displayRect.bottom}")
                    Log.d(TAG, "   Display Rect Width: ${displayRect.width()}, Height: ${displayRect.height()}")


                    // Map the scanFrame (which is relative to the `displayRect`'s origin in PreviewView coords)
                    // to the full `orientedBitmap` pixel space.
                    // The `scanFrame.left/top` are already offsets from the PreviewView's (0,0) and represent the
                    // top-left of the desired frame *within the visible camera feed*.
                    // So we effectively translate it to be relative to displayRect's top-left, then scale.

                    val cropX = ((scanFrame.left - displayRect.left) * (orientedBitmap.width / displayRect.width())).toInt().coerceAtLeast(0)
                    val cropY = ((scanFrame.top - displayRect.top) * (orientedBitmap.height / displayRect.height())).toInt().coerceAtLeast(0)

                    val finalCropWidth = (scanFrame.width() * (orientedBitmap.width / displayRect.width())).toInt().coerceAtLeast(1)
                    val finalCropHeight = (scanFrame.height() * (orientedBitmap.height / displayRect.height())).toInt().coerceAtLeast(1)

                    // Ensure crop coordinates and dimensions do not exceed bitmap bounds
                    val actualCropX = cropX.coerceIn(0, orientedBitmap.width - finalCropWidth)
                    val actualCropY = cropY.coerceIn(0, orientedBitmap.height - finalCropHeight)
                    val actualFinalCropWidth = finalCropWidth.coerceAtMost(orientedBitmap.width - actualCropX)
                    val actualFinalCropHeight = finalCropHeight.coerceAtMost(orientedBitmap.height - actualCropY)


                    Log.d(TAG, "5. Calculated Crop Area (in Bitmap Pixels): X=${actualCropX}, Y=${actualCropY}, Width=${actualFinalCropWidth}, Height=${actualFinalCropHeight}")

                    val croppedBitmap = try {
                        Bitmap.createBitmap(
                            orientedBitmap,
                            actualCropX,
                            actualCropY,
                            actualFinalCropWidth,
                            actualFinalCropHeight
                        )
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Bitmap.createBitmap failed with IllegalArgumentException: ${e.message}. Using whole oriented bitmap. Crop params: X=$actualCropX, Y=$actualCropY, W=$actualFinalCropWidth, H=$actualFinalCropHeight, Bitmap: ${orientedBitmap.width}x${orientedBitmap.height}")
                        orientedBitmap // Fallback to full image if arguments are invalid
                    }


                    val croppedUri = saveCroppedImage(croppedBitmap, artworkId)
                    Log.d(TAG, "6. Saved Cropped Image URI: $croppedUri")
                    Log.d(TAG, "---------------------------------")

                    // Proper recycling of bitmaps to prevent memory leaks
                    if (croppedBitmap !== orientedBitmap && orientedBitmap !== originalBitmap) {
                        originalBitmap.recycle()
                        orientedBitmap.recycle()
                    } else if (croppedBitmap !== orientedBitmap) {
                        orientedBitmap.recycle()
                    } else if (orientedBitmap !== originalBitmap) {
                        originalBitmap.recycle()
                    }

                    ContextCompat.getMainExecutor(getApplication()).execute {
                        callback(croppedUri)
                    }
                } else {
                    Log.e(TAG, "Failed to decode original bitmap for cropping. Original URI: $originalUri")
                    ContextCompat.getMainExecutor(getApplication()).execute {
                        callback(originalUri) // Fallback to original if decoding fails
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "CRITICAL ERROR: Image cropping or rotation failed entirely for $originalUri: ${exception.message}", exception)
                ContextCompat.getMainExecutor(getApplication()).execute {
                    callback(originalUri) // Fallback to original on error
                }
            }
        }
    }

    // This method is for gallery images, where we don't have a live preview frame.
    // It will calculate a default frame based on the bitmap's own dimensions.
    private fun cropGalleryImageToFrame(
        originalUri: Uri,
        artworkId: String,
        callback: (Uri) -> Unit
    ) {
        cameraExecutor.execute {
            try {
                val inputStream = getApplication<Application>().contentResolver
                    .openInputStream(originalUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val rotationDegrees = getExifOrientation(getApplication(), originalUri)
                    val orientedBitmap = rotateBitmap(originalBitmap, rotationDegrees)

                    // For gallery images, we apply a default frame relative to the bitmap's own dimensions.
                    // You might want to adjust these percentages for gallery images.
                    val frameWidth = orientedBitmap.width * 0.75f
                    val frameHeight = frameWidth * 1.2f // Maintain 1:1.2 aspect ratio

                    val left = (orientedBitmap.width - frameWidth) / 2f
                    val top = (orientedBitmap.height - frameHeight) / 2f
                    val right = left + frameWidth
                    val bottom = top + frameHeight

                    val defaultFrameInBitmapCoords = RectF(left, top, right, bottom)

                    Log.d(TAG, "--- Cropping Debug Info (Gallery) ---")
                    Log.d(TAG, "1. Gallery Bitmap dimensions (after EXIF rotation): ${orientedBitmap.width}x${orientedBitmap.height}")
                    Log.d(TAG, "2. Default Frame (calculated for bitmap coords): L:${defaultFrameInBitmapCoords.left}, T:${defaultFrameInBitmapCoords.top}, R:${defaultFrameInBitmapCoords.right}, B:${defaultFrameInBitmapCoords.bottom}")

                    val cropX = defaultFrameInBitmapCoords.left.toInt().coerceAtLeast(0)
                    val cropY = defaultFrameInBitmapCoords.top.toInt().coerceAtLeast(0)
                    val calculatedCropWidth = defaultFrameInBitmapCoords.width().toInt()
                    val calculatedCropHeight = defaultFrameInBitmapCoords.height().toInt()

                    val finalCropWidth = calculatedCropWidth.coerceAtMost(orientedBitmap.width - cropX).coerceAtLeast(1)
                    val finalCropHeight = calculatedCropHeight.coerceAtMost(orientedBitmap.height - cropY).coerceAtLeast(1)

                    Log.d(TAG, "3. Calculated Gallery Crop Area (in Bitmap Pixels): X=${cropX}, Y=${cropY}, Width=${finalCropWidth}, Height=${finalCropHeight}")

                    val croppedBitmap = try {
                        Bitmap.createBitmap(
                            orientedBitmap,
                            cropX,
                            cropY,
                            finalCropWidth,
                            finalCropHeight
                        )
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Bitmap.createBitmap failed for gallery image: ${e.message}. Using whole oriented bitmap. Crop params: X=$cropX, Y=$cropY, W=$finalCropWidth, H=$finalCropHeight, Bitmap: ${orientedBitmap.width}x${orientedBitmap.height}")
                        orientedBitmap // Fallback
                    }

                    val croppedUri = saveCroppedImage(croppedBitmap, artworkId)
                    Log.d(TAG, "4. Saved Cropped Image URI: $croppedUri")
                    Log.d(TAG, "---------------------------------")

                    // Proper recycling of bitmaps
                    if (croppedBitmap !== orientedBitmap && orientedBitmap !== originalBitmap) {
                        originalBitmap.recycle()
                        orientedBitmap.recycle()
                    } else if (croppedBitmap !== orientedBitmap) {
                        orientedBitmap.recycle()
                    } else if (orientedBitmap !== originalBitmap) {
                        originalBitmap.recycle()
                    }

                    ContextCompat.getMainExecutor(getApplication()).execute {
                        callback(croppedUri)
                    }
                } else {
                    Log.e(TAG, "Failed to decode gallery image for processing. Original URI: $originalUri")
                    ContextCompat.getMainExecutor(getApplication()).execute {
                        callback(originalUri)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing gallery image: ${e.message}", e)
                ContextCompat.getMainExecutor(getApplication()).execute {
                    callback(originalUri)
                }
            }
        }
    }

    private fun saveCroppedImage(bitmap: Bitmap, artworkId: String): Uri {
        val name = "cropped_${artworkId}_${SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                Log.d(TAG, "Successfully saved cropped bitmap to $savedUri")
            } ?: Log.e(TAG, "Failed to get outputStream for $savedUri")
        } ?: Log.e(TAG, "Failed to insert new content for saving cropped image.")

        return uri ?: throw Exception("Failed to save cropped image: URI was null")
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0 || bitmap == null) return bitmap
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotatedBitmap !== bitmap) {
            bitmap.recycle()
        }
        return rotatedBitmap
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        var rotationDegrees = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EXIF orientation for URI $uri: ${e.message}", e)
        }
        return rotationDegrees
    }

    private fun updateState(update: (ScanState) -> ScanState) {
        _scanState.value = update(_scanState.value ?: ScanState())
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
        artStyleClassifier.close()
        Log.d(TAG, "ScanViewModel onCleared: Executor shutdown, classifier closed.")
    }
}