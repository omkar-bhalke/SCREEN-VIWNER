package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VoiceSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceManagementSheet(
    voiceSettings: VoiceSettings,
    onSaveSettings: (VoiceSettings) -> Unit,
    onTestVoice: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentPitch by remember { mutableFloatStateOf(voiceSettings.pitch) }
    var currentSpeed by remember { mutableFloatStateOf(voiceSettings.speechRate) }
    var currentContinuous by remember { mutableStateOf(voiceSettings.continuousMode) }
    var currentLanguage by remember { mutableStateOf(voiceSettings.language) }
    var currentTone by remember { mutableStateOf(voiceSettings.tone) }
    var currentVoiceModel by remember { mutableStateOf(voiceSettings.voiceName) }

    val languages = listOf("English", "Hindi", "Hinglish")
    val tones = listOf(
        "Charming & Witty" to "Playful banter, clever remarks, warm and charming",
        "Confident & Sharp" to "Clear, articulate, direct, and intellectually agile",
        "Empathetic & Supportive" to "Gentle, compassionate, warm, and comforting",
        "Playful & Energetic" to "Vibrant, cheerful, optimistic, and lively"
    )
    val voiceModels = listOf(
        "Kore (Dynamic Female)" to "Rich resonance, expressive human pacing",
        "Aoede (Melodic)" to "Softer, lyrical, elegant pronunciation",
        "Puck (Playful)" to "Fast, spirited, conversational warmth"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF070B14),
        scrimColor = Color.Black.copy(alpha = 0.85f),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0075FF), Color(0xFF00F0FF))
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "VOICE MANAGEMENT",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Acoustic modulation & voice persona",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x2E282835))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Hands-Free Continuous Voice-to-Voice Mode
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0D1728))
                    .border(1.dp, Color(0x3800F0FF), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x2B00F0FF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Continuous Voice Mode",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Hands-free continuous listening right after AHI speaks",
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Switch(
                        checked = currentContinuous,
                        onCheckedChange = {
                            currentContinuous = it
                            onSaveSettings(
                                voiceSettings.copy(
                                    pitch = currentPitch,
                                    speechRate = currentSpeed,
                                    continuousMode = it,
                                    language = currentLanguage,
                                    tone = currentTone,
                                    voiceName = currentVoiceModel
                                )
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0075FF),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("switch_continuous_voice")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Language & Accent
            Text(
                text = "PRIMARY CONVERSATION LANGUAGE",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                languages.forEach { lang ->
                    val isSelected = currentLanguage.equals(lang, ignoreCase = true)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF0F3260) else Color(0xFF0B1322))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF00F0FF) else Color(0x240075FF),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                currentLanguage = lang
                                onSaveSettings(
                                    voiceSettings.copy(
                                        pitch = currentPitch,
                                        speechRate = currentSpeed,
                                        continuousMode = currentContinuous,
                                        language = lang,
                                        tone = currentTone,
                                        voiceName = currentVoiceModel
                                    )
                                )
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = lang,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8)
                            )
                            Text(
                                text = when (lang) {
                                    "Hindi" -> "हिंदी बोलचाल"
                                    "Hinglish" -> "Natural Mix"
                                    else -> "Global / US"
                                },
                                fontSize = 10.sp,
                                color = if (isSelected) Color(0xFF00F0FF) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Section 3: Persona & Vocal Tone
            Text(
                text = "AI PERSONA & TONALITY",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tones.forEach { (toneTitle, toneDesc) ->
                    val isSelected = currentTone == toneTitle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF0F2646) else Color(0xFF0B1322))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF00F0FF) else Color(0x240075FF),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                currentTone = toneTitle
                                onSaveSettings(
                                    voiceSettings.copy(
                                        pitch = currentPitch,
                                        speechRate = currentSpeed,
                                        continuousMode = currentContinuous,
                                        language = currentLanguage,
                                        tone = toneTitle,
                                        voiceName = currentVoiceModel
                                    )
                                )
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = toneTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = toneDesc,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            if (isSelected) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00F0FF))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Section 4: Pitch & Cadence Modulation Sliders
            Text(
                text = "ACOUSTIC MODULATION",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Pitch Slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0B1322))
                    .border(1.dp, Color(0x240075FF), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vocal Pitch", fontSize = 13.5.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            text = String.format("%.2fx", currentPitch),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00F0FF)
                        )
                    }
                    Slider(
                        value = currentPitch,
                        onValueChange = { currentPitch = it },
                        onValueChangeFinished = {
                            onSaveSettings(
                                voiceSettings.copy(
                                    pitch = currentPitch,
                                    speechRate = currentSpeed,
                                    continuousMode = currentContinuous,
                                    language = currentLanguage,
                                    tone = currentTone,
                                    voiceName = currentVoiceModel
                                )
                            )
                        },
                        valueRange = 0.8f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00F0FF),
                            activeTrackColor = Color(0xFF0075FF),
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("slider_pitch")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Deeper (0.8x)", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        Text("Natural (1.15x)", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        Text("Crisp / High (1.4x)", fontSize = 10.5.sp, color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Speech Rate / Cadence Slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0B1322))
                    .border(1.dp, Color(0x240075FF), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Speech Cadence", fontSize = 13.5.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            text = String.format("%.2fx", currentSpeed),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Slider(
                        value = currentSpeed,
                        onValueChange = { currentSpeed = it },
                        onValueChangeFinished = {
                            onSaveSettings(
                                voiceSettings.copy(
                                    pitch = currentPitch,
                                    speechRate = currentSpeed,
                                    continuousMode = currentContinuous,
                                    language = currentLanguage,
                                    tone = currentTone,
                                    voiceName = currentVoiceModel
                                )
                            )
                        },
                        valueRange = 0.8f..1.35f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF0075FF),
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("slider_speech_rate")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Deliberate (0.8x)", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        Text("Natural (1.05x)", fontSize = 10.5.sp, color = Color(0xFF64748B))
                        Text("Fast / Energetic (1.35x)", fontSize = 10.5.sp, color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons: Preview Voice & Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Test Voice Button
                Button(
                    onClick = {
                        val testSentence = when (currentLanguage) {
                            "Hindi" -> "नमस्ते! मैं अही हूँ, आपकी पर्सनल एआई साथी। सब कुछ एकदम सही तरह से काम कर रहा है!"
                            "Hinglish" -> "Hey there! Main AHI hoon, aapki smart voice assistant. Sab set hai, boliye kya madat karun?"
                            else -> "Hello there! I'm AHI, your personal voice companion. Everything is tuned and ready to chat!"
                        }
                        onTestVoice(testSentence)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0052D4), Color(0xFF4364F7), Color(0xFF6FB1FC))
                            )
                        )
                        .testTag("button_test_voice")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TEST VOICE",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Reset Button
                Button(
                    onClick = {
                        val defaults = VoiceSettings()
                        currentPitch = defaults.pitch
                        currentSpeed = defaults.speechRate
                        currentContinuous = defaults.continuousMode
                        currentLanguage = defaults.language
                        currentTone = defaults.tone
                        currentVoiceModel = defaults.voiceName
                        onSaveSettings(defaults)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("button_reset_voice")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
