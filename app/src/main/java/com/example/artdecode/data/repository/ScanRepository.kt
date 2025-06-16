package com.example.artdecode.data.repository

import android.graphics.RectF
import android.net.Uri
import android.util.Log
import java.util.Locale

class ScanRepository {

    private val TAG = "ScanRepository"

    // Define desired frame percentages relative to the *visible* camera feed.
    // Adjust these values as needed to match your desired frame size.
    private val FRAME_WIDTH_PERCENT = 0.75f
    private val FRAME_HEIGHT_ASPECT_RATIO = 1.2f // Frame Height = Frame Width * 1.2 (e.g., for a slightly taller frame)

    /**
     * Calculates the dimensions of the scan frame relative to the visible camera feed within the PreviewView.
     *
     * @param previewViewWidth The actual width of the PreviewView in pixels.
     * @param previewViewHeight The actual height of the PreviewView in pixels.
     * @param imageAspectRatio The aspect ratio (width / height) of the camera's output stream.
     * @param previewViewScaleType The scaleType of the PreviewView (e.g., "FILL_CENTER", "FIT_CENTER").
     * @return A RectF representing the scan frame in the PreviewView's coordinate system,
     * but aligned with the visible camera feed.
     */
    fun getFrameDimensions(
        previewViewWidth: Float,
        previewViewHeight: Float,
        imageAspectRatio: Float,
        previewViewScaleType: String
    ): RectF {
        // First, calculate the actual display rectangle of the camera feed within the PreviewView.
        val displayRect = calculateCameraDisplayRect(
            previewViewWidth,
            previewViewHeight,
            imageAspectRatio,
            previewViewScaleType
        )

        Log.d(TAG, "Display Rect calculated: ${displayRect.toShortString()} (W:${displayRect.width()}, H:${displayRect.height()})")

        // Now, calculate the scan frame relative to this `displayRect`
        val frameWidth = displayRect.width() * FRAME_WIDTH_PERCENT
        val frameHeight = frameWidth * FRAME_HEIGHT_ASPECT_RATIO // Maintain desired frame aspect ratio

        // Center the frame within the displayRect
        val left = displayRect.centerX() - (frameWidth / 2f)
        val top = displayRect.centerY() - (frameHeight / 2f)
        val right = left + frameWidth
        val bottom = top + frameHeight

        // Ensure the calculated frame is within the displayRect bounds
        val finalFrame = RectF(
            left.coerceAtLeast(displayRect.left),
            top.coerceAtLeast(displayRect.top),
            right.coerceAtMost(displayRect.right),
            bottom.coerceAtMost(displayRect.bottom)
        )

        Log.d(TAG, "Scan Frame (relative to visible feed) calculated: ${finalFrame.toShortString()}")
        return finalFrame
    }

    /**
     * Helper method to calculate the actual display rectangle of the camera feed within the PreviewView.
     * This is crucial for accurate coordinate mapping when the PreviewView's aspect ratio
     * doesn't exactly match the camera's output, or when scaleType is 'fitCenter'.
     *
     * @param previewViewWidth The actual width of the PreviewView in pixels.
     * @param previewViewHeight The actual height of the PreviewView in pixels.
     * @param imageAspectRatio The aspect ratio (width / height) of the camera stream.
     * @param scaleType The scaleType of the PreviewView (e.g., PreviewView.ScaleType.FILL_CENTER.name, PreviewView.ScaleType.FIT_CENTER.name).
     * @return A RectF representing the actual area where the camera image is displayed within the PreviewView.
     */
    internal fun calculateCameraDisplayRect(
        previewViewWidth: Float,
        previewViewHeight: Float,
        imageAspectRatio: Float,
        scaleType: String
    ): RectF {
        val previewAspectRatio = previewViewWidth / previewViewHeight

        var displayedImageWidth: Float
        var displayedImageHeight: Float

        return when (scaleType.uppercase()) {
            "FILL_CENTER" -> {
                // The camera output is scaled to fill the preview view, potentially cropping the image itself.
                // The display rectangle is the entire preview view.
                Log.d(TAG, "ScaleType: FILL_CENTER. Display Rect is full PreviewView.")
                RectF(0f, 0f, previewViewWidth, previewViewHeight)
            }
            "FIT_CENTER" -> {
                // The camera output is scaled to fit within the preview view, with letterboxing/pillarboxing.
                if (imageAspectRatio > previewAspectRatio) {
                    // Image is wider than preview (e.g., 16:9 camera on a 4:3 preview), pillarboxing (black bars on top/bottom)
                    displayedImageWidth = previewViewWidth
                    displayedImageHeight = previewViewWidth / imageAspectRatio
                    Log.d(TAG, "ScaleType: FIT_CENTER (Pillarboxing). Calculated displayed: ${displayedImageWidth}x${displayedImageHeight}")
                } else {
                    // Image is taller than preview (e.g., 4:3 camera on a 16:9 preview), letterboxing (black bars on sides)
                    displayedImageHeight = previewViewHeight
                    displayedImageWidth = previewViewHeight * imageAspectRatio
                    Log.d(TAG, "ScaleType: FIT_CENTER (Letterboxing). Calculated displayed: ${displayedImageWidth}x${displayedImageHeight}")
                }

                val xOffset = (previewViewWidth - displayedImageWidth) / 2
                val yOffset = (previewViewHeight - displayedImageHeight) / 2

                RectF(
                    xOffset,
                    yOffset,
                    xOffset + displayedImageWidth,
                    yOffset + displayedImageHeight
                )
            }
            else -> {
                Log.w(TAG, "Unsupported PreviewView scaleType '$scaleType'. Assuming FILL_CENTER behavior for display rect calculation.")
                RectF(0f, 0f, previewViewWidth, previewViewHeight)
            }
        }
    }

    /**
     * Validates if the given Uri is a valid image Uri.
     * This is a placeholder and should be expanded based on your specific validation needs.
     */
    fun validateImageUri(uri: Uri): Boolean {
        // Simple check: ensure the scheme is not null and is content or file
        return uri.scheme != null && (uri.scheme == "content" || uri.scheme == "file")
    }
}