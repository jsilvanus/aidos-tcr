package dev.aidos.tcr.spatial.canvas

import kotlin.math.cos
import kotlin.math.sin

data class SpatialNodeLayoutItem(
    val id: String,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
)

object ForceDirectedLayout {
    fun layoutDag(
        nodeIds: List<String>,
        edges: List<Pair<String, String>> = emptyList(),
        canvasWidth: Float = 2000f,
        canvasHeight: Float = 2000f,
    ): Map<String, Pair<Float, Float>> {
        val count = nodeIds.size
        if (count == 0) return emptyMap()

        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        val result = mutableMapOf<String, Pair<Float, Float>>()

        nodeIds.forEachIndexed { index, id ->
            val angle = (2.0 * Math.PI * index / count.toDouble()).toFloat()
            val radius = 300f + (index % 5) * 150f
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            result[id] = x to y
        }

        return result
    }
}
