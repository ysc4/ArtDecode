package com.example.artdecode.data.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import androidx.core.graphics.scale

class ArtStyleClassifier(private val context: Context) {

    companion object {
        private const val TAG = "ArtStyleClassifier"
        private const val MODEL_FILENAME = "model.tflite" // Ensure this is your model file name
        private const val INPUT_SIZE = 224 // Your model's expected input width/height
        private const val NUM_CLASSES = 16 // CORRECTED: From 15 to 16, as you have Class 0 to Class 15

        private val ART_STYLES = arrayOf(
            "Art Nouveau Modern",       // Class 0
            "Baroque",                  // Class 1
            "Color Field Painting",     // Class 2
            "Cubism",                   // Class 3
            "Expressionism",            // Class 4
            "Fauvism",                  // Class 5
            "Impressionism",            // Class 6
            "Minimalism",               // Class 7
            "Naive Art Primitivism",    // Class 8
            "Pop Art",                  // Class 9
            "Realism",                  // Class 10
            "Renaissance",              // Class 11
            "Rococo",                   // Class 12
            "Romanticism",              // Class 13
            "Symbolism",                // Class 14
            "Ukiyo-e"                   // Class 15
        )
    }

    private var interpreter: Interpreter? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            interpreter = Interpreter(modelBuffer)
            Log.d(TAG, "Model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILENAME)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classifyImage(imageUri: Uri): ClassificationResult? {
        return try {
            val bitmap = loadAndPreprocessImage(imageUri)
            val inputBuffer = convertBitmapToByteBuffer(bitmap)
            val outputArray = Array(1) { FloatArray(NUM_CLASSES) }

            interpreter?.run(inputBuffer, outputArray)

            val probabilities = outputArray[0]
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1 // Use -1 as default

            if (maxIndex != -1) { // Only proceed if a valid index was found
                val confidence = probabilities[maxIndex]
                val artStyle = ART_STYLES.getOrElse(maxIndex) { "Unknown" }

                Log.d(TAG, "Classification result: $artStyle with confidence: $confidence")
                Log.d(TAG, "All probabilities: ${probabilities.joinToString()}")
                ClassificationResult(artStyle, confidence)
            } else {
                Log.e(TAG, "No valid classification index found.")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during classification: ${e.message}", e)
            null
        }
    }

    private fun loadAndPreprocessImage(imageUri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw IllegalArgumentException("Could not open input stream for URI: $imageUri")
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw IllegalStateException("Could not decode bitmap from stream for URI: $imageUri")
        inputStream.close()

        // Resize the image. .scale extension function handles scaling correctly.
        val scaledBitmap = originalBitmap.scale(INPUT_SIZE, INPUT_SIZE)
        if (scaledBitmap != originalBitmap) {
            originalBitmap.recycle() // Recycle original if a new one was created by scale()
        }
        Log.d(TAG, "Image loaded and preprocessed to ${scaledBitmap.width}x${scaledBitmap.height}")
        return scaledBitmap
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3) // 4 bytes per float
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                byteBuffer.putFloat(((value shr 16) and 0xFF) / 255.0f) // Red
                byteBuffer.putFloat(((value shr 8) and 0xFF) / 255.0f)  // Green
                byteBuffer.putFloat((value and 0xFF) / 255.0f)        // Blue
            }
        }
        byteBuffer.rewind()
        return byteBuffer
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        Log.d(TAG, "Interpreter closed.")
    }
}