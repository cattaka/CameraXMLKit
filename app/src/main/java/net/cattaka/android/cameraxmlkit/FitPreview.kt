package net.cattaka.android.cameraxmlkit

import android.graphics.Matrix
import android.util.Size
import android.view.*
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig

class FitPreview(
        val textureView: TextureView,
        previewConfig: PreviewConfig
) : Preview(previewConfig) {
    private var rotationDegrees = 0
    private var textureSize = Size(0, 0)

    init {
        setOnPreviewOutputUpdateListener {
            // Need to re-add view to refresh surface in Android Q Beta 3(bug?)
            (textureView.parent as ViewGroup).apply {
                removeView(textureView)
                addView(textureView, 0)
            }

            rotationDegrees = it.rotationDegrees
            textureSize = it.textureSize

            textureView.surfaceTexture = it.surfaceTexture
            updateTransform()
        }
    }

    fun updateTransform() {
        textureView.setTransform(calcFitMatrix(textureSize, textureView))
    }

    companion object {
        fun getDisplayDegree(display: Display?): Int {
            return when (display?.rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }
        }

        fun calcFitMatrix(textureSize: Size, targetView: View): Matrix {
            val matrix = Matrix()

            val displayDegree = getDisplayDegree(targetView.display)
            val oddRotate = (Math.abs(displayDegree / 90) % 2 == 0)
            val w = (if (oddRotate) textureSize.height else textureSize.width).toFloat()
            val h = (if (oddRotate) textureSize.width else textureSize.height).toFloat()

            val sx = targetView.width.toFloat() / w
            val sy = targetView.height.toFloat() / h
            val scale = Math.max(sx, sy)

            matrix.postScale(1f / targetView.width, 1f / targetView.height)
            matrix.postTranslate(-0.5f, -0.5f)
            matrix.postRotate(-displayDegree.toFloat())
            matrix.postScale(w, h)
            matrix.postScale(scale, scale)
            matrix.postTranslate(targetView.width / 2f, targetView.height / 2f)

            return matrix
        }

    }
}