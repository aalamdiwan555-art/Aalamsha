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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.autopilot.driver.AalamLog
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class AalamScreenService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL = "aalam_running"
        private const val NOTIFICATION_ID = 41
        private const val INTERVAL_MS = 3500L
        private const val FAST_INTERVAL_MS = 1000L
        private const val IDLE_INTERVAL_MS = 5000L
        const val ACTION_PAUSE = "com.autopilot.driver.action.PAUSE"
        const val ACTION_RESUME = "com.autopilot.driver.action.RESUME"
        const val ACTION_STOP = "com.autopilot.driver.action.STOP"
        @Volatile var isRunning = false
        @Volatile var isPaused = false
    }

    private val tag = AalamLog.TAG
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val ocrInFlight = AtomicBoolean(false)
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val readerLock = Any()
    private val bitmapLock = Any()
    private var currentBitmap: Bitmap? = null
    private var ocrJob: Job? = null
    private var destroyed = false
    private var lastFrameHadHint = false
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var captureWidth = 0
    private var captureHeight = 0

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.w(tag, "MediaProjection stopped by system")
            projection = null
            handler.post { stopSelf() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        isPaused = false
        destroyed = false
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
        when (intent?.action) {
            ACTION_PAUSE -> {
                  isPaused = true
                  handler.removeCallbacks(captureLoop)
                  updateNotification(getString(com.autopilot.driver.R.string.status_paused))
                  sendBroadcast(
                      Intent("aalam.update")
                          .setPackage(packageName)
                          .putExtra("state", "PAUSED")
                          .putExtra("sender", "internal")
                  )
                  return START_STICKY
              }
            ACTION_RESUME -> {
                  isPaused = false
                  handler.removeCallbacks(captureLoop)
                  handler.post(captureLoop)
                  updateNotification(getString(com.autopilot.driver.R.string.status_watching))
                  sendBroadcast(
                      Intent("aalam.update")
                          .setPackage(packageName)
                          .putExtra("state", "RUNNING")
                          .putExtra("sender", "internal")
                  )
                  return START_STICKY
              }
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

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
            projection?.registerCallback(projectionCallback, handler)
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
            if (!isRunning || isPaused || destroyed) return
            if (projection == null) {
                Log.w(tag, "Projection lost, pausing capture loop")
                handler.postDelayed(this, IDLE_INTERVAL_MS)
                return
            }
            // Scan the currently visible app; the user explicitly enabled any-app mode.
            processLatestFrame()
            val nextInterval = if (lastFrameHadHint) FAST_INTERVAL_MS else INTERVAL_MS
            handler.postDelayed(this, nextInterval)
        }
    }

    private fun processLatestFrame() {
        if (!ocrInFlight.compareAndSet(false, true)) return
        val image = try {
            synchronized(readerLock) { reader?.acquireLatestImage() }
        } catch (error: IllegalStateException) {
            Log.w(tag, "ImageReader closed during frame acquisition", error)
            ocrInFlight.set(false)
            return
        }
        if (image == null) {
            ocrInFlight.set(false)
            return
        }
        val bitmap = try {
            imageToBitmap(image)
        } catch (error: Exception) {
            Log.e(tag, "Unable to convert captured frame", error)
            null
        } finally {
            image.close()
        }
        if (bitmap == null) {
            ocrInFlight.set(false)
            return
        }
        synchronized(bitmapLock) {
            if (destroyed) {
                bitmap.recycle()
                ocrInFlight.set(false)
                return
            }
            currentBitmap = bitmap
        }
        ocrJob = scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                }
                lastFrameHadHint = result.text.contains("ride", ignoreCase = true) ||
                    result.text.contains("new", ignoreCase = true) ||
                    result.text.contains("booking", ignoreCase = true)
                if (OcrKeywords.containsAccept(result.text)) {
                    val targetBounds = findAcceptBounds(result)
                    if (targetBounds != null) {
                        AalamAccessibilityService.requestAcceptClick(mapCaptureBoundsToScreen(targetBounds))
                    } else {
                        AalamAccessibilityService.requestAcceptClick()
                    }
                }
            } catch (error: Exception) {
                Log.e(tag, "OCR failed", error)
            } finally {
                synchronized(bitmapLock) {
                    if (currentBitmap === bitmap) currentBitmap = null
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
                ocrInFlight.set(false)
            }
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
        val source = if (paddedWidth == image.width) {
            paddedBitmap
        } else {
            val croppedBitmap = Bitmap.createBitmap(paddedBitmap, 0, 0, image.width, image.height)
            paddedBitmap.recycle()
            croppedBitmap
        }
        val maxDimension = 720
        if (source.width <= maxDimension && source.height <= maxDimension) return source
        val ratio = minOf(
            maxDimension.toFloat() / source.width,
            maxDimension.toFloat() / source.height,
        )
        val scaled = Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).toInt(),
            (source.height * ratio).toInt(),
            true,
        )
        source.recycle()
        return scaled
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
            val channel = NotificationChannel(
                CHANNEL,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_autopilot)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacksAndMessages(null)
        isRunning = false
        isPaused = false
        ocrInFlight.set(false)
        display?.release()
        synchronized(readerLock) {
            reader?.close()
            reader = null
        }
        projection?.unregisterCallback(projectionCallback)
        projection?.stop()

        // Cancel any pending OCR and wait for it to finish
        ocrJob?.cancel()
        runBlocking {
            try {
                ocrJob?.join()
            } catch (_: Exception) { }
        }

        synchronized(bitmapLock) {
            currentBitmap?.let { if (!it.isRecycled) it.recycle() }
            currentBitmap = null
        }
        recognizer.close()
        scope.cancel()

        sendBroadcast(
            Intent("aalam.update")
                .setPackage(packageName)
                .putExtra("state", "STOPPED")
                .putExtra("sender", "internal")
        )
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
