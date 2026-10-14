package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CallStatus
import com.example.model.CallUiState
import com.example.ui.theme.CallRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SoftLilac
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicHudOverlay(
    state: CallUiState,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // 1. TOP BAR: Left Caller Info, Right Leaf Village Hologram
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Top-Left Caller Badge
            TopCallerInfo(
                state = state,
                pulseGlow = pulseGlow
            )

            // Top-Right Konoha Leaf Swirl HUD Bracket
            KonohaHudBadge(pulseGlow = pulseGlow)
        }

        // 2. MIDDLE FLOATING HUD PANELS
        // Left HUD: Audio Visualizer + Hinata Hyuga + ECG Heartbeat
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(bottom = 60.dp)
        ) {
            LeftTelemetryHudPanel(
                state = state,
                pulseGlow = pulseGlow
            )
        }

        // Right HUD: "हमेशा आपके साथ…" Romantic Sci-Fi Card
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = 60.dp)
        ) {
            RightAffectionHudPanel(pulseGlow = pulseGlow)
        }

        // 3. BOTTOM CALL CONTROLS DOCK
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Active status pill
            CallStatusPill(state = state, pulseGlow = pulseGlow)

            Spacer(modifier = Modifier.height(14.dp))

            // Glass Call Controls Bar
            BottomGlassCallBar(
                isMuted = state.isMuted,
                isSpeakerOn = state.isSpeakerOn,
                onMuteToggle = onMuteToggle,
                onSpeakerToggle = onSpeakerToggle,
                onEndCall = onEndCall
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom chevron
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = NeonCyan.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun TopCallerInfo(
    state: CallUiState,
    pulseGlow: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Avatar with Glowing Neon Border and Heart Badge
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer neon pulse ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.35f * pulseGlow),
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            // Avatar Image
            Image(
                painter = painterResource(id = R.drawable.ic_hinata_avatar),
                contentDescription = stringResource(R.string.hinata_name),
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
            )
            // Tiny bottom heart badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(16.dp)
                    .background(Color(0xFF140E2C), CircleShape)
                    .border(1.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = SoftLilac,
                    modifier = Modifier.size(9.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name, Call Duration, Voice Call Status
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.hinata_name),
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = ElectricBlue,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Timer (e.g. "00:32")
            val minutes = state.durationSeconds / 60
            val seconds = state.durationSeconds % 60
            val timerFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            Text(
                text = timerFormatted,
                color = SoftLilac,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Voice Call indicator with animated wave bars
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniVoiceWaveIcon(state = state)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.voice_call),
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MiniVoiceWaveIcon(state: CallUiState) {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_wave")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "w2"
    )

    Canvas(modifier = Modifier.size(14.dp, 10.dp)) {
        val stroke = 1.8f
        val color = if (state.status == CallStatus.SPEAKING || state.status == CallStatus.LISTENING) NeonCyan else TextMuted
        val h = size.height

        drawLine(color, Offset(2.dp.toPx(), h * (0.5f - 0.25f * wave1)), Offset(2.dp.toPx(), h * (0.5f + 0.25f * wave1)), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(7.dp.toPx(), h * (0.5f - 0.5f * wave2)), Offset(7.dp.toPx(), h * (0.5f + 0.5f * wave2)), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(12.dp.toPx(), h * (0.5f - 0.35f * wave1)), Offset(12.dp.toPx(), h * (0.5f + 0.35f * wave1)), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/**
 * Top-Right Leaf Village swirl icon in high-tech HUD bracket.
 */
@Composable
private fun KonohaHudBadge(pulseGlow: Float) {
    Box(
        modifier = Modifier
            .size(72.dp, 56.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x3300E5FF), Color(0x110A071E))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.7f * pulseGlow), ElectricBlue.copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // High-tech corner accents & Leaf Village spiral
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cyan = NeonCyan.copy(alpha = 0.85f * pulseGlow)

            // Sci-fi corner brackets
            val bracketLen = 8.dp.toPx()
            drawLine(cyan, Offset(0f, 0f), Offset(bracketLen, 0f), strokeWidth = 2f)
            drawLine(cyan, Offset(0f, 0f), Offset(0f, bracketLen), strokeWidth = 2f)

            drawLine(cyan, Offset(w, 0f), Offset(w - bracketLen, 0f), strokeWidth = 2f)
            drawLine(cyan, Offset(w, 0f), Offset(w, bracketLen), strokeWidth = 2f)

            drawLine(cyan, Offset(0f, h), Offset(bracketLen, h), strokeWidth = 2f)
            drawLine(cyan, Offset(0f, h), Offset(0f, h - bracketLen), strokeWidth = 2f)

            drawLine(cyan, Offset(w, h), Offset(w - bracketLen, h), strokeWidth = 2f)
            drawLine(cyan, Offset(w, h), Offset(w, h - bracketLen), strokeWidth = 2f)

            // Leaf Village (Konoha) spiral insignia
            val center = Offset(w / 2, h / 2)
            val spiralPath = Path()
            val totalPoints = 36
            for (i in 0..totalPoints) {
                val t = i.toFloat() / totalPoints
                val angle = t * 3.5f * PI.toFloat()
                val radius = (4.dp.toPx() + t * 14.dp.toPx())
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)
                if (i == 0) spiralPath.moveTo(x, y) else spiralPath.lineTo(x, y)
            }
            // Leaf triangle arrow
            spiralPath.lineTo(center.x + 16.dp.toPx(), center.y - 4.dp.toPx())
            drawPath(
                path = spiralPath,
                color = ElectricBlue,
                style = Stroke(width = 2.2f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Left Side HUD Panel with 8 Equalizer bars, Hinata Hyuga label, and ECG Heartbeat.
 */
@Composable
private fun LeftTelemetryHudPanel(
    state: CallUiState,
    pulseGlow: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ecg_wave")
    val ecgPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "ecg"
    )

    Box(
        modifier = Modifier
            .size(86.dp, 140.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x33100B2E), Color(0x1F00E5FF))
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.6f * pulseGlow), ElectricBlue.copy(alpha = 0.2f))
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Equalizer audio bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val baseLevel = state.audioLevel
                val isTalking = state.status == CallStatus.SPEAKING || state.status == CallStatus.LISTENING
                repeat(7) { index ->
                    val factor = if (isTalking) {
                        (sin((index + 1) * 1.3f) * 0.4f + 0.6f) * (0.3f + baseLevel * 0.7f)
                    } else 0.18f

                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height((24 * factor.coerceIn(0.15f, 1f)).dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(NeonCyan, ElectricBlue)
                                ),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            // 2. Heart icon + Hinata Hyuga
            Column(horizontalAlignment = Alignment.Start) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = SoftLilac,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.hinata_full_name),
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 13.sp
                )
            }

            // 3. Futuristic pulsing ECG line
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
            ) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                val path = Path().apply {
                    moveTo(0f, midY)
                    lineTo(w * 0.25f, midY)
                    // P wave
                    lineTo(w * 0.32f, midY - h * 0.2f)
                    lineTo(w * 0.38f, midY)
                    // QRS complex
                    lineTo(w * 0.45f, midY + h * 0.25f)
                    lineTo(w * 0.52f, midY - h * 0.55f)
                    lineTo(w * 0.60f, midY + h * 0.35f)
                    lineTo(w * 0.66f, midY)
                    // T wave
                    lineTo(w * 0.75f, midY - h * 0.25f)
                    lineTo(w * 0.82f, midY)
                    lineTo(w, midY)
                }

                drawPath(
                    path = path,
                    color = NeonCyan.copy(alpha = 0.85f),
                    style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Right Side HUD Panel with Heart and "हमेशा आपके साथ…"
 */
@Composable
private fun RightAffectionHudPanel(pulseGlow: Float) {
    Box(
        modifier = Modifier
            .size(90.dp, 84.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x33100B2E), Color(0x1F00E5FF))
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.6f * pulseGlow), ElectricBlue.copy(alpha = 0.2f))
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = SoftLilac,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.always_by_your_side),
                color = SoftLilac,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Animated Status Pill showing call state in pure Hindi.
 */
@Composable
private fun CallStatusPill(
    state: CallUiState,
    pulseGlow: Float
) {
    val statusText = when (state.status) {
        CallStatus.CONNECTING -> stringResource(R.string.call_status_reconnecting)
        CallStatus.LISTENING -> stringResource(R.string.call_status_listening)
        CallStatus.THINKING -> stringResource(R.string.call_status_thinking)
        CallStatus.SPEAKING -> stringResource(R.string.call_status_speaking)
        CallStatus.ENDED -> stringResource(R.string.call_status_ended)
    }

    val glowColor = when (state.status) {
        CallStatus.SPEAKING -> ElectricBlue
        CallStatus.LISTENING -> NeonCyan
        CallStatus.THINKING -> SoftLilac
        CallStatus.ENDED -> CallRed
        else -> NeonCyan
    }

    Box(
        modifier = Modifier
            .background(
                color = Color(0x66080516),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = glowColor.copy(alpha = 0.6f * pulseGlow),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Blinking status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(glowColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Bottom futuristic frosted glass dock with Mute, Red End Call, and Speaker toggles.
 */
@Composable
private fun BottomGlassCallBar(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onEndCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "end_call_pulse")
    val endCallScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "end_call_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x551E174D), Color(0x880C0724))
                ),
                shape = RoundedCornerShape(44.dp)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(ElectricBlue.copy(alpha = 0.5f), NeonCyan, ElectricBlue.copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(44.dp)
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // MUTE / UNMUTE BUTTON
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        color = if (isMuted) Color(0x44FF2A4B) else Color(0x33FFFFFF),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = if (isMuted) CallRed else NeonCyan.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onMuteToggle
                    )
                    .testTag("call_mute_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = stringResource(if (isMuted) R.string.call_unmute else R.string.call_mute),
                    tint = if (isMuted) CallRed else TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            // PROMINENT RED END CALL BUTTON
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(endCallScale)
                    .shadow(elevation = 14.dp, shape = CircleShape, spotColor = CallRed)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF4868), CallRed)
                        ),
                        shape = CircleShape
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEndCall
                    )
                    .testTag("call_end_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = stringResource(R.string.call_end),
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            // SPEAKER ON/OFF BUTTON
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        color = if (isSpeakerOn) Color(0x4400E5FF) else Color(0x33FFFFFF),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSpeakerOn) NeonCyan else Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSpeakerToggle
                    )
                    .testTag("call_speaker_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = stringResource(R.string.call_speaker),
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
