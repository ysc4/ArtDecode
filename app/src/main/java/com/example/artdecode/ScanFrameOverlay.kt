// ScanFrameOverlay.kt
package com.example.artdecode

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ScanFrameOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val overlayPaint = Paint().apply {
        color = Color.BLACK
        alpha = 150
    }

    private val framePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val cornerPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    private val frameRect = RectF()
    private val cornerLength = 40f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawScanFrame(canvas)
    }

    private fun drawScanFrame(canvas: Canvas) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        // Calculate frame dimensions (similar to artwork aspect ratio)
        val frameWidth = width * 0.75f
        val frameHeight = frameWidth * 1.2f

        val left = (width - frameWidth) / 2
        val top = (height - frameHeight) / 2
        val right = left + frameWidth
        val bottom = top + frameHeight

        frameRect.set(left, top, right, bottom)

        // Draw dark overlay outside the frame
        drawOverlay(canvas, frameRect)

        // Draw frame border
        canvas.drawRect(frameRect, framePaint)

        // Draw corner brackets
        drawCornerBrackets(canvas, frameRect)
    }

    private fun drawOverlay(canvas: Canvas, frameRect: RectF) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        // Top overlay
        canvas.drawRect(0f, 0f, width, frameRect.top, overlayPaint)

        // Bottom overlay
        canvas.drawRect(0f, frameRect.bottom, width, height, overlayPaint)

        // Left overlay
        canvas.drawRect(0f, frameRect.top, frameRect.left, frameRect.bottom, overlayPaint)

        // Right overlay
        canvas.drawRect(frameRect.right, frameRect.top, width, frameRect.bottom, overlayPaint)
    }

    private fun drawCornerBrackets(canvas: Canvas, rect: RectF) {
        // Top-left corner
        canvas.drawLine(rect.left, rect.top + cornerLength, rect.left, rect.top, cornerPaint)
        canvas.drawLine(rect.left, rect.top, rect.left + cornerLength, rect.top, cornerPaint)

        // Top-right corner
        canvas.drawLine(rect.right - cornerLength, rect.top, rect.right, rect.top, cornerPaint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + cornerLength, cornerPaint)

        // Bottom-left corner
        canvas.drawLine(rect.left, rect.bottom - cornerLength, rect.left, rect.bottom, cornerPaint)
        canvas.drawLine(rect.left, rect.bottom, rect.left + cornerLength, rect.bottom, cornerPaint)

        // Bottom-right corner
        canvas.drawLine(rect.right - cornerLength, rect.bottom, rect.right, rect.bottom, cornerPaint)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - cornerLength, cornerPaint)
    }
}