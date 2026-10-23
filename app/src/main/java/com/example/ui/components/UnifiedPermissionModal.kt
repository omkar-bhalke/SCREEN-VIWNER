package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalWifiConnectedNoInternet4
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.permission.UnifiedReadinessState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedPermissionModal(
    readinessState: UnifiedReadinessState,
    hasMicPermission: Boolean,
    isInternetConnected: Boolean,
    onRequestMicPermission: () -> Unit,
    onRetryConnection: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF070B14),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag("unified_permission_modal")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x2200F0FF))
                            .border(1.dp, Color(0x6600F0FF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Unified Access Flow",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Microphone & Internet access required for AHI voice",
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("unified_modal_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Microphone Access Status Card
            UnifiedStatusCard(
                title = "Microphone Access (RECORD_AUDIO)",
                description = if (hasMicPermission) {
                    "Microphone hardware access is granted. Real-time AudioRecord streaming is active."
                } else {
                    "Grant microphone permission so AHI can hear your voice and transcribe audio streams."
                },
                icon = Icons.Default.Mic,
                isReady = hasMicPermission,
                actionLabel = "Grant Microphone",
                onAction = onRequestMicPermission,
                modifier = Modifier.testTag("mic_status_card")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Internet Connectivity Status Card
            UnifiedStatusCard(
                title = "Internet Connectivity (INTERNET)",
                description = if (isInternetConnected) {
                    "Connected to the network. Gemini API neural models and Live voice ready."
                } else {
                    "No active internet connection detected. AHI requires online access for cloud intelligence."
                },
                icon = if (isInternetConnected) Icons.Default.Wifi else Icons.Default.SignalWifiConnectedNoInternet4,
                isReady = isInternetConnected,
                actionLabel = "Open Network Settings",
                onAction = {
                    try {
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.testTag("internet_status_card")
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Actions row: Retry & Continue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRetryConnection,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00F0FF)),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(12.dp))
                        .testTag("retry_connection_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Check Again", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (hasMicPermission && isInternetConnected) {
                            onDismiss()
                        } else if (!hasMicPermission) {
                            onRequestMicPermission()
                        } else {
                            try {
                                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasMicPermission && isInternetConnected) Color(0xFF10B981) else Color(0xFF0075FF)
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(46.dp)
                        .testTag("unified_primary_action_button")
                ) {
                    Text(
                        text = when {
                            hasMicPermission && isInternetConnected -> "Continue with AHI"
                            !hasMicPermission -> "Allow Microphone"
                            else -> "Configure Wi-Fi"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UnifiedStatusCard(
    title: String,
    description: String,
    icon: ImageVector,
    isReady: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D1424))
            .border(
                1.dp,
                if (isReady) Color(0x3334D399) else Color(0x33F87171),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isReady) Color(0x2234D399) else Color(0x22F87171))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isReady) Color(0xFF34D399) else Color(0xFFF87171),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isReady) Color(0x2234D399) else Color(0x22F87171))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isReady) "READY" else "ACTION NEEDED",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) Color(0xFF34D399) else Color(0xFFF87171)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF94A3B8)
                )

                if (!isReady) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
