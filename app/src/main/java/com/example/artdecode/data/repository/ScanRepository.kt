package com.example.artdecode.data.repository

import android.graphics.RectF
import android.net.Uri
import com.example.artdecode.data.model.CameraConfiguration

class ScanRepository {

    fun getCameraConfiguration(): CameraConfiguration {
        return CameraConfiguration()
    }

    fun validateImageUri(uri: Uri?): Boolean {
        return uri != null
    }

    fun getFrameDimensions(viewWidth: Float, viewHeight: Float): RectF {
        val frameWidth = viewWidth * 0.75f
        val frameHeight = frameWidth * 1.2f
        val left = (viewWidth - frameWidth) / 2
        val top = (viewHeight - frameHeight) / 2
        val right = left + frameWidth
        val bottom = top + frameHeight
        return RectF(left, top, right, bottom)
    }
}