package com.autopilot.driver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.autopilot.driver.automation.AalamAccessibilityService
import com.autopilot.driver.service.AalamScreenService
import com.autopilot.driver.service.FloatingPanelService
import com.autopilot.driver.storage.SettingsStore
import com.autopilot.driver.ui.AalamScreen
import com.autopilot.driver.ui.theme.AalamTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var settingsStore: SettingsStore
    private var receiverRegistered = false
    private val runtimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != "aalam.update") return
            if (intent.`package` != packageName && intent.getStringExtra("sender") != "internal") return
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            lifecycleScope.launch {
                settingsStore.setCaptureGranted(true)
                settingsStore.setAutopilotEnabled(true)
                val intent = Intent(this@MainActivity, AalamScreenService::class.java).apply {
                    putExtra(AalamScreenService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(AalamScreenService.EXTRA_RESULT_DATA, result.data)
                }
                startForegroundService(intent)
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    val fpIntent = Intent(this@MainActivity, FloatingPanelService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(fpIntent)
                    } else {
                        startService(fpIntent)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = SettingsStore(this)
        lifecycleScope.launch {
            if (!AalamScreenService.isRunning) {
                settingsStore.setCaptureGranted(false)
                settingsStore.setAutopilotEnabled(false)
            }
        }

        val filter = IntentFilter("aalam.update")
        ContextCompat.registerReceiver(this, runtimeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true

        setContent {
            AalamTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AalamScreen(
                        settingsStore = settingsStore,
                        onStart = { startAalam() },
                        onPause = { pauseAalam() },
                        onStop = { stopAalam() },
                        onOpenAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        onOpenOverlay = {
                            if (Build.VERSION.SDK_INT >= 23) {
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            }
                        },
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= 33) {
                                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 11)
                            }
                        },
                        onRequestCapture = { startAalam() },
                    )
                }
            }
        }
    }

    private fun startAalam() {
        if (!AalamAccessibilityService.isEnabled(this)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun pauseAalam() {
        lifecycleScope.launch {
            settingsStore.setAutopilotEnabled(false)
        }
        startService(Intent(this, AalamScreenService::class.java).setAction("com.autopilot.driver.action.PAUSE"))
    }

    private fun stopAalam() {
        lifecycleScope.launch {
            settingsStore.setAutopilotEnabled(false)
            settingsStore.setCaptureGranted(false)
        }
        stopService(Intent(this, AalamScreenService::class.java))
        stopService(Intent(this, FloatingPanelService::class.java))
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            runCatching { unregisterReceiver(runtimeReceiver) }
            receiverRegistered = false
        }
        super.onDestroy()
    }
}
