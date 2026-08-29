package com.autopilot.driver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import com.autopilot.driver.R
import com.autopilot.driver.automation.AalamAccessibilityService
import com.autopilot.driver.OcrKeywords
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class AalamScreenService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL = "aalam_running"
        private const val NOTIFICATION_ID = 41
        private const val INTERVAL_MS = 1500L
        @Volatile var isRunning = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val ocrInFlight = AtomicBoolean(false)
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var captureWidth = 0
    private var captureHeight = 0

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_autopilot)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Watching for ride offers")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (data == null || resultCode == 0) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (projection == null) {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            startCapture()
        }
        return START_STICKY
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        val width = screenWidth.coerceAtMost(1440)
        val height = screenHeight.coerceAtMost(2560)
        captureWidth = width
        captureHeight = height

        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        display = projection?.createVirtualDisplay(
            "AalamScreenReader", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, handler
        )
        handler.post(captureLoop)
    }

    private val captureLoop = object : Runnable {
        override fun run() {
            if (isRunning && AalamAccessibilityService.foregroundPackage in setOf(
                "com.rapido.rider", "com.olacabs.oladriver",
                "com.ubercab.driver", "com.ubercab"
            )) {
                processLatestFrame()
            }
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    private fun processLatestFrame() {
        if (!ocrInFlight.compareAndSet(false, true)) return
        val image = reader?.acquireLatestImage()
        if (image == null) {
            ocrInFlight.set(false)
            return
        }
        try {
            val bitmap = imageToBitmap(image)
            image.close()
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                    }
                    if (OcrKeywords.containsAccept(result.text)) {
                        val targetBounds = findAcceptBounds(result)
                        if (targetBounds != null) {
                            val screenBounds = mapCaptureBoundsToScreen(targetBounds)
                            AalamAccessibilityService.requestAcceptClick(screenBounds)
                        } else {
                            AalamAccessibilityService.requestAcceptClick()
                        }
                    }
                } finally {
                    bitmap.recycle()
                    ocrInFlight.set(false)
                }
            }
        } catch (_: Exception) {
            image.close()
            ocrInFlight.set(false)
        }
    }

    private fun imageToBitmap(image: android.media.Image): Bitmap {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val paddedWidth = image.width + rowPadding / pixelStride
        val paddedBitmap = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
        paddedBitmap.copyPixelsFromBuffer(buffer)
        return if (paddedWidth == image.width) {
            paddedBitmap
        } else {
            val croppedBitmap = Bitmap.createBitmap(paddedBitmap, 0, 0, image.width, image.height)
            paddedBitmap.recycle()
            croppedBitmap
        }
    }

    private fun findAcceptBounds(result: com.google.mlkit.vision.text.Text): Rect? {
        val matches = mutableListOf<Rect>()
        for (block in result.textBlocks) {
            for (line in block.lines) {
                if (OcrKeywords.containsAccept(line.text)) {
                    line.boundingBox?.let { matches.add(it) }
                }
            }
            if (OcrKeywords.containsAccept(block.text)) {
                block.boundingBox?.let { matches.add(it) }
            }
        }
        return matches.minByOrNull { it.width() * it.height() }
    }

    private fun mapCaptureBoundsToScreen(bounds: Rect): Rect {
        if (captureWidth <= 0 || captureHeight <= 0 || screenWidth <= 0 || screenHeight <= 0) {
            return Rect(bounds)
        }
        val scaleX = screenWidth.toFloat() / captureWidth
        val scaleY = screenHeight.toFloat() / captureHeight
        val left = (bounds.left * scaleX).toInt().coerceIn(0, screenWidth)
        val top = (bounds.top * scaleY).toInt().coerceIn(0, screenHeight)
        val right = (bounds.right * scaleX).toInt().coerceIn(0, screenWidth)
        val bottom = (bounds.bottom * scaleY).toInt().coerceIn(0, screenHeight)
        return Rect(left, top, right, bottom)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        isRunning = false
        ocrInFlight.set(false)
        display?.release()
        reader?.close()
        projection?.stop()
        recognizer.close()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
