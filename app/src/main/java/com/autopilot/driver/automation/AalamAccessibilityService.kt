package com.autopilot.driver.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.autopilot.driver.OcrKeywords
import com.autopilot.driver.AalamLog
import kotlin.math.roundToInt

class AalamAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = AalamLog.TAG
        private const val CLICK_COOLDOWN_MS = 500L
        private const val STROKE_MS = 100L
        @Volatile var instance: AalamAccessibilityService? = null
            private set
        @Volatile var foregroundPackage: String = ""

        val RIDE_PACKAGES = setOf(
            "com.rapido.rider",
            "com.olacabs.oladriver",
            "com.ubercab.driver",
            "com.ubercab",
            "com.ubercab.eats",
            "com.dunzo.user",
            "com.bigbasket.deliveryapp",
            "com.swiggy.deliveryapp",
            "com.zomato.delivery",
            "com.application.zomato",
            "com.zepto.rider",
            "com.blinkit.delivery",
            "com.ninjacart",
            "com.shadowfax",
            "com.loadshare",
            "com.porter.customerapp",
            "com.urbanclap.customer",
            "com.sulekha",
            "com.housejoy.consumer",
            "com.jugnoo.driver",
            "com.merucab.driver",
            "com.gojek.driver",
            "com.grab.driver",
            "com.ola.driver",
            "com.uber.driver",
            "com.lyft.driver",
            "com.didiglobal.driver",
            "com.careem.driver",
            "com.taxify.driver",
            "com.bolt.driver"
        )

        fun isEnabled(context: android.content.Context): Boolean {
            val am = context.getSystemService(ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
            return am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?.any { it.resolveInfo.serviceInfo.packageName == context.packageName } == true
        }

        fun requestAcceptClick() {
            instance?.requestClick(null)
        }

        fun requestAcceptClick(bounds: Rect) {
            instance?.requestClick(Rect(bounds))
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastClickAt = 0L
    private var gestureInFlight = false
    @Volatile private var destroyed = false
    private var screenWidth = 0
    private var screenHeight = 0

    override fun onServiceConnected() {
        destroyed = false
        instance = this
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        foregroundPackage = event?.packageName?.toString().orEmpty()
    }

    private fun requestClick(bounds: Rect?) {
        if (destroyed) {
            Log.w(TAG, "Ignoring click request after service destruction")
            return
        }
        mainHandler.post {
            if (destroyed) return@post
            if (bounds == null || !clickAt(bounds)) clickAccept()
        }
    }

    private fun clickAt(bounds: Rect): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickAt < CLICK_COOLDOWN_MS || gestureInFlight) {
            Log.d(TAG, "Gesture skipped: cooldown or another gesture is active")
            return false
        }
        if (foregroundPackage !in RIDE_PACKAGES) {
            Log.d(TAG, "Gesture skipped: unsupported package=$foregroundPackage")
            return false
        }
        if (bounds.isEmpty) {
            Log.d(TAG, "Gesture skipped: empty bounds")
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.d(TAG, "Gesture unavailable before API 24")
            return false
        }

        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val path = Path().apply {
            moveTo(centerX, centerY)
            lineTo(centerX + 1f, centerY + 1f)
        }

        if (centerX < 0 || centerY < 0 || centerX > screenWidth || centerY > screenHeight) {
            Log.w(TAG, "Gesture skipped: center outside screen")
            return false
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, STROKE_MS))
            .build()

        gestureInFlight = true
        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    gestureInFlight = false
                    lastClickAt = System.currentTimeMillis()
                    Log.i(TAG, "Screen gesture completed at ($centerX, $centerY)")
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    gestureInFlight = false
                    Log.w(TAG, "Screen gesture cancelled; trying node fallback")
                    mainHandler.post { clickAccept() }
                }
            },
            mainHandler,
        )
        if (accepted) {
            Log.d(TAG, "Screen gesture accepted for dispatch")
            return true
        }
        gestureInFlight = false
        Log.w(TAG, "Screen gesture rejected by AccessibilityService")
        return false
    }

    private fun clickAccept(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickAt < CLICK_COOLDOWN_MS || gestureInFlight) {
            Log.d(TAG, "Node click skipped: cooldown or gesture active")
            return false
        }
        if (foregroundPackage !in RIDE_PACKAGES) {
            Log.d(TAG, "Node click skipped: unsupported package=$foregroundPackage")
            return false
        }
        val root = rootInActiveWindow ?: run {
            Log.w(TAG, "Node click skipped: no active window")
            return false
        }
        val candidate = try {
            findCandidate(root)
        } finally {
            root.recycle()
        }
        if (candidate != null) {
            val clicked = performClick(candidate)
            if (clicked && !gestureInFlight) {
                lastClickAt = System.currentTimeMillis()
            }
            Log.i(TAG, "Node click result=$clicked")
            candidate.recycle()
            return clicked
        } else {
            Log.d(TAG, "Node click found no accept candidate")
            return false
        }
    }

    private fun findCandidate(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val label = listOf(node.text, node.contentDescription)
            .filterNotNull().joinToString(" ")
        if (OcrKeywords.containsAccept(label) && node.isVisibleToUser && node.isEnabled) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findCandidate(child)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true

        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                parent.recycle()
                return true
            }
            val next = parent.parent
            parent.recycle()
            parent = next
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.isEmpty) return false

        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        val path = Path().apply {
            moveTo(cx, cy)
            lineTo(cx + 1f, cy + 1f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, STROKE_MS))
            .build()

        gestureInFlight = true
        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    gestureInFlight = false
                    lastClickAt = System.currentTimeMillis()
                    Log.i(TAG, "Node bounds gesture completed")
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    gestureInFlight = false
                    Log.w(TAG, "Node bounds gesture cancelled")
                }
            },
            mainHandler,
        )
        if (!accepted) gestureInFlight = false
        return accepted
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        destroyed = true
        mainHandler.removeCallbacksAndMessages(null)
        gestureInFlight = false
        if (instance === this) instance = null
        foregroundPackage = ""
        super.onDestroy()
    }
}
