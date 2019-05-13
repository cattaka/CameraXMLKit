package net.cattaka.android.cameraxmlkit

import android.media.Image
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
    val liveData = MutableLiveData<FirebaseVisionText>()

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        val img = image.image ?: return
        runBlocking {
            val visionText = runTextRecognition(img, FirebaseVisionImageMetadata.ROTATION_90)
            liveData.postValue(visionText)
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
}
