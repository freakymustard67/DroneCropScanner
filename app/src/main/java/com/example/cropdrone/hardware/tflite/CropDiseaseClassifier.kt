package com.example.cropdrone.hardware.tflite

import android.content.Context
import android.graphics.Bitmap
import com.example.cropdrone.domain.model.Anomaly
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Pass 1 — On-device TFLite classifier.
 *
 * Loads `crop_disease_mobilenet.tflite` from `assets/`, runs MobileNet V3 inference
 * on each video frame (~30 FPS target), and flags frames with non-healthy label
 * and confidence above threshold for Pass 2 cloud analysis.
 *
 * Output classes (4): healthy, disease, pest, water_stress
 */
@Singleton
class CropDiseaseClassifier @Inject constructor(
    private val context: Context,
) {
    companion object {
        private const val MODEL_FILENAME = "crop_disease_mobilenet.tflite"
        private const val INPUT_IMAGE_SIZE = 224
        private const val PIXEL_SIZE = 3 // RGB
        // TFLite input is normalized to [-1, 1]
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f

        // MobileNet V3 labels (order matters — matches training)
        val LABELS = listOf("healthy", "disease", "pest", "water_stress")

        const val FLAG_THRESHOLD = 0.6f
    }

    private var interpreter: Interpreter? = null
    private val inputImageArray: Array<Array<Array<FloatArray>>> =
        Array(INPUT_IMAGE_SIZE) {
            Array(INPUT_IMAGE_SIZE) {
                Array(1) { FloatArray(PIXEL_SIZE) }
            }
        }

    fun initialize() {
        if (interpreter != null) return
        val assetManager = context.assets
        val modelBuffer = assetManager.openFd(MODEL_FILENAME).let { fd ->
            val inputStream = fd.createInputStream()
            val bytes = ByteArray(fd.length.toInt())
            inputStream.read(bytes)
            inputStream.close()
            ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes)
        }
        val options = Interpreter.Options()
        // Use GPU delegate if compatible
        if (CompatibilityList().isDelegateSupportedOnThisDevice) {
            options.addDelegate(GpuDelegate())
        } else {
            options.numThreads = 4
        }
        interpreter = Interpreter(modelBuffer, options)
    }

    data class ClassificationResult(
        val label: String,
        val confidence: Float,
        val flagged: Boolean,
    )

    fun classify(bitmap: Bitmap): ClassificationResult {
        val interp = interpreter ?: error("Classifier not initialized — call initialize() first")
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, false)
        convertBitmapToInputBuffer(scaled)

        val outputArray = Array(1) { FloatArray(LABELS.size) }
        interp.run(inputImageArray, outputArray)

        val scores = outputArray[0]
        // Find top class
        var maxIdx = 0
        var maxScore = scores[0]
        for (i in 1 until scores.size) {
            if (scores[i] > maxScore) { maxScore = scores[i]; maxIdx = i }
        }
        val label = LABELS[maxIdx]
        val flagged = label != "healthy" && maxScore >= FLAG_THRESHOLD
        return ClassificationResult(label = label, confidence = maxScore, flagged = flagged)
    }

    /**
     * Process a stream of frames, emitting anomalies for flagged frames.
     * Caller is responsible for recording the drone's GPS at the moment of each flag.
     */
    fun processFrame(bitmap: Bitmap, currentLat: Double, currentLng: Double, frameImagePath: String): Anomaly? {
        val result = classify(bitmap)
        if (!result.flagged) return null
        return Anomaly(
            id = "anom_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}",
            lat = currentLat,
            lng = currentLng,
            imagePath = frameImagePath,
            thumbnailPath = frameImagePath,
            pass1Confidence = result.confidence.toDouble(),
            pass1Label = result.label,
            detectedAt = System.currentTimeMillis(),
        )
    }

    private fun convertBitmapToInputBuffer(bitmap: Bitmap) {
        val pixels = IntArray(INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_IMAGE_SIZE, 0, 0, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE)
        for (i in 0 until INPUT_IMAGE_SIZE) {
            for (j in 0 until INPUT_IMAGE_SIZE) {
                val pixel = pixels[i * INPUT_IMAGE_SIZE + j]
                inputImageArray[i][j][0][0] = ((pixel shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD
                inputImageArray[i][j][0][1] = ((pixel shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD
                inputImageArray[i][j][0][2] = ((pixel and 0xFF) - IMAGE_MEAN) / IMAGE_STD
            }
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
