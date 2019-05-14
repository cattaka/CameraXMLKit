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
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ObjectAnalyzer : ImageAnalysis.Analyzer {
    val liveData = MutableLiveData<ObjectAnalyzerResult>()
    var rotation = FirebaseVisionImageMetadata.ROTATION_0

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        val img = image.image ?: return
        runBlocking {
            val visionObject = runObjectDetection(img)
            liveData.postValue(ObjectAnalyzerResult(visionObject, Size(img.width, img.height)))
        }
    }

    private suspend fun runObjectDetection(src: Image): List<FirebaseVisionObject> = suspendCoroutine { continuation ->
        // Step 1: create MLKit's VisionImage object
        val image = FirebaseVisionImage.fromMediaImage(src, rotation)

        // Step 2: acquire detector object
        val options = FirebaseVisionObjectDetectorOptions.Builder()
            .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        val detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)

        // Step 3: feed given image to detector and setup callback
        detector.processImage(image)
            .addOnSuccessListener { obj ->
                continuation.resume(obj)
            }
            .addOnFailureListener { e -> continuation.resumeWithException(e) }
    }

    data class ObjectAnalyzerResult(
        val visionObject: List<FirebaseVisionObject>,
        val imageSize: Size
    )

    companion object {
        fun calcFitMatrix(result: ObjectAnalyzerResult, targetView: View, displayDegree: Int): Matrix {
            val degree = displayDegree
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
