package dev.aidos.tcr.spatial.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import dev.aidos.tcr.intent.TcrDerivedStatus
import dev.aidos.tcr.intent.TcrIntentActorKind
import dev.aidos.tcr.intent.TcrIntentNodeType
import dev.aidos.tcr.spatial.edge.BezierCausalEdge
import dev.aidos.tcr.spatial.node.AtomicTaskCapsule
import dev.aidos.tcr.spatial.node.GoalHexagonNode
import dev.aidos.tcr.spatial.node.SatelliteCriterionNode
import dev.aidos.tcr.theme.ColorTokens
import dev.aidos.tcr.theme.colorForTcrActor
import dev.aidos.tcr.theme.colorForTcrStatus

data class SpatialCanvasNodeItem(
    val id: String,
    val title: String,
    val type: TcrIntentNodeType = TcrIntentNodeType.ATOMIC_TASK,
    val actorKind: TcrIntentActorKind = TcrIntentActorKind.AGENT,
)

data class SpatialCanvasEdgeItem(
    val fromNodeId: String,
    val toNodeId: String,
)

@Composable
fun InfiniteZuiCanvasView(
    nodes: List<SpatialCanvasNodeItem>,
    edges: List<SpatialCanvasEdgeItem>,
    positions: Map<String, Pair<Float, Float>>,
    statuses: Map<String, TcrDerivedStatus>,
    viewport: ViewportTransform,
    lod: LodLevel,
    onPan: (deltaX: Float, deltaY: Float) -> Unit,
    onZoom: (zoomFactor: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (pan != Offset.Zero) {
                        onPan(pan.x / viewport.zoom, pan.y / viewport.zoom)
                    }
                    if (zoom != 1f) {
                        onZoom(zoom)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Causal Edges (Culled if sub-pixel)
            if (lod != LodLevel.MACRO_GALAXY) {
                edges.forEach { edge ->
                    val fromPos = positions[edge.fromNodeId]
                    val toPos = positions[edge.toNodeId]
                    if (fromPos != null && toPos != null) {
                        val screenFrom = viewport.toScreenCoordinates(fromPos.first, fromPos.second)
                        val screenTo = viewport.toScreenCoordinates(toPos.first, toPos.second)

                        BezierCausalEdge.drawBezierEdge(
                            drawScope = this,
                            start = screenFrom,
                            end = screenTo,
                            color = Color(0x6638BDF8),
                            strokeWidth = 2f * viewport.zoom,
                        )
                    }
                }
            }

            // 2. Draw Nodes with LOD Culling
            nodes.forEach { node ->
                val pos = positions[node.id] ?: return@forEach
                if (!viewport.isPointInViewport(pos.first, pos.second, canvasW, canvasH)) {
                    return@forEach // LOD Viewport Culling
                }

                val screenPos = viewport.toScreenCoordinates(pos.first, pos.second)
                val actorColor = colorForTcrActor(node.actorKind)
                val status = statuses[node.id] ?: TcrDerivedStatus.NOT_STARTED
                val statusColor = colorForTcrStatus(status)

                when (lod) {
                    LodLevel.MACRO_GALAXY -> {
                        // Tiny dot or cluster representation
                        drawCircle(
                            color = actorColor,
                            radius = 4f,
                            center = screenPos,
                        )
                    }
                    LodLevel.MESO_CLUSTER -> {
                        when (node.type) {
                            TcrIntentNodeType.GOAL, TcrIntentNodeType.GROUP -> {
                                GoalHexagonNode.drawHexagon(
                                    drawScope = this,
                                    center = screenPos,
                                    radius = 24f * viewport.zoom,
                                    fillColor = Color(0xDD0F172A),
                                    borderColor = statusColor,
                                )
                            }
                            TcrIntentNodeType.ACCEPTANCE_CRITERION -> {
                                SatelliteCriterionNode.drawDiamond(
                                    drawScope = this,
                                    center = screenPos,
                                    size = 18f * viewport.zoom,
                                    fillColor = Color(0xDD0F172A),
                                    borderColor = statusColor,
                                )
                            }
                            else -> {
                                AtomicTaskCapsule.drawCapsule(
                                    drawScope = this,
                                    center = screenPos,
                                    width = 60f * viewport.zoom,
                                    height = 24f * viewport.zoom,
                                    fillColor = Color(0xDD0F172A),
                                    borderColor = actorColor,
                                )
                            }
                        }
                    }
                    LodLevel.MICRO_BLUEPRINT -> {
                        when (node.type) {
                            TcrIntentNodeType.GOAL, TcrIntentNodeType.GROUP -> {
                                GoalHexagonNode.drawHexagon(
                                    drawScope = this,
                                    center = screenPos,
                                    radius = 36f * viewport.zoom,
                                    fillColor = Color(0xDD0F172A),
                                    borderColor = statusColor,
                                )
                            }
                            TcrIntentNodeType.ACCEPTANCE_CRITERION -> {
                                SatelliteCriterionNode.drawDiamond(
                                    drawScope = this,
                                    center = screenPos,
                                    size = 28f * viewport.zoom,
                                    fillColor = Color(0xDD0F172A),
                                    borderColor = statusColor,
                                )
                            }
                            else -> {
                                AtomicTaskCapsule.drawCapsule(
                                    drawScope = this,
                                    center = screenPos,
                                    width = 100f * viewport.zoom,
                                    height = 36f * viewport.zoom,
                                    fillColor = Color(0xDD0F172A),
                                    borderColor = actorColor,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
