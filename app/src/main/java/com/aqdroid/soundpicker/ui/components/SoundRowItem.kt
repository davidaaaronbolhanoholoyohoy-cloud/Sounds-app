package com.aqdroid.soundpicker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aqdroid.soundpicker.model.SoundItem
import com.aqdroid.soundpicker.model.SoundSourceType

@Composable
fun SoundRowItem(
    sound: SoundItem,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSilent = sound.isSilent

    // Highlight styles matching the user's mockups
    // The mockup highlights the selected item with solid black/dark framing and a play indicator on the right
    val containerBg = when {
        isSelected -> Color(0xFF1E2235)
        else -> Color.Transparent
    }

    val borderColor = when {
        isSelected -> Color(0xFF6366F1)
        else -> Color.Transparent
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sound_item_${sound.id}")
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect),
        color = containerBg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val iconTint = when {
                isSelected && isPlaying -> Color(0xFF818CF8)
                isSelected -> Color(0xFF6366F1)
                isSilent -> Color(0xFF94A3B8)
                else -> Color(0xFFCBD5E1)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF2C3252) else Color(0xFF181A26)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSilent) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = "Silent tone",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (isPlaying && isSelected) {
                    AnimatedEqualizerMini()
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music note",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Sound Titles
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sound.title,
                        color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isSilent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF334155))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Mute",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (sound.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sound.subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right side: Play arrow indicator (matching screenshots 2 & 3: Play arrow on the right)
            if (!isSilent) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("play_button_${sound.id}")
                ) {
                    if (isPlaying && isSelected) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Pause",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play sound",
                            tint = if (isSelected) Color(0xFF818CF8) else Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF10B981),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedEqualizerMini() {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_eq")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        modifier = Modifier.size(20.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h1.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color(0xFF38BDF8))
        )
        Spacer(modifier = Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color(0xFF818CF8))
        )
        Spacer(modifier = Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h3.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color(0xFFC084FC))
        )
    }
}
