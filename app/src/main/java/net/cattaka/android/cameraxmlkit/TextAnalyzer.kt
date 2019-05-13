package net.cattaka.android.cameraxmlkit

import android.graphics.Matrix
import android.media.Image
import android.util.Size
import android.view.View
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class TextAnalyzer : ImageAnalysis.Analyzer {
    val liveData = MutableLiveData<TextAnalyzerResult>()
    var rotation = FirebaseVisionImageMetadata.ROTATION_0

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        val img = image.image ?: return
        runBlocking {
            val visionText = runTextRecognition(img, rotation)
            liveData.postValue(TextAnalyzerResult(rotation, visionText, Size(img.width, img.height)))
        }
    }

    private suspend fun runTextRecognition(src: Image, rotation: Int): FirebaseVisionText = suspendCoroutine { continuation ->
        val image = FirebaseVisionImage.fromMediaImage(src, rotation)
        val recognizer = FirebaseVision.getInstance()
                .onDeviceTextRecognizer
        recognizer.processImage(image)
                .addOnSuccessListener { texts -> continuation.resume(texts) }
                .addOnFailureListener { e -> continuation.resumeWithException(e) }
    }

    data class TextAnalyzerResult(
            val rotation: Int,
            val visionText: FirebaseVisionText,
            val imageSize: Size
    )

    companion object {
        fun calcFitMatrix(result: TextAnalyzerResult, targetView: View, displayDegree: Int): Matrix {
            val resultDegree = when (result.rotation) {
                FirebaseVisionImageMetadata.ROTATION_0 -> 0
                FirebaseVisionImageMetadata.ROTATION_90 -> 90
                FirebaseVisionImageMetadata.ROTATION_180 -> 180
                FirebaseVisionImageMetadata.ROTATION_270 -> 270
                else -> 0
            }

            val degree = displayDegree - resultDegree
            val imageSize = result.imageSize
            val matrix = Matrix()

            val oddRotate = (Math.abs(degree / 90) % 2 == 0)
            val w = (if (oddRotate) imageSize.height else imageSize.width).toFloat()
            val h = (if (oddRotate) imageSize.width else imageSize.height).toFloat()

            val sx = targetView.width.toFloat() / w
            val sy = targetView.height.toFloat() / h
            val scale = Math.max(sx, sy)

            matrix.postScale(1f / imageSize.width, 1f / imageSize.height)
            matrix.postTranslate(-0.5f, -0.5f)
            matrix.postRotate(-degree.toFloat())
            matrix.postScale(w, h)
            matrix.postScale(scale, scale)
            matrix.postTranslate(targetView.width / 2f, targetView.height / 2f)

            return matrix
        }
    }
}
