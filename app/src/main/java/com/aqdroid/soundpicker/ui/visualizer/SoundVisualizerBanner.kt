package com.aqdroid.soundpicker.ui.visualizer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aqdroid.soundpicker.model.SoundItem
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SoundVisualizerBanner(
    currentSound: SoundItem?,
    isPlaying: Boolean,
    amplitude: Float,
    frequencyBands: FloatArray,
    modifier: Modifier = Modifier
) {
    // Smoothly animate the visual amplitude so transitions (including to 0 on silence) are silky smooth
    val animatedAmp = remember { Animatable(0f) }

    LaunchedEffect(amplitude, isPlaying) {
        val target = if (isPlaying && currentSound?.isSilent != true) amplitude else 0f
        animatedAmp.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = if (target > 0f) 80 else 250,
                easing = FastOutSlowInEasing
            )
        )
    }

    // Continuous smooth phase animation
    val infiniteTransition = rememberInfiniteTransition(label = "wave_phase")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07080E),
                        Color(0xFF0F111E),
                        Color(0xFF0A0C16)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2A2D3E),
                        Color(0xFF141724)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // High fidelity Canvas Wave Visualizer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val midY = height * 0.58f

            val currentAmp = animatedAmp.value

            // 1. Ambient Background Glow that pulses with audio
            val glowRadius = (width * 0.45f).coerceAtLeast(100f)
            val glowCenter = Offset(width * 0.5f, midY)
            val glowAlpha = (0.08f + currentAmp * 0.22f).coerceIn(0.05f, 0.35f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6366F1).copy(alpha = glowAlpha),
                        Color(0xFF8B5CF6).copy(alpha = glowAlpha * 0.6f),
                        Color.Transparent
                    ),
                    center = glowCenter,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = glowCenter
            )

            // 2. Layer 3: Cyan/Turquoise back wave
            val pathBack = Path()
            val points = 80
            val dx = width / points

            val ampBack = (height * 0.22f) * currentAmp + (height * 0.03f)

            pathBack.moveTo(0f, midY)
            for (i in 0..points) {
                val x = i * dx
                val normalizedX = i.toFloat() / points
                val envelope = sin(normalizedX * PI).toFloat() // taper at edges
                val bandWeight = frequencyBands.getOrElse(i % 5) { 0.5f }

                val y = midY + sin((normalizedX * 4 * PI + phase3).toDouble()).toFloat() * ampBack * envelope * (0.8f + 0.4f * bandWeight)
                if (i == 0) pathBack.moveTo(x, y) else pathBack.lineTo(x, y)
            }

            drawPath(
                path = pathBack,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF06B6D4).copy(alpha = 0.55f),
                        Color(0xFF3B82F6).copy(alpha = 0.65f),
                        Color(0xFF10B981).copy(alpha = 0.50f)
                    )
                ),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Layer 2: Indigo / Magenta filled wave under-glow
            val pathFill = Path()
            val pathMiddle = Path()
            val ampMid = (height * 0.30f) * currentAmp + (height * 0.04f)

            pathMiddle.moveTo(0f, midY)
            pathFill.moveTo(0f, height)
            pathFill.lineTo(0f, midY)

            for (i in 0..points) {
                val x = i * dx
                val normalizedX = i.toFloat() / points
                val envelope = sin(normalizedX * PI).toFloat()
                val bandWeight = frequencyBands.getOrElse((i + 2) % 5) { 0.5f }

                val y = midY - sin((normalizedX * 3.5 * PI + phase2).toDouble()).toFloat() * ampMid * envelope * (0.7f + 0.5f * bandWeight)
                pathMiddle.lineTo(x, y)
                pathFill.lineTo(x, y)
            }
            pathFill.lineTo(width, height)
            pathFill.close()

            // Translucent glowing wave body
            drawPath(
                path = pathFill,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6).copy(alpha = (0.05f + currentAmp * 0.18f)),
                        Color(0xFF4F46E5).copy(alpha = (0.02f + currentAmp * 0.08f)),
                        Color.Transparent
                    ),
                    startY = midY - ampMid,
                    endY = height
                )
            )

            drawPath(
                path = pathMiddle,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFEC4899).copy(alpha = 0.7f),
                        Color(0xFF8B5CF6).copy(alpha = 0.85f),
                        Color(0xFF6366F1).copy(alpha = 0.7f)
                    )
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Layer 1: Foreground electric violet-blue wave
            val pathFront = Path()
            val ampFront = (height * 0.36f) * currentAmp + (height * 0.05f)

            pathFront.moveTo(0f, midY)
            for (i in 0..points) {
                val x = i * dx
                val normalizedX = i.toFloat() / points
                val envelope = sin(normalizedX * PI).toFloat()
                val bandWeight = frequencyBands.getOrElse((i * 3) % 5) { 0.6f }

                val y = midY + sin((normalizedX * 2.8 * PI + phase1).toDouble()).toFloat() * ampFront * envelope * (0.75f + 0.45f * bandWeight)
                if (i == 0) pathFront.moveTo(x, y) else pathFront.lineTo(x, y)
            }

            drawPath(
                path = pathFront,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF818CF8),
                        Color(0xFFA78BFA),
                        Color(0xFF38BDF8),
                        Color(0xFFC084FC)
                    )
                ),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 5. Floating particle light motes when active
            if (currentAmp > 0.05f) {
                for (p in 0..6) {
                    val pX = width * ((p * 0.14f + (phase1 * 0.05f)) % 1f)
                    val pAmp = frequencyBands.getOrElse(p % 5) { 0.5f }
                    val pY = midY - (pAmp * 25.dp.toPx()) * sin(phase2 + p).toFloat()
                    val pRadius = (2.5f + 2f * pAmp).dp.toPx()
                    drawCircle(
                        color = Color(0xFFC4B5FD).copy(alpha = (0.3f + 0.5f * currentAmp).coerceIn(0f, 1f)),
                        radius = pRadius,
                        center = Offset(pX, pY)
                    )
                }
            }
        }

        // Top Status Badge inside visualizer banner
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .background(Color(0xFF131522).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF2C3048).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isSilent = currentSound?.isSilent == true
            if (isSilent) {
                Icon(
                    imageVector = Icons.Default.VolumeOff,
                    contentDescription = "Silent",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Silence Active",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            } else if (isPlaying && currentSound != null) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Equalizer",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Playing: ${currentSound.title}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music",
                    tint = Color(0xFFA5B4FC),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentSound?.title ?: "Select a sound",
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        // Live Audio frequency indicator dots on top right
        if (isPlaying && currentSound?.isSilent != true) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..4) {
                    val bandVal = frequencyBands.getOrElse(i) { 0.5f }
                    val dotColor = when (i) {
                        0 -> Color(0xFF38BDF8)
                        1 -> Color(0xFF818CF8)
                        2 -> Color(0xFFA78BFA)
                        3 -> Color(0xFFF472B6)
                        else -> Color(0xFF34D399)
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(width = 3.dp, height = (6.dp + (14.dp * bandVal)))
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}
