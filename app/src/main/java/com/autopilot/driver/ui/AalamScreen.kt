package com.autopilot.driver.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autopilot.driver.R

@Composable
fun AalamScreen(
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val ink = Color(0xFF0B161A)
    val teal = Color(0xFF78E6D0)
    var minimumPrice by remember { mutableStateOf("100") }
    var maximumPrice by remember { mutableStateOf("150") }
    var isRunning by remember { mutableStateOf(false) }
    var overlayEnabled by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }
    var captureGranted by remember { mutableStateOf(false) }
    var notificationsGranted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_autopilot),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(48.dp)
            )
            Column {
                Text("AALAM", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("AUTO CLICKER", color = teal, fontSize = 10.sp, letterSpacing = 1.6.sp)
            }
        }

        // Status Card
        StatusCard(isRunning)

        // Price Range Card
        PriceCard(
            minimum = minimumPrice,
            maximum = maximumPrice,
            onMinimumChanged = { minimumPrice = it },
            onMaximumChanged = { maximumPrice = it },
        )

        // Control Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (isRunning) onPause() else onStart()
                    isRunning = !isRunning
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = teal, contentColor = ink),
            ) { Text(if (isRunning) "PAUSE" else "START", fontWeight = FontWeight.Bold) }

            Button(
                onClick = { onStop(); isRunning = false },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE35D6A), contentColor = Color.White),
            ) { Text("STOP", fontWeight = FontWeight.Bold) }
        }

        // Permissions Card
        PermissionsCard(
            accessibilityEnabled = accessibilityEnabled,
            overlayEnabled = overlayEnabled,
            captureGranted = captureGranted,
            notificationsGranted = notificationsGranted,
            onAccessibilityClick = { accessibilityEnabled = true; onOpenAccessibility() },
            onOverlayClick = { overlayEnabled = true; onOpenOverlay() },
            onCaptureClick = { captureGranted = true },
            onNotificationsClick = { notificationsGranted = true; onRequestNotifications() },
        )

        // Floating Panel Toggle
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13262B)), shape = RoundedCornerShape(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Settings, contentDescription = null, tint = teal)
                    Column {
                        Text("FLOATING CONTROL PANEL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Show controls above other apps", color = Color(0xFF9EB3B6), fontSize = 12.sp)
                    }
                }
                Switch(checked = overlayEnabled, onCheckedChange = { overlayEnabled = it })
            }
        }

        // Info Text
        Text(
            "Aalam detects and taps ride accept buttons across all Indian languages. Enable Accessibility and Screen Capture permissions to start.",
            color = Color(0xFF789094),
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun StatusCard(running: Boolean) {
    val color = if (running) Color(0xFF78E6D0) else Color(0xFF9EB3B6)
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13262B)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("STATUS", color = Color(0xFF789094), fontSize = 11.sp, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                if (running) "RUNNING" else "READY",
                color = color,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (running) "Watching for ride offers..." else "Tap START to begin monitoring",
                color = Color(0xFF789094),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun PriceCard(
    minimum: String,
    maximum: String,
    onMinimumChanged: (String) -> Unit,
    onMaximumChanged: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF13262B)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("PRICE RANGE", color = Color(0xFF78E6D0), fontSize = 11.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minimum,
                    onValueChange = onMinimumChanged,
                    label = { Text("MIN PRICE") },
                    prefix = { Text("Rs ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = maximum,
                    onValueChange = onMaximumChanged,
                    label = { Text("MAX PRICE") },
                    prefix = { Text("Rs ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PermissionsCard(
    accessibilityEnabled: Boolean,
    overlayEnabled: Boolean,
    captureGranted: Boolean,
    notificationsGranted: Boolean,
    onAccessibilityClick: () -> Unit,
    onOverlayClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2024)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.BugReport, contentDescription = null, tint = Color(0xFF78E6D0), modifier = Modifier.size(18.dp))
                Text("PERMISSIONS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            PermissionRow("Accessibility Service", accessibilityEnabled, onAccessibilityClick)
            PermissionRow("Overlay Permission", overlayEnabled, onOverlayClick)
            PermissionRow("Screen Capture", captureGranted, onCaptureClick)
            PermissionRow("Notifications", notificationsGranted, onNotificationsClick)
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF789094), fontSize = 12.sp)
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (granted) Color(0xFF78E6D0) else Color(0xFF274248),
                contentColor = if (granted) Color(0xFF0B161A) else Color(0xFF78E6D0)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(if (granted) "GRANTED" else "ENABLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
