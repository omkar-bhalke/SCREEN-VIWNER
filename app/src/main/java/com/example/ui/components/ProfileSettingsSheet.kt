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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AhiEmotion
import com.example.model.UserProfile
import com.example.model.VoiceSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsSheet(
    profile: UserProfile,
    voiceSettings: VoiceSettings,
    onSaveProfile: (String, String, String, AhiEmotion) -> Unit,
    onUpdateVoice: (Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(profile.userName) }
    var selectedLanguage by remember { mutableStateOf(profile.preferredLanguage) }
    var selectedEmotion by remember { mutableStateOf(profile.currentEmotion) }

    var pitch by remember { mutableFloatStateOf(voiceSettings.pitch) }
    var speed by remember { mutableFloatStateOf(voiceSettings.speechRate) }

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
                .testTag("profile_settings_sheet")
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x220075FF))
                            .border(1.dp, Color(0x660075FF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Companion & Identity Settings",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Personalize greeting, language & emotional tone",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // User Name field (reflects in "Hello, [User Name]")
            Text(
                text = "YOUR NAME",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00F0FF),
                    unfocusedBorderColor = Color(0x330075FF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_name_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preferred Language selection (English, Hindi, Hinglish)
            Text(
                text = "COMMUNICATION LANGUAGE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("English", "Hinglish", "Hindi").forEach { lang ->
                    val isSelected = selectedLanguage.equals(lang, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF0075FF) else Color(0xFF131D31))
                            .border(1.dp, if (isSelected) Color(0xFF00F0FF) else Color(0x330075FF), RoundedCornerShape(12.dp))
                            .clickable { selectedLanguage = lang }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = lang,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emotional State Engine
            Text(
                text = "EMOTIONAL EXPRESSION STATE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AhiEmotion.values()) { emo ->
                    val isSelected = selectedEmotion == emo
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) emo.primaryColor.copy(alpha = 0.25f) else Color(0xFF131D31))
                            .border(1.dp, if (isSelected) emo.primaryColor else Color(0x330075FF), RoundedCornerShape(12.dp))
                            .clickable { selectedEmotion = emo }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = emo.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voice Pitch and Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Vocal Pitch (${String.format("%.2f", pitch)}x)",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Slider(
                value = pitch,
                onValueChange = {
                    pitch = it
                    onUpdateVoice(pitch, speed)
                },
                valueRange = 0.9f..1.4f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00F0FF),
                    activeTrackColor = Color(0xFF00F0FF),
                    inactiveTrackColor = Color(0xFF131D31)
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    onSaveProfile(name.ifBlank { "Alex" }, selectedLanguage, profile.preferredStyle, selectedEmotion)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075FF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_profile_button")
            ) {
                Text(
                    text = "Save Identity & Preferences",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
