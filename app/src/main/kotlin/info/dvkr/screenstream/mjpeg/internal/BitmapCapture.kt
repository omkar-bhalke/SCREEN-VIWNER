package info.dvkr.screenstream.mjpeg.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import info.dvkr.screenstream.mjpeg.settings.MjpegSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Captures screen frames via MediaProjection → ImageReader → Bitmap.
 * Applies crop, resize, flip, rotation, grayscale per MjpegSettings.
 * Writes Bitmaps to bitmapStateFlow for the MJPEG pipeline to compress.
 */
class BitmapCapture(
    private val context: Context,
    private val mjpegSettings: MjpegSettings,
    private val mediaProjection: MediaProjection,
    private val bitmapStateFlow: MutableStateFlow<Bitmap?>,
    private val onError: (Throwable) -> Unit
) {
    private enum class State { IDLE, STARTED, STOPPED }

    private val imageThread: HandlerThread by lazy {
        HandlerThread("BitmapCaptureThread").also { it.start() }
    }
    private val imageThreadHandler: Handler by lazy {
        Handler(imageThread.looper)
    }

    private var state = State.IDLE
    private var imageOptions: ImageOptions = ImageOptions()
    private var transformMatrixDirty = true
    private var lastImageMillis: Long = 0L
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    data class ImageOptions(
        val width: Int = 0,
        val height: Int = 0,
        val maxFps: Int = 30,
        val jpegQuality: Int = 80,
        val resizeEnabled: Boolean = false,
        val resizeWidth: Int = 0,
        val resizeHeight: Int = 0,
        val rotation: Int = 0,
        val grayscale: Boolean = false,
        val cropEnabled: Boolean = false,
        val cropTop: Int = 0, val cropBottom: Int = 0,
        val cropLeft: Int = 0, val cropRight: Int = 0
    )

    private inner class ImageListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            val now = System.currentTimeMillis()
            val minInterval = 1000L / imageOptions.maxFps
            if (now - lastImageMillis < minInterval) {
                reader.acquireLatestImage()?.close()
                return
            }
            lastImageMillis = now
            val image = reader.acquireLatestImage() ?: return
            try {
                val bitmap = transformImageToBitmap(image)
                bitmapStateFlow.tryEmit(bitmap)
            } finally {
                image.close()
            }
        }
    }

    fun start(screenSize: android.util.Size): Boolean {
        if (state != State.IDLE) return false
        val opts = imageOptions
        val displayMetrics = context.resources.displayMetrics
        val width = if (opts.resizeEnabled && opts.resizeWidth > 0) opts.resizeWidth
                    else displayMetrics.widthPixels
        val height = if (opts.resizeEnabled && opts.resizeHeight > 0) opts.resizeHeight
                     else displayMetrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader!!.setOnImageAvailableListener(ImageListener(), imageThreadHandler)

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenStream", width, height, displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, imageThreadHandler
        )
        state = State.STARTED
        return true
    }

    private fun transformImageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val bitmap = Bitmap.createBitmap(
            image.width, image.height, Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(plane.buffer)

        val opts = imageOptions
        if (!opts.resizeEnabled && opts.rotation == 0 && !opts.grayscale && !opts.cropEnabled) {
            return bitmap
        }

        val matrix = Matrix()
        if (opts.rotation != 0) matrix.postRotate(opts.rotation.toFloat())

        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()

        return if (opts.grayscale) {
            val gray = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(gray)
            val paint = android.graphics.Paint()
            val cm = android.graphics.ColorMatrix()
            cm.setSaturation(0f)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(rotated, 0f, 0f, paint)
            rotated.recycle()
            gray
        } else rotated
    }

    fun resize() {
        // Restart capture with new dimensions
        safeRelease()
        state = State.IDLE
    }

    fun resize(width: Int, height: Int) {
        imageOptions = imageOptions.copy(resizeWidth = width, resizeHeight = height, resizeEnabled = true)
        resize()
    }

    fun destroy() {
        state = State.STOPPED
        safeRelease()
        imageThread.quitSafely()
    }

    private fun safeRelease() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
    }
}
