package dev.aidos.tcr.pill

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Atomic TCR component: Radial countdown ring with calibrated sweep progress.
 * "Work, as a concept, requires time."
 *
 * Visual System:
 * - Subtle background track ring with low opacity.
 * - Primary countdown progress arc sweeping clockwise from top (-90 degrees).
 * - Optional rotating radar-style sweep gradient line for ongoing generative tasks.
 */
@Composable
fun RadialCountdownRing(
    progress: Float,
    ringColor: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = ringColor.copy(alpha = 0.2f),
    strokeWidth: Dp = 1.75.dp,
    showRadarSweep: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by if (showRadarSweep) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "radarAngle"
        )
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1, easing = LinearEasing)
            ),
            label = "radarZero"
        )
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = strokeWidth.toPx()
            val diameter = size.minDimension - strokeWidthPx
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

            // Background circular track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx)
            )

            // Progress countdown arc (sweeps clockwise starting at -90 degrees / 12 o'clock)
            val clampedProgress = progress.coerceIn(0f, 1f)
            val progressSweep = 360f * clampedProgress

            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = progressSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Optional radar sweep effect for active processing
            if (showRadarSweep) {
                val sweepBrush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.75f to ringColor.copy(alpha = 0.1f),
                    1.0f to ringColor.copy(alpha = 0.45f),
                    center = center
                )
                drawArc(
                    brush = sweepBrush,
                    startAngle = sweepAngle - 90f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize
                )
            }
        }
    }
}
