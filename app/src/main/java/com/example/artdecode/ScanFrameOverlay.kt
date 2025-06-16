package com.example.artdecode

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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

    fun updateFrame(rect: RectF) {
        frameRect.set(rect)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (frameRect.isEmpty) {
            calculateDefaultFrame(canvas.width.toFloat(), canvas.height.toFloat())
        }
        drawScanFrame(canvas)
    }

    private fun calculateDefaultFrame(width: Float, height: Float) {
        val frameWidth = width * 0.75f
        val frameHeight = frameWidth * 1.2f
        val left = (width - frameWidth) / 2
        val top = (height - frameHeight) / 2
        val right = left + frameWidth
        val bottom = top + frameHeight
        frameRect.set(left, top, right, bottom)
    }

    private fun drawScanFrame(canvas: Canvas) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()

        drawOverlay(canvas, width, height)
        canvas.drawRect(frameRect, framePaint)
        drawCornerBrackets(canvas)
    }

    private fun drawOverlay(canvas: Canvas, width: Float, height: Float) {
        canvas.drawRect(0f, 0f, width, frameRect.top, overlayPaint)
        canvas.drawRect(0f, frameRect.bottom, width, height, overlayPaint)
        canvas.drawRect(0f, frameRect.top, frameRect.left, frameRect.bottom, overlayPaint)
        canvas.drawRect(frameRect.right, frameRect.top, width, frameRect.bottom, overlayPaint)
    }

    private fun drawCornerBrackets(canvas: Canvas) {
        canvas.drawLine(
            frameRect.left, frameRect.top + cornerLength,
            frameRect.left, frameRect.top, cornerPaint
        )
        canvas.drawLine(
            frameRect.left, frameRect.top,
            frameRect.left + cornerLength, frameRect.top, cornerPaint
        )
        canvas.drawLine(
            frameRect.right - cornerLength, frameRect.top,
            frameRect.right, frameRect.top, cornerPaint
        )
        canvas.drawLine(
            frameRect.right, frameRect.top,
            frameRect.right, frameRect.top + cornerLength, cornerPaint
        )
        canvas.drawLine(
            frameRect.left, frameRect.bottom - cornerLength,
            frameRect.left, frameRect.bottom, cornerPaint
        )
        canvas.drawLine(
            frameRect.left, frameRect.bottom,
            frameRect.left + cornerLength, frameRect.bottom, cornerPaint
        )
        canvas.drawLine(
            frameRect.right - cornerLength, frameRect.bottom,
            frameRect.right, frameRect.bottom, cornerPaint
        )
        canvas.drawLine(
            frameRect.right, frameRect.bottom,
            frameRect.right, frameRect.bottom - cornerLength, cornerPaint
        )
    }
}