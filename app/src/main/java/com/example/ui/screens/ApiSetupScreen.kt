package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.GeminiApiTester
import com.example.gemini.TestResult
import kotlinx.coroutines.launch

@Composable
fun ApiSetupScreen(
    onKeySaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apiKeyText by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testFeedback by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isHelpExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0F1D), // Dark navy gradient top
                        Color(0xFF04060B), // Pure dark bottom
                        Color(0xFF020306)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main Outer Card (28dp radius, glass surface, subtle border)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp), spotColor = Color(0x330055FF))
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0E1422))
                .border(width = 1.dp, color = Color(0x33283955), shape = RoundedCornerShape(28.dp))
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .testTag("api_setup_card")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Icon: Rounded square icon box (Indigo -> Teal gradient) with Key icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4F46E5), // Indigo
                                    Color(0xFF0EA5E9), // Sky
                                    Color(0xFF14B8A6)  // Teal
                                )
                            )
                        )
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0xFF0EA5E9))
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Setup Key",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title: "SET UP AHI" (bold, uppercase, letter-spaced)
                Text(
                    text = "SET UP AHI",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "Add the API key required to activate AHI's existing intelligence and voice. You only need to complete this once.",
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Inner Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0A0F1A))
                        .border(width = 1.dp, color = Color(0xFF2B384E), shape = RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header row: "GOOGLE GEMINI API" & "REQUIRED" badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GOOGLE GEMINI API",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = Color(0xFF93C5FD) // Light indigo
                            )

                            // "REQUIRED" outlined yellow badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(width = 1.dp, color = Color(0x99FACC15), shape = RoundedCornerShape(12.dp))
                                    .background(Color(0x1AFACC15))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "REQUIRED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = Color(0xFFFACC15)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Description
                        Text(
                            text = "Required for AHI's intelligence, conversation, memory extraction, live voice, and Android command understanding.",
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password-style input field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF070A12))
                                .border(width = 1.dp, color = Color(0xFF1E293B), shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    if (apiKeyText.isEmpty()) {
                                        Text(
                                            text = "Enter your Gemini API key",
                                            fontSize = 14.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    BasicTextField(
                                        value = apiKeyText,
                                        onValueChange = {
                                            apiKeyText = it
                                            testFeedback = null
                                            isError = false
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        cursorBrush = SolidColor(Color(0xFF38BDF8)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("api_key_input")
                                    )
                                }

                                IconButton(
                                    onClick = { isPasswordVisible = !isPasswordVisible },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle key visibility",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3 Buttons Row: PASTE, TEST API, GET KEY
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. PASTE Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF161F30))
                                    .border(1.dp, Color(0xFF24334C), RoundedCornerShape(10.dp))
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                        if (!clip.isNullOrBlank()) {
                                            apiKeyText = clip.trim()
                                            testFeedback = null
                                            isError = false
                                        }
                                    }
                                    .testTag("paste_key_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PASTE",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }

                            // 2. TEST API Button
                            val canTest = apiKeyText.isNotBlank() && !isTesting
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (canTest) Color(0xFF0D232C) else Color(0xFF0A151C))
                                    .border(1.dp, if (canTest) Color(0xFF134E5E) else Color(0x33134E5E), RoundedCornerShape(10.dp))
                                    .clickable(enabled = canTest) {
                                        scope.launch {
                                            isTesting = true
                                            testFeedback = "Validating with Gemini..."
                                            isError = false
                                            val result = GeminiApiTester.testKey(apiKeyText)
                                            isTesting = false
                                            when (result) {
                                                is TestResult.Success -> {
                                                    testFeedback = "✓ API key is active and verified!"
                                                    isError = false
                                                }
                                                is TestResult.Error -> {
                                                    testFeedback = result.message
                                                    isError = true
                                                }
                                            }
                                        }
                                    }
                                    .testTag("test_api_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isTesting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF2DD4BF)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = if (canTest) Color(0xFF2DD4BF) else Color(0xFF64748B),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TEST API",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                        color = if (canTest) Color(0xFF2DD4BF) else Color(0xFF64748B)
                                    )
                                }
                            }

                            // 3. GET KEY Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E1B4B))
                                    .border(1.dp, Color(0xFF312E81), RoundedCornerShape(10.dp))
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    }
                                    .testTag("get_key_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = Color(0xFF818CF8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "GET KEY",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFFA5B4FC)
                                    )
                                }
                            }
                        }

                        // Feedback message under buttons (success / error)
                        if (testFeedback != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = testFeedback!!,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = if (isError) Color(0xFFF87171) else Color(0xFF34D399),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // "▶ HOW TO GET A KEY" Expandable Section
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { isHelpExpanded = !isHelpExpanded }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isHelpExpanded) "▼ HOW TO GET A KEY" else "▶ HOW TO GET A KEY",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        AnimatedVisibility(
                            visible = isHelpExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text("1. Tap 'GET KEY' or visit aistudio.google.com/apikey", fontSize = 11.5.sp, color = Color(0xFFCBD5E1), lineHeight = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("2. Sign in with Google and tap 'Create API Key'", fontSize = 11.5.sp, color = Color(0xFFCBD5E1), lineHeight = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("3. Copy your key and tap 'PASTE' above", fontSize = 11.5.sp, color = Color(0xFFCBD5E1), lineHeight = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Security Note with Shield Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF14B8A6), // Teal shield
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "The saved key is encrypted by Android Keystore and is never returned to this interface, written to localStorage, bundled in the APK, or included in diagnostics.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Gradient Button: "TEST, SAVE & CONTINUE"
                val canSubmit = apiKeyText.isNotBlank() && !isTesting
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (canSubmit) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6), // Deep blue / indigo
                                        Color(0xFF0EA5E9), // Cyan
                                        Color(0xFF0D9488)  // Teal
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0x333B82F6),
                                        Color(0x330D9488)
                                    )
                                )
                            }
                        )
                        .clickable(enabled = canSubmit) {
                            scope.launch {
                                isTesting = true
                                testFeedback = "Validating key before saving..."
                                isError = false
                                val result = GeminiApiTester.testKey(apiKeyText)
                                isTesting = false
                                when (result) {
                                    is TestResult.Success -> {
                                        onKeySaved(apiKeyText.trim())
                                    }
                                    is TestResult.Error -> {
                                        testFeedback = result.message
                                        isError = true
                                    }
                                }
                            }
                        }
                        .testTag("test_save_continue_button")
                ) {
                    if (isTesting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.5.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "VERIFYING KEY...",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        }
                    } else {
                        Text(
                            text = "TEST, SAVE & CONTINUE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (canSubmit) Color.White else Color(0x66FFFFFF)
                        )
                    }
                }
            }
        }
    }
}
