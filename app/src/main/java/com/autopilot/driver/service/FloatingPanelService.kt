package com.autopilot.driver.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.autopilot.driver.AalamLog
import com.autopilot.driver.R
import kotlin.math.abs
import kotlin.math.roundToInt

class FloatingPanelService : Service() {
    private companion object { const val TAG = AalamLog.TAG }

    private var windowManager: WindowManager? = null
    private var panel: DraggablePanelLayout? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var isServiceRunning = false
    private var isServicePaused = false
    private var controlsContainer: LinearLayout? = null
    private var pauseButton: ImageButton? = null
    private var resumeButton: ImageButton? = null
    private var stopButton: ImageButton? = null

    private val runtimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != "aalam.update") return
            when (intent.getStringExtra("state")) {
                "RUNNING" -> {
                    isServiceRunning = true
                    isServicePaused = false
                    updateButtonVisibility()
                }
                "PAUSED" -> {
                    isServiceRunning = true
                    isServicePaused = true
                    updateButtonVisibility()
                }
                "STOPPED", "ERROR" -> stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val channelId = "aalam_floating"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "Floating Controls", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_autopilot)
            .setContentTitle("Control Panel Active")
            .setContentText("Floating controls are showing")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(42, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(42, notif)
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager ?: run {
            stopSelf()
            return
        }
        isServiceRunning = AalamScreenService.isRunning
        isServicePaused = AalamScreenService.isPaused
        registerRuntimeReceiver()
        buildPanel()
    }

    private fun buildPanel() {
        if (panel != null) return

        val root = DraggablePanelLayout(this) { dx, dy -> movePanel(dx, dy) }.apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 16)
            background = roundedBackground(Color.rgb(12, 29, 34), 24f)
        }
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        controlsContainer = controls

        pauseButton = iconButton(android.R.drawable.ic_media_pause, "Pause") {
            sendAction(AalamScreenService.ACTION_PAUSE)
        }
        resumeButton = iconButton(android.R.drawable.ic_media_play, "Resume") {
            sendAction(AalamScreenService.ACTION_RESUME)
        }
        stopButton = iconButton(android.R.drawable.ic_menu_close_clear_cancel, "Stop") {
            sendAction(AalamScreenService.ACTION_STOP)
        }

        root.addView(controls)
        val type = if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 18
            y = 120
        }
        panelParams = params
        panel = root
        updateButtonVisibility()
        runCatching { windowManager?.addView(root, params) }
            .onFailure {
                panel = null
                panelParams = null
                controlsContainer = null
                stopSelf()
            }
    }

    private fun updateButtonVisibility() {
        val controls = controlsContainer ?: return
        controls.removeAllViews()
        val buttonParams = LinearLayout.LayoutParams(56, 56)
        val spacedButtonParams = LinearLayout.LayoutParams(56, 56).apply { topMargin = 8 }

        if (isServiceRunning && !isServicePaused) {
            pauseButton?.let { controls.addView(it, buttonParams) }
        } else {
            resumeButton?.let { controls.addView(it, buttonParams) }
        }
        stopButton?.let { controls.addView(it, spacedButtonParams) }
    }

    private fun movePanel(dx: Float, dy: Float) {
        val root = panel ?: return
        val params = panelParams ?: return
        val metrics = displayMetrics()
        val maxX = (metrics.widthPixels - root.width).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - root.height).coerceAtLeast(0)
        params.x = (params.x - dx.roundToInt()).coerceIn(0, maxX)
        params.y = (params.y + dy.roundToInt()).coerceIn(0, maxY)
        runCatching { windowManager?.updateViewLayout(root, params) }
    }

    private fun displayMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.getRealMetrics(metrics)
        return metrics
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        panel?.post {
            val root = panel ?: return@post
            val params = panelParams ?: return@post
            val metrics = displayMetrics()
            params.x = params.x.coerceIn(0, (metrics.widthPixels - root.width).coerceAtLeast(0))
            params.y = params.y.coerceIn(0, (metrics.heightPixels - root.height).coerceAtLeast(0))
            runCatching { windowManager?.updateViewLayout(root, params) }
        }
    }

    private fun iconButton(icon: Int, desc: String, action: () -> Unit) = ImageButton(this).apply {
        setImageResource(icon)
        contentDescription = desc
        setColorFilter(Color.rgb(120, 230, 208))
        setBackgroundColor(Color.TRANSPARENT)
        setPadding(10, 10, 10, 10)
        setOnClickListener { action() }
    }

    private fun roundedBackground(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        setStroke(1, Color.rgb(39, 66, 72))
    }

    private fun sendAction(action: String) {
        startService(Intent(this, AalamScreenService::class.java).setAction(action))
    }

    private fun registerRuntimeReceiver() {
        val filter = IntentFilter("aalam.update")
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.registerReceiver(this, runtimeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(runtimeReceiver, filter)
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(runtimeReceiver) }
        panel?.let { runCatching { windowManager?.removeView(it) } }
        panel = null
        panelParams = null
        controlsContainer = null
        windowManager = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (panel == null) buildPanel()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private class DraggablePanelLayout(
        context: Context,
        private val onDrag: (dx: Float, dy: Float) -> Unit,
    ) : LinearLayout(context) {
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var lastX = 0f
        private var lastY = 0f
        private var dragging = false

        override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    lastX = event.rawX
                    lastY = event.rawY
                    dragging = false
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragging && (abs(event.rawX - downX) > touchSlop || abs(event.rawY - downY) > touchSlop)) {
                        dragging = true
                        return true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
            }
            return dragging
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    lastX = event.rawX
                    lastY = event.rawY
                    dragging = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    onDrag(event.rawX - lastX, event.rawY - lastY)
                    lastX = event.rawX
                    lastY = event.rawY
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    return true
                }
            }
            return true
        }
    }
}
