package net.cattaka.android.cameraxmlkit

// Your IDE likely can auto-import these classes, but there are several
// different implementations so we list them here to disambiguate
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.TextureView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysisConfig
import androidx.camera.core.PreviewConfig
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import net.cattaka.android.cameraxmlkit.view.GraphicOverlay
import net.cattaka.android.cameraxmlkit.view.ObjectGraphic
import net.cattaka.android.cameraxmlkit.view.TextGraphic

// This is an arbitrary number we are using to keep tab of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts
private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: TextureView
    private lateinit var graphicOverlay: GraphicOverlay
    private var fitPreview: FitPreview? = null
    private var textAnalyzer = TextAnalyzer()
    private var objectAnalyzer = ObjectAnalyzer()
    private var analyzerUseCase: ImageAnalysis? = null

    companion object {
        val analyzerThread = HandlerThread(
            "CameraXAnalysis"
        ).apply { start() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.view_finder)
        graphicOverlay = findViewById(R.id.graphic_overlay)

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            fitPreview?.updateTransform()
        }

        textAnalyzer.liveData.observe(this, Observer {
            processVisionResult(it)
        })
        objectAnalyzer.liveData.observe(this, Observer {
            processVisionResult(it)
        })

        val onCheckedChangeListener = RadioGroup.OnCheckedChangeListener { group, checkedId ->
            graphicOverlay.clear()
            analyzerUseCase?.let {
                when (checkedId) {
                    R.id.mode_object -> {
                        it.analyzer = objectAnalyzer
                    }
                    R.id.mode_text -> {
                        it.analyzer = textAnalyzer
                    }
                }
            }
        }
        findViewById<RadioGroup>(R.id.mode_group).setOnCheckedChangeListener(onCheckedChangeListener)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
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
                    this, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private fun startCamera() {
//        val targetResolution = Size(1920, 1080)
        val targetResolution = Size(800, 600)
        val previewConfig = PreviewConfig.Builder()
            .setTargetResolution(targetResolution)
            .build()

        fitPreview = FitPreview(viewFinder, previewConfig)

        // Setup image analysis pipeline that computes average pixel luminance
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setCallbackHandler(Handler(analyzerThread.looper))
            setTargetResolution(targetResolution)
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        // Build the image analysis use case and instantiate our analyzer
        analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            analyzer = textAnalyzer
        }

        CameraX.bindToLifecycle(this, fitPreview, analyzerUseCase)
    }

    private fun processVisionResult(result: TextAnalyzer.TextAnalyzerResult) {
        val blocks = result.visionText.textBlocks
        if (blocks.isEmpty()) {
            return
        }

        graphicOverlay.clear()
        for (i in blocks.indices) {
            val lines = blocks[i].lines
            for (j in lines.indices) {
                val elements = lines[j].elements
                for (k in elements.indices) {
                    val textGraphic = TextGraphic(graphicOverlay, elements[k])
                    graphicOverlay.add(textGraphic)
                }
            }
        }

        val displayDegree = FitPreview.getDisplayDegree(viewFinder.display)
        val imageDegree = fitPreview?.rotationDegrees ?: 0
        val matrix = TextAnalyzer.calcFitMatrix(result, viewFinder, displayDegree - imageDegree)
        graphicOverlay.matrix = matrix
    }

    private fun processVisionResult(result: ObjectAnalyzer.ObjectAnalyzerResult) {
        val blocks = result.visionObject
        if (blocks.isEmpty()) {
            return
        }

        graphicOverlay.clear()
        for (block in blocks) {
            val textGraphic = ObjectGraphic(graphicOverlay, block)
            graphicOverlay.add(textGraphic)
        }

        val displayDegree = FitPreview.getDisplayDegree(viewFinder.display)
        val imageDegree = fitPreview?.rotationDegrees ?: 0
        val matrix = ObjectAnalyzer.calcFitMatrix(result, viewFinder, displayDegree - imageDegree)
        graphicOverlay.matrix = matrix
    }
}
