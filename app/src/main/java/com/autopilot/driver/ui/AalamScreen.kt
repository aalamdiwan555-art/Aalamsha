package com.autopilot.driver.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autopilot.driver.R
import com.autopilot.driver.automation.AalamAccessibilityService
import com.autopilot.driver.service.AalamScreenService
import com.autopilot.driver.storage.SettingsStore
import kotlinx.coroutines.delay

@Composable
fun AalamScreen(
    settingsStore: SettingsStore,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestCapture: () -> Unit,
) {
    val ink = Color(0xFF0B161A)
    val teal = Color(0xFF78E6D0)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val storedSettings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = SettingsStore.StoredSettings())
    var minimumPrice by remember { mutableStateOf("100") }
    var maximumPrice by remember { mutableStateOf("150") }
    var pendingPriceSave by remember { mutableStateOf(false) }
    var pricesHydrated by remember { mutableStateOf(false) }
    var isTransitioning by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(AalamScreenService.isRunning) }
    var overlayEnabled by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var accessibilityEnabled by remember { mutableStateOf(AalamAccessibilityService.isEnabled(context)) }
    var captureGranted by remember { mutableStateOf(AalamScreenService.isRunning) }
    var notificationsGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }

    LaunchedEffect(storedSettings.minimumPrice, storedSettings.maximumPrice) {
        if (!pendingPriceSave) {
            minimumPrice = storedSettings.minimumPrice.toString()
            maximumPrice = storedSettings.maximumPrice.toString()
            pricesHydrated = true
        }
    }
    LaunchedEffect(minimumPrice, maximumPrice, pricesHydrated) {
        if (!pricesHydrated) return@LaunchedEffect
        delay(400)
        val min = minimumPrice.toDoubleOrNull()
        val max = maximumPrice.toDoubleOrNull()
        if (min != null && max != null && min > 0 && max > 0 && min <= max) {
            settingsStore.savePriceRange(min, max)
            pendingPriceSave = false
        }
    }
    LaunchedEffect(isTransitioning) {
        if (isTransitioning) {
            delay(10_000)
            isTransitioning = false
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isTransitioning = false
                isRunning = AalamScreenService.isRunning
                captureGranted = AalamScreenService.isRunning
                overlayEnabled = Settings.canDrawOverlays(context)
                accessibilityEnabled = AalamAccessibilityService.isEnabled(context)
                notificationsGranted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val minValue = minimumPrice.toDoubleOrNull()
    val maxValue = maximumPrice.toDoubleOrNull()
    val validPriceRange = minValue != null && maxValue != null && minValue > 0 && maxValue > 0 && minValue <= maxValue

    Column(modifier = Modifier.fillMaxSize().background(ink).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painter = painterResource(R.drawable.ic_autopilot), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(48.dp))
            Column {
                Text(stringResource(R.string.app_title), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(stringResource(R.string.app_subtitle), color = teal, fontSize = 10.sp, letterSpacing = 1.6.sp)
            }
        }
        StatusCard(isRunning)
        PriceCard(minimum = minimumPrice, maximum = maximumPrice, onMinimumChanged = { pendingPriceSave = true; minimumPrice = it }, onMaximumChanged = { pendingPriceSave = true; maximumPrice = it })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { if (isTransitioning) return@Button; if (!isRunning && !validPriceRange) return@Button; isTransitioning = true; if (isRunning) { onPause() } else { onStart() } }, enabled = !isTransitioning, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = teal, contentColor = ink)) {
                Text(if (isRunning) stringResource(R.string.pause) else stringResource(R.string.start), fontWeight = FontWeight.Bold)
            }
            Button(onClick = { onStop(); isRunning = false }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE35D6A), contentColor = Color.White)) { Text(stringResource(R.string.stop), fontWeight = FontWeight.Bold) }
        }
        PermissionsCard(accessibilityEnabled, overlayEnabled, captureGranted, notificationsGranted, onOpenAccessibility, onOpenOverlay, onRequestCapture, onRequestNotifications)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable private fun StatusCard(running: Boolean) {
    val color = if (running) Color(0xFF78E6D0) else Color(0xFF9EB3B6)
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13262B)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(stringResource(R.string.status), color = Color(0xFF789094), fontSize = 11.sp, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(6.dp))
            Text(if (running) stringResource(R.string.status_running) else stringResource(R.string.status_ready), color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(if (running) stringResource(R.string.status_watching) else stringResource(R.string.status_tap_start), color = Color(0xFF789094), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable private fun PriceCard(minimum: String, maximum: String, onMinimumChanged: (String) -> Unit, onMaximumChanged: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13262B)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.price_range), color = Color(0xFF78E6D0), fontSize = 11.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = minimum, onValueChange = { onMinimumChanged(sanitizePriceInput(it)) }, label = { Text(stringResource(R.string.min_price)) }, prefix = { Text(stringResource(R.string.currency_prefix)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = maximum, onValueChange = { onMaximumChanged(sanitizePriceInput(it)) }, label = { Text(stringResource(R.string.max_price)) }, prefix = { Text(stringResource(R.string.currency_prefix)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun sanitizePriceInput(value: String): String {
    if (value.isEmpty()) return ""
    val filtered = value.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot < 0) filtered.trimStart('0').ifEmpty { "0" } else {
        val beforeDot = filtered.substring(0, firstDot).trimStart('0').ifEmpty { "0" }
        val afterDot = filtered.substring(firstDot + 1).replace(".", "")
        "$beforeDot.$afterDot"
    }
}

@Composable private fun PermissionsCard(accessibilityEnabled: Boolean, overlayEnabled: Boolean, captureGranted: Boolean, notificationsGranted: Boolean, onAccessibilityClick: () -> Unit, onOverlayClick: () -> Unit, onCaptureClick: () -> Unit, onNotificationsClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2024)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(androidx.compose.material.icons.Icons.Outlined.BugReport, contentDescription = null, tint = Color(0xFF78E6D0), modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.permissions), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            PermissionRow(stringResource(R.string.accessibility_service), accessibilityEnabled, onAccessibilityClick)
            PermissionRow(stringResource(R.string.overlay_permission), overlayEnabled, onOverlayClick)
            PermissionRow(stringResource(R.string.screen_capture), captureGranted, onCaptureClick)
            PermissionRow(stringResource(R.string.notifications), notificationsGranted, onNotificationsClick)
        }
    }
}

@Composable private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFF789094), fontSize = 12.sp)
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = if (granted) Color(0xFF78E6D0) else Color(0xFF274248), contentColor = if (granted) Color(0xFF0B161A) else Color(0xFF78E6D0)), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp)) {
            Text(if (granted) stringResource(R.string.granted) else stringResource(R.string.enable), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
