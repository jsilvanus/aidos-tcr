package dev.aidos.tcr.spatial.radar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.aidos.tcr.intent.TcrDerivedStatus
import dev.aidos.tcr.theme.ColorTokens
import kotlin.math.cos
import kotlin.math.sin

enum class RadarZone {
    STRIKE_ZONE,
    ORBIT_ZONE,
    DEEP_SYSTEM,
    INTERMEDIATE_ORBIT,
    OUTER_PERIMETER,
}

data class RadarNodeItem(
    val id: String,
    val title: String,
    val status: TcrDerivedStatus,
    val angleRad: Float,
    val distanceRatio: Float,
    val zone: RadarZone,
)

fun colorForTcrStatus(status: TcrDerivedStatus): Color = when (status) {
    TcrDerivedStatus.NOT_STARTED -> ColorTokens.StatusNotStarted
    TcrDerivedStatus.IN_PROGRESS -> ColorTokens.StatusInProgress
    TcrDerivedStatus.BLOCKED -> ColorTokens.StatusBlocked
    TcrDerivedStatus.NEEDS_REVIEW -> ColorTokens.StatusReview
    TcrDerivedStatus.DONE -> ColorTokens.StatusDone
    TcrDerivedStatus.STALE -> ColorTokens.StatusStale
    TcrDerivedStatus.CONFLICTED -> ColorTokens.StatusConflicted
}

/**
 * Mobile 180-degree bottom-arc thumb-zone view (Design 4, RFC-0111).
 * Positioned in the bottom 40% of the screen for ergonomic single-hand reach.
 */
@Composable
fun MobileHorizonArcView(
    nodes: List<RadarNodeItem>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height)
            val maxRadius = size.width * 0.9f

            val strikeRadius = maxRadius * 0.4f
            val orbitRadius = maxRadius * 0.7f
            val deepRadius = maxRadius * 1.0f

            // Draw concentric arc horizon rings
            drawCircle(color = Color(0x3338BDF8), radius = strikeRadius, center = center, style = Stroke(1.5f))
            drawCircle(color = Color(0x2294A3B8), radius = orbitRadius, center = center, style = Stroke(1f))
            drawCircle(color = Color(0x1194A3B8), radius = deepRadius, center = center, style = Stroke(1f))

            // Plot nodes on the horizon arc
            nodes.forEach { item ->
                val r = when (item.zone) {
                    RadarZone.STRIKE_ZONE -> strikeRadius * 0.85f
                    RadarZone.ORBIT_ZONE, RadarZone.INTERMEDIATE_ORBIT -> (strikeRadius + orbitRadius) / 2f
                    RadarZone.DEEP_SYSTEM, RadarZone.OUTER_PERIMETER -> (orbitRadius + deepRadius) / 2f
                }

                // Constrain angle to top half (-PI to 0)
                val clampedAngle = (-Math.PI + item.distanceRatio * Math.PI).toFloat()
                val x = center.x + r * cos(clampedAngle)
                val y = center.y + r * sin(clampedAngle)

                val nodeColor = colorForTcrStatus(item.status)
                drawCircle(color = nodeColor, radius = 8f, center = Offset(x, y))
            }
        }
    }
}
