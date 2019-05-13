package net.cattaka.android.cameraxmlkit

// Your IDE likely can auto-import these classes, but there are several
// different implementations so we list them here to disambiguate
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.HandlerThread
import android.util.Size
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraX
import androidx.camera.core.PreviewConfig
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// This is an arbitrary number we are using to keep tab of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts
private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: TextureView
    private var fitPreview: FitPreview? = null
    private var rotationDegrees = 0
    private var textureSize = Size(0, 0)

    companion object {
        val analyzerThread = HandlerThread(
                "LuminosityAnalysis").apply { start() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.view_finder)

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            fitPreview?.updateTransform()
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                            this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun startCamera() {
//        val previewConfig = PreviewConfig.Builder().apply {
//            setTargetAspectRatio(Rational(1, 1))
//            setTargetResolution(Size(640, 640))
//        }.build()
        val previewConfig = PreviewConfig.Builder().build()

        fitPreview = FitPreview(viewFinder, previewConfig)

//        val imageCaptureConfig = ImageCaptureConfig.Builder()
//                .apply {
//                    setTargetAspectRatio(Rational(1, 1))
//                    // We don't set a resolution for image capture; instead, we
//                    // select a capture mode which will infer the appropriate
//                    // resolution based on aspect ration and requested mode
//                    setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
//                }.build()

//        val imageCapture = ImageCapture(imageCaptureConfig)
//        findViewById<ImageButton>(R.id.capture_button).setOnClickListener {
//            val file = File(externalMediaDirs.first(),
//                    "${System.currentTimeMillis()}.jpg")
//            imageCapture.takePicture(file,
//                    object : ImageCapture.OnImageSavedListener {
//                        override fun onError(error: ImageCapture.UseCaseError,
//                                             message: String, exc: Throwable?) {
//                            val msg = "Photo capture failed: $message"
//                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                            Log.e("CameraXApp", msg)
//                            exc?.printStackTrace()
//                        }
//
//                        override fun onImageSaved(file: File) {
//                            val msg = "Photo capture succeeded: ${file.absolutePath}"
//                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                            Log.d("CameraXApp", msg)
//                        }
//                    })
//        }

        // Setup image analysis pipeline that computes average pixel luminance
//        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
//            // Use a worker thread for image analysis to prevent glitches
//            setCallbackHandler(Handler(analyzerThread.looper))
//            // In our analysis, we care more about the latest image than
//            // analyzing *every* image
//            setImageReaderMode(
//                    ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
//        }.build()

        // Build the image analysis use case and instantiate our analyzer
//        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
//            analyzer = LuminosityAnalyzer()
//        }
//
//        CameraX.bindToLifecycle(
//                this, preview, imageCapture, analyzerUseCase)

        CameraX.bindToLifecycle(
                this, fitPreview)
    }
}
