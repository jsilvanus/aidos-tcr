package dev.aidos.tcr.pill

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.actor.PillProjectionEvent
import dev.aidos.tcr.actor.PillProjectionStyle
import dev.aidos.tcr.actor.PillTargetCone
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Custom fast-go, fast-stop easing curve:
 * Explosive acceleration covering ~85% of transit distance in first ~30% time,
 * cruising with velocity, then executing an immediate high-damping tactile snap stop.
 */
val FastGoFastStopEasing = CubicBezierEasing(0.05f, 0.95f, 0.10f, 1.0f)

/**
 * Directional volumetric projection cone (warm orange-ish by default) emitted
 * from an Actor Pill to highlight a target document object (word, phrase, paraphrase).
 */
@Composable
fun TargetObjectProjectionCone(
    cone: PillTargetCone,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "conePulse")
    val pulseAlpha by if (cone.isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        remember { mutableFloatStateOf(0.75f) }
    }

    val coneBaseColor = Color(cone.colorArgb)

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Volumetric light cone drawn from pill anchor to target rect bounds
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sourceX = cone.pillPosition.x
            val sourceY = cone.pillPosition.y

            val targetLeft = cone.targetRect.left
            val targetRight = cone.targetRect.right
            val targetTop = cone.targetRect.top
            val targetBottom = cone.targetRect.bottom
            val targetWidth = cone.targetRect.width
            val targetHeight = cone.targetRect.height

            // Cone polygonal path
            val conePath = Path().apply {
                moveTo(sourceX, sourceY)
                lineTo(targetLeft, targetTop)
                lineTo(targetRight, targetTop)
                close()
            }

            // Volumetric gradient from vibrant source to soft ambient target
            val coneGradient = Brush.verticalGradient(
                colors = listOf(
                    coneBaseColor.copy(alpha = 0.52f * pulseAlpha),
                    coneBaseColor.copy(alpha = 0.22f * pulseAlpha),
                    coneBaseColor.copy(alpha = 0.08f)
                ),
                startY = sourceY,
                endY = targetTop
            )

            drawPath(path = conePath, brush = coneGradient)

            // Volumetric cone edge rays
            drawLine(
                color = coneBaseColor.copy(alpha = 0.65f * pulseAlpha),
                start = Offset(sourceX, sourceY),
                end = Offset(targetLeft, targetTop),
                strokeWidth = 1.2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = coneBaseColor.copy(alpha = 0.65f * pulseAlpha),
                start = Offset(sourceX, sourceY),
                end = Offset(targetRight, targetTop),
                strokeWidth = 1.2f,
                cap = StrokeCap.Round
            )

            // Glowing target highlight frame around the word / phrase
            val targetCorner = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            drawRoundRect(
                color = coneBaseColor.copy(alpha = 0.18f * pulseAlpha),
                topLeft = Offset(targetLeft - 2.dp.toPx(), targetTop - 2.dp.toPx()),
                size = Size(targetWidth + 4.dp.toPx(), targetHeight + 4.dp.toPx()),
                cornerRadius = targetCorner
            )
            drawRoundRect(
                color = coneBaseColor.copy(alpha = 0.85f * pulseAlpha),
                topLeft = Offset(targetLeft - 2.dp.toPx(), targetTop - 2.dp.toPx()),
                size = Size(targetWidth + 4.dp.toPx(), targetHeight + 4.dp.toPx()),
                cornerRadius = targetCorner,
                style = Stroke(width = 1.25.dp.toPx())
            )
        }

        // 2. Micro highlight label chip (e.g. "Paraphrase", "Target word")
        cone.targetLabel?.let { label ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = coneBaseColor.copy(alpha = 0.92f),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = cone.targetRect.left.roundToInt(),
                            y = (cone.targetRect.top - 18.dp.toPx()).roundToInt()
                        )
                    }
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Orchestrator Spawning Animation Overlay:
 * Projects a new worker pill into existence using one of 3 distinct animation styles:
 * 1. BEAM_TELEPORT (radiant coherent laser conduit with photon collapse)
 * 2. MITOSIS_EJECT (budding daughter pill, elastic snap, and slingshot deceleration)
 * 3. HOLOGRAPHIC_MATERIALIZE (quantum wireframe grid and cybernetic scanlines)
 */
@Composable
fun PillProjectionOverlay(
    event: PillProjectionEvent,
    onAnimationComplete: (projectionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(event.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = event.durationMs.toInt(),
                easing = FastGoFastStopEasing
            )
        )
        onAnimationComplete(event.id)
    }

    when (event.style) {
        PillProjectionStyle.BEAM_TELEPORT -> {
            BeamTeleportProjection(
                event = event,
                progress = progress.value,
                modifier = modifier
            )
        }
        PillProjectionStyle.MITOSIS_EJECT -> {
            MitosisEjectProjection(
                event = event,
                progress = progress.value,
                modifier = modifier
            )
        }
        PillProjectionStyle.HOLOGRAPHIC_MATERIALIZE -> {
            HolographicMaterializeProjection(
                event = event,
                progress = progress.value,
                modifier = modifier
            )
        }
    }
}

/**
 * Style 1: Beam-Teleporting Animation:
 * Coherent laser conduit energizes between Orchestrator and target,
 * pulses photonic particles, and condenses into the new pill.
 */
@Composable
fun BeamTeleportProjection(
    event: PillProjectionEvent,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val actorColor = Color(event.targetPillConfig.colorArgb)
    val source = Offset(event.sourcePoint.x, event.sourcePoint.y)
    val target = Offset(event.targetPoint.x, event.targetPoint.y)

    Canvas(modifier = modifier.fillMaxSize()) {
        val beamProgress = (progress / 0.82f).coerceIn(0f, 1f)
        val currentBeamEnd = Offset(
            x = source.x + (target.x - source.x) * beamProgress,
            y = source.y + (target.y - source.y) * beamProgress
        )

        // 1. Outer radiant beam conduit
        val beamWidth = 5.dp.toPx() * (1f - progress * 0.4f)
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFEF4444), actorColor),
                start = source,
                end = currentBeamEnd
            ),
            start = source,
            end = currentBeamEnd,
            strokeWidth = beamWidth,
            cap = StrokeCap.Round
        )

        // 2. Inner core laser filament
        drawLine(
            color = Color.White,
            start = source,
            end = currentBeamEnd,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 3. Target photon condensation ring (energizes in the final 45% of transit)
        if (progress > 0.45f) {
            val ringProgress = (progress - 0.45f) / 0.55f
            val condensationRadius = 24.dp.toPx() * (1f - ringProgress)
            val ringAlpha = (1f - ringProgress).coerceIn(0f, 1f)

            drawCircle(
                color = actorColor.copy(alpha = ringAlpha),
                radius = condensationRadius,
                center = target,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = ringAlpha),
                radius = (condensationRadius * 0.5f),
                center = target,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

/**
 * Style 2: Mitosis / Fission Ejection Animation:
 * Daughter cell buds out of orchestrator body, elongates along velocity vector,
 * snaps free, and decelerates into resting coordinates with spring bounce.
 */
@Composable
fun MitosisEjectProjection(
    event: PillProjectionEvent,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val actorColor = Color(event.targetPillConfig.colorArgb)
    val srcX = event.sourcePoint.x
    val srcY = event.sourcePoint.y
    val dstX = event.targetPoint.x
    val dstY = event.targetPoint.y

    val curX = srcX + (dstX - srcX) * progress
    val curY = srcY + (dstY - srcY) * progress

    // Motion stretch: capsule elongates during highest velocity transit
    val stretch = 1f + sin(progress * Math.PI.toFloat()) * 0.38f

    Box(modifier = modifier.fillMaxSize()) {
        // Elastic filament between mother and daughter cell during budding phase (< 35% time)
        if (progress < 0.35f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val tetherAlpha = 1f - (progress / 0.35f)
                drawLine(
                    color = Color(0xFFEF4444).copy(alpha = tetherAlpha),
                    start = Offset(srcX, srcY),
                    end = Offset(curX, curY),
                    strokeWidth = 3.dp.toPx() * tetherAlpha,
                    cap = StrokeCap.Round
                )
            }
        }

        // Flying daughter pill capsule
        Box(
            modifier = Modifier
                .offset { IntOffset(curX.roundToInt(), curY.roundToInt()) }
                .scale(scaleX = stretch, scaleY = 1f / stretch)
                .size(24.dp, 14.dp)
                .background(actorColor, shape = CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.65f), shape = CircleShape)
        )
    }
}

/**
 * Style 3: Holographic Materialization:
 * Quantum wireframe reticle projects over the destination with rapid laser scanlines,
 * materializing the pill into solid reality.
 */
@Composable
fun HolographicMaterializeProjection(
    event: PillProjectionEvent,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val actorColor = Color(event.targetPillConfig.colorArgb)
    val srcX = event.sourcePoint.x
    val srcY = event.sourcePoint.y
    val dstX = event.targetPoint.x
    val dstY = event.targetPoint.y

    Canvas(modifier = modifier.fillMaxSize()) {
        val src = Offset(srcX, srcY)
        val dst = Offset(dstX, dstY)

        // 1. Hologram laser telemetry line
        drawLine(
            color = actorColor.copy(alpha = 0.40f * (1f - progress * 0.5f)),
            start = src,
            end = dst,
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )

        // 2. Quantum reticle around target
        val reticleRadius = 20.dp.toPx() * (1f - progress * 0.25f)
        drawCircle(
            color = actorColor.copy(alpha = progress * 0.75f),
            radius = reticleRadius,
            center = dst,
            style = Stroke(
                width = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
            )
        )

        // 3. Cybernetic scanlines assembling capsule
        val pillWidth = 26.dp.toPx()
        val pillHeight = 15.dp.toPx()
        val pillTopLeft = Offset(dst.x - pillWidth / 2f, dst.y - pillHeight / 2f)

        drawRoundRect(
            color = actorColor.copy(alpha = progress),
            topLeft = pillTopLeft,
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = progress * 0.85f),
            topLeft = pillTopLeft,
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
