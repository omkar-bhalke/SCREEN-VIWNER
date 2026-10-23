package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AhiEmotion
import com.example.model.LiveState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FlowingRibbonOrb(
    liveState: LiveState,
    amplitude: Float,
    emotion: AhiEmotion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RibbonMotion")

    // Base rotation & flow phases
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (liveState) {
                    LiveState.THINKING -> 3200
                    LiveState.LISTENING -> 5500
                    LiveState.SPEAKING -> 4200
                    LiveState.IDLE -> 9500
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "FlowPhase"
    )

    val secondaryPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (liveState) {
                    LiveState.THINKING -> 2400
                    LiveState.LISTENING -> 4800
                    LiveState.SPEAKING -> 3600
                    LiveState.IDLE -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "SecondaryPhase"
    )

    // Breathing pulse
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (liveState == LiveState.SPEAKING) 1200 else 2800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathPulse"
    )

    // Palette: Electric blue, cyan, deep navy, subtle violet highlights
    val electricBlue = Color(0xFF0075FF)
    val electricCyan = emotion.primaryColor
    val deepNavy = Color(0xFF03112E)
    val subtleViolet = emotion.secondaryColor
    val coreGlowColor = Color(0xFF00A2FF)

    val reactiveAmp = if (liveState == LiveState.LISTENING || liveState == LiveState.SPEAKING) {
        amplitude.coerceIn(0f, 1f)
    } else {
        0.05f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(240.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("flowing_ribbon_orb")
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2.3f) * breathScale * (1f + reactiveAmp * 0.18f)

            // 1. Soft Blue Ambient Glow Background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreGlowColor.copy(alpha = 0.32f + reactiveAmp * 0.25f),
                        electricBlue.copy(alpha = 0.18f + reactiveAmp * 0.15f),
                        subtleViolet.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.55f
                ),
                radius = baseRadius * 1.55f,
                center = center
            )

            // Inner dark cosmic core for deep volumetric contrast
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF000818).copy(alpha = 0.95f),
                        deepNavy.copy(alpha = 0.85f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.95f
                ),
                radius = baseRadius * 0.95f,
                center = center
            )

            // 2. Parametric 3D Energy Ribbons (Möbius / Trefoil knot projected ribbons)
            // Ribbon strands with varying phase and depth
            val strandConfigs = listOf(
                Triple(0.0f, 4.0.dp.toPx(), electricCyan),
                Triple(PI.toFloat() * 0.65f, 3.2.dp.toPx(), electricBlue),
                Triple(PI.toFloat() * 1.35f, 2.5.dp.toPx(), subtleViolet),
                Triple(PI.toFloat() * 1.85f, 1.8.dp.toPx(), Color(0xFF67E8F9))
            )

            val cameraDistance = 320f
            val samplePoints = 72

            strandConfigs.forEach { (offsetPhase, strokeWidth, strandColor) ->
                val ribbonPath = Path()
                var firstPoint = true

                for (i in 0..samplePoints) {
                    val theta = (i.toFloat() / samplePoints) * 2f * PI.toFloat()
                    val t = theta + flowPhase + offsetPhase

                    // Parametric 3D flowing ribbon: Torus knot with dynamic harmonic ripple
                    val r = baseRadius * 0.72f * (1f + 0.18f * sin(2f * t + secondaryPhase) + reactiveAmp * 0.15f * sin(4f * t))
                    val x3d = r * (cos(t) + 0.35f * cos(3f * t))
                    val y3d = r * (sin(t) - 0.35f * sin(3f * t))
                    val z3d = r * 0.55f * sin(2f * t + flowPhase * 0.5f)

                    // 3D perspective projection
                    val depthFactor = cameraDistance / (cameraDistance + z3d)
                    val projX = center.x + x3d * depthFactor
                    val projY = center.y + y3d * depthFactor

                    if (firstPoint) {
                        ribbonPath.moveTo(projX, projY)
                        firstPoint = false
                    } else {
                        ribbonPath.lineTo(projX, projY)
                    }
                }

                ribbonPath.close()

                // Draw luminous ribbon strand
                drawPath(
                    path = ribbonPath,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            strandColor.copy(alpha = 0.2f),
                            strandColor.copy(alpha = 0.95f),
                            electricBlue.copy(alpha = 0.85f),
                            subtleViolet.copy(alpha = 0.9f),
                            strandColor.copy(alpha = 0.2f)
                        ),
                        center = center
                    ),
                    style = Stroke(
                        width = strokeWidth * (1f + reactiveAmp * 0.3f),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 3. Central Luminous Core Resonance
            val corePulseRadius = baseRadius * 0.38f * (1f + reactiveAmp * 0.4f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f + reactiveAmp * 0.35f),
                        electricCyan.copy(alpha = 0.55f),
                        electricBlue.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = corePulseRadius
                ),
                radius = corePulseRadius,
                center = center
            )

            // Dynamic vocal acoustic ripples when speaking or listening
            if (liveState == LiveState.SPEAKING || liveState == LiveState.LISTENING) {
                val rippleRadius = baseRadius * (1.12f + reactiveAmp * 0.35f)
                drawCircle(
                    color = electricCyan.copy(alpha = 0.22f + reactiveAmp * 0.25f),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = subtleViolet.copy(alpha = 0.14f + reactiveAmp * 0.15f),
                    radius = rippleRadius * 1.18f,
                    center = center,
                    style = Stroke(width = 1.0.dp.toPx())
                )
            }
        }
    }
}
