package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LiveState

@Composable
fun InputCapsule(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onToggleVoice: () -> Unit,
    liveState: LiveState,
    modifier: Modifier = Modifier
) {
    val isListening = liveState == LiveState.LISTENING

    val infiniteTransition = rememberInfiniteTransition(label = "CapsuleGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveformPulse"
    )

    // Glass-like blue pill gradient styling from Screenshot 1
    val capsuleBackground = Brush.horizontalGradient(
        colors = listOf(
            Color(0x4010325C), // Translucent blue
            Color(0x60184A88),
            Color(0x4010325C)
        )
    )

    val capsuleBorder = Brush.horizontalGradient(
        colors = listOf(
            Color(0x4D38BDF8),
            Color(0x9938BDF8).copy(alpha = if (isListening) glowAlpha else 0.5f),
            Color(0x4D38BDF8)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(
                elevation = if (isListening) 16.dp else 8.dp,
                shape = CircleShape,
                spotColor = Color(0xFF0284C7).copy(alpha = if (isListening) 0.6f else 0.25f),
                ambientColor = Color(0xFF0369A1).copy(alpha = 0.2f)
            )
            .clip(CircleShape)
            .background(capsuleBackground)
            .border(width = 1.0.dp, brush = capsuleBorder, shape = CircleShape)
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("input_capsule")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            // Text input area: "Ask anything..."
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = if (isListening) "Listening to you..." else "Ask anything...",
                        style = TextStyle(
                            color = if (isListening) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                BasicTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(Color(0xFF38BDF8)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_text_input")
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button: Send if text entered, Waveform icon if empty
            val hasText = inputText.isNotBlank()

            AnimatedContent(
                targetState = hasText,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "CapsuleActionButton"
            ) { canSend ->
                if (canSend) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0075FF))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onSendMessage(inputText)
                            }
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                } else {
                    // Acoustic Waveform Bars matching Screenshot 1
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onToggleVoice()
                            }
                            .testTag("mic_button")
                    ) {
                        Canvas(modifier = Modifier.size(22.dp)) {
                            val barColor = if (isListening) Color(0xFF00F0FF) else Color(0xFF7DD3FC)
                            val barWidth = 2.5.dp.toPx()
                            val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                            val heights = if (isListening) {
                                listOf(0.4f * wavePhase, 0.9f * wavePhase, 1.0f * (2f - wavePhase), 0.6f * wavePhase)
                            } else {
                                listOf(0.35f, 0.75f, 1.0f, 0.55f)
                            }

                            val spacing = (size.width - (4 * barWidth)) / 3f

                            for (i in 0 until 4) {
                                val x = i * (barWidth + spacing)
                                val h = (size.height * heights[i].coerceIn(0.2f, 1.0f))
                                val y = (size.height - h) / 2f

                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, h),
                                    cornerRadius = cornerRadius
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
