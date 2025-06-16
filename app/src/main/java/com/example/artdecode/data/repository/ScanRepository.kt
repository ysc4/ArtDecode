package com.example.artdecode.data.repository

import android.graphics.RectF
import android.net.Uri
import android.util.Log
import java.util.Locale

class ScanRepository {

    private val TAG = "ScanRepository"
    private val FRAME_WIDTH_PERCENT = 0.75f
    private val FRAME_HEIGHT_ASPECT_RATIO = 1.2f

    fun getFrameDimensions(
        previewViewWidth: Float,
        previewViewHeight: Float,
        imageAspectRatio: Float,
        previewViewScaleType: String
    ): RectF {
        val displayRect = calculateCameraDisplayRect(
            previewViewWidth,
            previewViewHeight,
            imageAspectRatio,
            previewViewScaleType
        )

        Log.d(TAG, "Display Rect calculated: ${displayRect.toShortString()} (W:${displayRect.width()}, H:${displayRect.height()})")

        val frameWidth = displayRect.width() * FRAME_WIDTH_PERCENT
        val frameHeight = frameWidth * FRAME_HEIGHT_ASPECT_RATIO // Maintain desired frame aspect ratio

        val left = displayRect.centerX() - (frameWidth / 2f)
        val top = displayRect.centerY() - (frameHeight / 2f)
        val right = left + frameWidth
        val bottom = top + frameHeight

        val finalFrame = RectF(
            left.coerceAtLeast(displayRect.left),
            top.coerceAtLeast(displayRect.top),
            right.coerceAtMost(displayRect.right),
            bottom.coerceAtMost(displayRect.bottom)
        )

        Log.d(TAG, "Scan Frame (relative to visible feed) calculated: ${finalFrame.toShortString()}")
        return finalFrame
    }

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
                Log.d(TAG, "ScaleType: FILL_CENTER. Display Rect is full PreviewView.")
                RectF(0f, 0f, previewViewWidth, previewViewHeight)
            }
            "FIT_CENTER" -> {
                if (imageAspectRatio > previewAspectRatio) {
                    displayedImageWidth = previewViewWidth
                    displayedImageHeight = previewViewWidth / imageAspectRatio
                    Log.d(TAG, "ScaleType: FIT_CENTER (Pillarboxing). Calculated displayed: ${displayedImageWidth}x${displayedImageHeight}")
                } else {
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
    fun validateImageUri(uri: Uri): Boolean {
        return uri.scheme != null && (uri.scheme == "content" || uri.scheme == "file")
    }
}