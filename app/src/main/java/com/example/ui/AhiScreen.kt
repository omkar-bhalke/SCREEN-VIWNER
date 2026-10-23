package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AhiViewModel
import com.example.model.LiveState
import com.example.ui.components.FlowingRibbonOrb
import com.example.ui.components.InputCapsule
import com.example.ui.components.MemorySheet
import com.example.ui.components.PermissionsSheet
import com.example.ui.components.ProfileSettingsSheet
import com.example.ui.components.UnifiedPermissionModal
import com.example.ui.components.VoiceManagementSheet
import com.example.ui.screens.ApiSetupScreen

@Composable
fun AhiScreen(
    viewModel: AhiViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showMemorySheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var showVoiceManagementSheet by remember { mutableStateOf(false) }

    val readinessState by viewModel.unifiedPermissionManager.readinessState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setMicPermissionGranted(isGranted)
        if (isGranted && uiState.isInternetConnected) {
            viewModel.dismissUnifiedPermissionModal()
            viewModel.startListening()
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setMicPermissionGranted(hasPermission)
    }

    // SCREEN 1: API SETUP POPUP (Shows on first open or when requested)
    // Resilient check: never repeatedly pops up once key is configured
    val isKeyConfigured = uiState.hasSavedApiKey || viewModel.keystoreManager.isConfigured()
    if (!isKeyConfigured || uiState.showApiSetupDialog) {
        ApiSetupScreen(
            onKeySaved = { key ->
                viewModel.saveApiKeyFromSetup(key)
            },
            modifier = modifier
        )
        return
    }

    // SCREEN 2: MAIN ASSISTANT (Pixel-perfect matching Screenshot 1)
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = Color(0xFF000000), // Pure Black Background
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF000000))
        ) {
            // Radiant Electric Blue Ambient Glow rising up from bottom (from Screenshot 1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x18023E8A),
                                Color(0x400077B6),
                                Color(0x850096C7),
                                Color(0xC00077B6)
                            )
                        )
                    )
            )

            // Top Control Bar (matching Screenshot 1 circular translucent buttons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left circular close button (from Screenshot 1)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x38282835))
                        .clickable {
                            // Reset conversation or clear session
                            viewModel.clearChatHistory()
                        }
                        .testTag("top_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close session",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Top-Right circular overflow menu button (from Screenshot 1)
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x38282835))
                            .clickable { showMenu = true }
                            .testTag("top_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(Color(0xFF0D1424))
                            .border(1.dp, Color(0x330075FF), RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Voice & Persona Hub", color = Color.White, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showMenu = false
                                showVoiceManagementSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Android Permissions Hub", color = Color.White, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showMenu = false
                                showPermissionsSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Memory Vault (${memories.size})", color = Color.White, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showMenu = false
                                showMemorySheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2DD4BF), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Profile Settings", color = Color.White, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showMenu = false
                                showProfileSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Gemini API Engine", color = Color.White, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showMenu = false
                                viewModel.openApiSetup()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Clear History", color = Color(0xFFF43F5E), fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showMenu = false
                                viewModel.clearChatHistory()
                            }
                        )
                    }
                }
            }

            // Central Vertical Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                if (!uiState.isInternetConnected) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22F87171))
                            .border(1.dp, Color(0x66F87171), RoundedCornerShape(12.dp))
                            .clickable { viewModel.showUnifiedPermissionModal() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("offline_warning_pill")
                    ) {
                        Text(
                            text = "Offline • Internet required for Gemini AI (Tap to fix)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Upper-Middle Content: Luminous Blue 3D Ribbon Orb, Greeting, Heading
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Central 3D Flowing Ribbon Orb (Screenshot 1)
                    FlowingRibbonOrb(
                        liveState = uiState.liveState,
                        amplitude = uiState.audioAmplitude,
                        emotion = uiState.currentEmotion,
                        onClick = {
                            if (!uiState.hasMicPermission || !uiState.isInternetConnected) {
                                viewModel.showUnifiedPermissionModal()
                            } else {
                                viewModel.onOrbClicked()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 2. Greeting: "Hello Robin" / "Hello <userName>"
                    Text(
                        text = "Hello ${uiState.userProfile.userName}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF94A3B8), // Muted grey
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("greeting_text")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Heading: "How can I help\nyou today?" (Bold white, 2 lines from Screenshot 1)
                    Text(
                        text = "How can I help\nyou today?",
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("main_heading_text")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Live Speech Transcription / Subtitle Area
                    AnimatedVisibility(
                        visible = uiState.userSpeechLive.isNotBlank() || uiState.ahiSpokenText.isNotBlank(),
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 })
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                        ) {
                            if (uiState.userSpeechLive.isNotBlank() && uiState.liveState == LiveState.LISTENING) {
                                Text(
                                    text = "\"${uiState.userSpeechLive}\"",
                                    fontSize = 15.sp,
                                    color = Color(0xFF00F0FF),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (uiState.ahiSpokenText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = uiState.ahiSpokenText,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    color = Color(0xFFE2E8F0),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.testTag("ahi_speech_response")
                                )
                            }
                        }
                    }

                    // Memory saved confirmation toast
                    AnimatedVisibility(
                        visible = uiState.memorySavedNotice != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        uiState.memorySavedNotice?.let { notice ->
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0A2548))
                                    .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = notice,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF00F0FF)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Content: Translucent Blue Input Capsule ("Ask anything..." + Waveform)
                InputCapsule(
                    inputText = uiState.chatInputText,
                    onInputTextChange = { viewModel.onInputTextChange(it) },
                    onSendMessage = { viewModel.sendMessage(it) },
                    onToggleVoice = {
                        if (!uiState.hasMicPermission || !uiState.isInternetConnected) {
                            viewModel.showUnifiedPermissionModal()
                        } else {
                            viewModel.toggleVoiceListening()
                        }
                    },
                    liveState = uiState.liveState,
                    modifier = Modifier.padding(bottom = 28.dp)
                )
            }
        }
    }

    // Memory Vault Sheet
    if (showMemorySheet) {
        MemorySheet(
            memories = memories,
            onAddMemory = { cat, title, content ->
                viewModel.addManualMemory(cat, title, content)
            },
            onDeleteMemory = { id ->
                viewModel.deleteMemory(id)
            },
            onClearChatHistory = {
                viewModel.clearChatHistory()
            },
            onDismiss = { showMemorySheet = false }
        )
    }

    // Profile & Preferences Settings Sheet
    if (showProfileSheet) {
        ProfileSettingsSheet(
            profile = uiState.userProfile,
            voiceSettings = uiState.voiceSettings,
            onSaveProfile = { name, lang, style, emotion ->
                viewModel.updateProfile(name, lang, style, emotion)
            },
            onUpdateVoice = { pitch, rate ->
                viewModel.updateVoicePitchAndRate(pitch, rate)
            },
            onDismiss = { showProfileSheet = false }
        )
    }

    // Voice & Persona Hub Sheet
    if (showVoiceManagementSheet) {
        VoiceManagementSheet(
            voiceSettings = uiState.voiceSettings,
            onSaveSettings = { settings ->
                viewModel.updateVoiceSettings(settings)
            },
            onTestVoice = { sampleText ->
                viewModel.testVoicePreview(sampleText)
            },
            onDismiss = { showVoiceManagementSheet = false }
        )
    }

    // Android Permissions Center Sheet
    if (showPermissionsSheet) {
        PermissionsSheet(
            onDismiss = {
                showPermissionsSheet = false
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                viewModel.setMicPermissionGranted(hasPermission)
            }
        )
    }

    // Unified Permission & Internet Access Flow Modal
    if (uiState.showUnifiedPermissionModal) {
        UnifiedPermissionModal(
            readinessState = readinessState,
            hasMicPermission = uiState.hasMicPermission,
            isInternetConnected = uiState.isInternetConnected,
            onRequestMicPermission = {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onRetryConnection = {
                viewModel.retryConnectionAndPermissions()
            },
            onDismiss = {
                viewModel.dismissUnifiedPermissionModal()
            }
        )
    }
}
