package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CallStatus
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HinataCharacterView(
    callStatus: CallStatus,
    audioLevel: Float,
    visemeMouthOpen: Float,
    modifier: Modifier = Modifier
) {
    // Infinite transition for organic breathing and micro-movements
    val infiniteTransition = rememberInfiniteTransition(label = "hinata_breathing")

    // Slow sinusoidal breathing cycle (~3.6 seconds)
    val breathingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breathing_phase"
    )

    // Chakra aura pulse
    val chakraPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chakra_pulse"
    )

    // Natural eye blink state (blinks every 3.5 to 5.2 seconds, blink duration ~170ms)
    var eyeBlinkProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            val waitMillis = (3400..5000).random().toLong()
            delay(waitMillis)
            // Blink down
            eyeBlinkProgress = 1f
            delay(160)
            // Eye open
            eyeBlinkProgress = 0f
            // Occasional double blink
            if ((1..4).random() == 1) {
                delay(120)
                eyeBlinkProgress = 1f
                delay(140)
                eyeBlinkProgress = 0f
            }
        }
    }

    // Dynamic head tilt based on speaking/listening
    val targetRotation = when (callStatus) {
        CallStatus.SPEAKING -> sin(breathingPhase) * 1.2f
        CallStatus.LISTENING -> (sin(breathingPhase * 0.7f) * 0.8f) + (audioLevel * 1.5f)
        CallStatus.THINKING -> -1.8f
        else -> sin(breathingPhase) * 0.5f
    }
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "head_tilt"
    )

    // Breathing vertical offset and scale
    val verticalOffsetDp = (sin(breathingPhase) * 3.5f).dp
    val breathScale = 1.0f + (sin(breathingPhase) * 0.014f) + (if (callStatus == CallStatus.SPEAKING) 0.008f else 0f)

    // Smooth mouth opening interpolator
    val smoothMouthOpen by animateFloatAsState(
        targetValue = if (callStatus == CallStatus.SPEAKING) visemeMouthOpen else 0f,
        animationSpec = tween(durationMillis = 75, easing = LinearEasing),
        label = "smooth_mouth"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = verticalOffsetDp)
                .scale(breathScale)
                .graphicsLayer {
                    rotationZ = animatedRotation
                    cameraDistance = 12f * density
                }
        ) {
            // Full-screen portrait image of Hinata in her cozy bedroom
            Image(
                painter = painterResource(id = R.drawable.img_hinata_bg),
                contentDescription = "Hinata Hyuga",
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        // Draw base character image
                        drawContent()

                        // 1. Pale Lavender Byakugan Eyes Chakra Glow
                        drawByakuganChakraGlow(
                            widthPx = size.width,
                            heightPx = size.height,
                            pulse = chakraPulse,
                            isSpeaking = callStatus == CallStatus.SPEAKING
                        )

                        // 2. Natural Eyelid Blinking Overlay
                        if (eyeBlinkProgress > 0.05f) {
                            drawEyelidBlink(
                                widthPx = size.width,
                                heightPx = size.height,
                                blinkProgress = eyeBlinkProgress
                            )
                        }

                        // 3. Dynamic Real-Time Mouth Lip Sync
                        if (smoothMouthOpen > 0.08f) {
                            drawDynamicMouth(
                                widthPx = size.width,
                                heightPx = size.height,
                                mouthOpen = smoothMouthOpen
                            )
                        }

                        // 4. Subtle holographic call vignette and room neon glow
                        drawCyberVignette(size.width, size.height, audioLevel)
                    },
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Draws the pale glowing lavender Byakugan chakra pulse over Hinata's eyes.
 * Eye coordinates are relative to the 9:16 portrait composition.
 */
private fun DrawScope.drawByakuganChakraGlow(
    widthPx: Float,
    heightPx: Float,
    pulse: Float,
    isSpeaking: Boolean
) {
    // Relative eye centers on the generated portrait
    val leftEyeCenter = Offset(widthPx * 0.405f, heightPx * 0.428f)
    val rightEyeCenter = Offset(widthPx * 0.535f, heightPx * 0.412f)
    val eyeRadius = widthPx * 0.045f * (if (isSpeaking) 1.1f else 1.0f)

    val glowColor = Color(0xFFE4D5FF).copy(alpha = 0.22f * pulse)
    val irisColor = Color(0xFFFFFFFF).copy(alpha = 0.35f * pulse)

    // Left eye glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(irisColor, glowColor, Color.Transparent),
            center = leftEyeCenter,
            radius = eyeRadius * 1.8f
        ),
        radius = eyeRadius * 1.8f,
        center = leftEyeCenter
    )

    // Right eye glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(irisColor, glowColor, Color.Transparent),
            center = rightEyeCenter,
            radius = eyeRadius * 1.8f
        ),
        radius = eyeRadius * 1.8f,
        center = rightEyeCenter
    )
}

/**
 * Draws soft organic eyelid curves when Hinata blinks.
 */
private fun DrawScope.drawEyelidBlink(
    widthPx: Float,
    heightPx: Float,
    blinkProgress: Float
) {
    val skinTone = Color(0xFFFEE8DC).copy(alpha = 0.96f)
    val lashColor = Color(0xFF2C2442)

    // Left eye area
    val leftEyeCenter = Offset(widthPx * 0.405f, heightPx * 0.428f)
    val leftEyeW = widthPx * 0.082f
    val leftEyeH = widthPx * 0.042f * blinkProgress

    drawOval(
        color = skinTone,
        topLeft = Offset(leftEyeCenter.x - leftEyeW / 2, leftEyeCenter.y - leftEyeH / 2),
        size = Size(leftEyeW, leftEyeH),
        style = Fill
    )
    // Left eye lash line
    val leftLashPath = Path().apply {
        moveTo(leftEyeCenter.x - leftEyeW / 2, leftEyeCenter.y)
        quadraticTo(
            leftEyeCenter.x,
            leftEyeCenter.y + leftEyeH * 0.4f,
            leftEyeCenter.x + leftEyeW / 2,
            leftEyeCenter.y - leftEyeH * 0.2f
        )
    }
    drawPath(leftLashPath, lashColor, style = Stroke(width = 2.5f))

    // Right eye area
    val rightEyeCenter = Offset(widthPx * 0.535f, heightPx * 0.412f)
    val rightEyeW = widthPx * 0.080f
    val rightEyeH = widthPx * 0.040f * blinkProgress

    drawOval(
        color = skinTone,
        topLeft = Offset(rightEyeCenter.x - rightEyeW / 2, rightEyeCenter.y - rightEyeH / 2),
        size = Size(rightEyeW, rightEyeH),
        style = Fill
    )
    // Right eye lash line
    val rightLashPath = Path().apply {
        moveTo(rightEyeCenter.x - rightEyeW / 2, rightEyeCenter.y - rightEyeH * 0.1f)
        quadraticTo(
            rightEyeCenter.x,
            rightEyeCenter.y + rightEyeH * 0.35f,
            rightEyeCenter.x + rightEyeW / 2,
            rightEyeCenter.y
        )
    }
    drawPath(rightLashPath, lashColor, style = Stroke(width = 2.5f))
}

/**
 * Draws dynamic lip-sync mouth aperture opening and closing with speech syllables.
 */
private fun DrawScope.drawDynamicMouth(
    widthPx: Float,
    heightPx: Float,
    mouthOpen: Float
) {
    // Mouth center position on portrait
    val mouthCenter = Offset(widthPx * 0.490f, heightPx * 0.485f)
    val mouthWidth = widthPx * 0.062f * (0.85f + mouthOpen * 0.25f)
    val mouthHeight = widthPx * 0.042f * mouthOpen

    // Soft mouth interior shadow
    val interiorColor = Color(0xFF88243A).copy(alpha = 0.95f)
    val teethColor = Color(0xFFFFF7F9).copy(alpha = 0.95f)
    val lipBorder = Color(0xFFC7556E)

    // Inner cavity
    val mouthPath = Path().apply {
        moveTo(mouthCenter.x - mouthWidth / 2, mouthCenter.y)
        // Upper lip curve
        cubicTo(
            mouthCenter.x - mouthWidth * 0.2f, mouthCenter.y - mouthHeight * 0.45f,
            mouthCenter.x + mouthWidth * 0.2f, mouthCenter.y - mouthHeight * 0.45f,
            mouthCenter.x + mouthWidth / 2, mouthCenter.y
        )
        // Lower lip curve
        cubicTo(
            mouthCenter.x + mouthWidth * 0.25f, mouthCenter.y + mouthHeight * 0.85f,
            mouthCenter.x - mouthWidth * 0.25f, mouthCenter.y + mouthHeight * 0.85f,
            mouthCenter.x - mouthWidth / 2, mouthCenter.y
        )
        close()
    }

    drawPath(mouthPath, color = interiorColor, style = Fill)

    // Subtle upper teeth line
    if (mouthOpen > 0.35f) {
        val teethHeight = mouthHeight * 0.28f
        val teethPath = Path().apply {
            moveTo(mouthCenter.x - mouthWidth * 0.32f, mouthCenter.y - mouthHeight * 0.15f)
            lineTo(mouthCenter.x + mouthWidth * 0.32f, mouthCenter.y - mouthHeight * 0.15f)
            lineTo(mouthCenter.x + mouthWidth * 0.25f, mouthCenter.y - mouthHeight * 0.15f + teethHeight)
            lineTo(mouthCenter.x - mouthWidth * 0.25f, mouthCenter.y - mouthHeight * 0.15f + teethHeight)
            close()
        }
        drawPath(teethPath, color = teethColor, style = Fill)
    }

    // Lip line contour
    drawPath(mouthPath, color = lipBorder, style = Stroke(width = 1.8f))
}

/**
 * Draws subtle cyber ambiance vignette and bottom gradient to make HUD elements pop cleanly.
 */
private fun DrawScope.drawCyberVignette(widthPx: Float, heightPx: Float, audioLevel: Float) {
    // Top subtle gradient for top bar readability
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xAA080516),
                Color(0x44080516),
                Color.Transparent
            ),
            startY = 0f,
            endY = heightPx * 0.25f
        ),
        size = Size(widthPx, heightPx * 0.25f)
    )

    // Bottom dark gradient so glass call controls and indicators have high contrast
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x55080516),
                Color(0xDD080516)
            ),
            startY = heightPx * 0.65f,
            endY = heightPx
        ),
        topLeft = Offset(0f, heightPx * 0.65f),
        size = Size(widthPx, heightPx * 0.35f)
    )

    // Reactive audio cyan edge glow when listening/speaking
    if (audioLevel > 0.15f) {
        val reactiveAlpha = (audioLevel * 0.25f).coerceIn(0f, 0.35f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xFF00E5FF).copy(alpha = reactiveAlpha)),
                center = Offset(widthPx / 2, heightPx / 2),
                radius = widthPx * 0.85f
            )
        )
    }
}
