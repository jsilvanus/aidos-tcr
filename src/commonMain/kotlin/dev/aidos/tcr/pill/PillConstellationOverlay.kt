package dev.aidos.tcr.pill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.aidos.tcr.actor.ActorPillStatus
import dev.aidos.tcr.actor.PillConstellationManager
import kotlin.math.roundToInt

/**
 * Organism TCR component: Hosts the multi-pill constellation overlay (Option B).
 *
 * Coordinates:
 * - Red Pill (Voice Orchestrator) with Drag-to-Move and Autonomous Fast-Go/Fast-Stop Transit
 * - Worker Pill Creation Projections (Beam Teleport, Mitosis Eject, Holographic Materialize)
 * - Volumetric Target Projection Cone (Orange-ish beam highlighting words/paraphrases)
 * - Blue Pill (Deterministic Editor) with Spring & Freeze drag-to-pause and tear-to-abort
 * - Green Pill (AI Generator) with token streaming & drag intervention
 * - 1-click Happenings HUD floating card
 * - 2-click Transcript Drawer (docked on desktop, 50% on mobile)
 */
@Composable
fun PillConstellationOverlay(
    manager: PillConstellationManager,
    cursorX: Int,
    cursorY: Int,
    modifier: Modifier = Modifier,
    onDragCursor: ((Offset) -> Unit)? = null
) {
    val orchestratorConfig by manager.orchestratorConfig.collectAsState()
    val orchestratorStatus by manager.orchestratorStatus.collectAsState()

    val editorConfig by manager.editorConfig.collectAsState()
    val editorStatus by manager.editorStatus.collectAsState()

    val aiConfig by manager.aiConfig.collectAsState()
    val aiStatus by manager.aiStatus.collectAsState()
    val customPills by manager.customPills.collectAsState()

    val activeProjection by manager.activeProjection.collectAsState()
    val activeTargetCone by manager.activeTargetCone.collectAsState()

    val isHudVisible by manager.isHappeningsHudVisible.collectAsState()
    val isTranscriptOpen by manager.isTranscriptDrawerOpen.collectAsState()
    val happenings by manager.happenings.collectAsState()
    val transcript by manager.transcript.collectAsState()

    // Autonomous fast-go, fast-stop relocation animation
    val transitProgress = remember(orchestratorStatus) { Animatable(0f) }
    LaunchedEffect(orchestratorStatus) {
        if (orchestratorStatus is ActorPillStatus.AutonomousTransit) {
            transitProgress.snapTo(0f)
            transitProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastGoFastStopEasing)
            )
        }
    }

    val density = LocalDensity.current
    val pillHalfWidthPx = with(density) { 24.dp.roundToPx() }
    val pillHoverOffsetYPx = with(density) { 36.dp.roundToPx() }

    val targetRedPillX = when (val s = orchestratorStatus) {
        is ActorPillStatus.AutonomousTransit -> {
            val cur = s.from.x + (s.to.x - s.from.x) * transitProgress.value
            cur.roundToInt().coerceAtLeast(16)
        }
        else -> (cursorX - pillHalfWidthPx + 1).coerceAtLeast(16)
    }

    val targetRedPillY = when (val s = orchestratorStatus) {
        is ActorPillStatus.AutonomousTransit -> {
            val cur = s.from.y + (s.to.y - s.from.y) * transitProgress.value
            cur.roundToInt().coerceAtLeast(16)
        }
        else -> (cursorY - pillHoverOffsetYPx + 1).coerceAtLeast(16)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Volumetric Target Projection Cone (Orange-ish highlight)
        activeTargetCone?.let { cone ->
            TargetObjectProjectionCone(
                cone = cone,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Active Spawning Animation Overlay (Beam, Mitosis, Holographic)
        activeProjection?.let { projectionEvent ->
            PillProjectionOverlay(
                event = projectionEvent,
                onAnimationComplete = { manager.clearActiveProjection() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Red Pill (Voice Orchestrator & Caret Anchor)
        FloatingActorPill(
            config = orchestratorConfig,
            status = orchestratorStatus,
            onSingleClick = { manager.onPillSingleClick(orchestratorConfig.id) },
            onDoubleClick = { manager.onPillDoubleClick(orchestratorConfig.id) },
            onDragCursor = onDragCursor,
            modifier = Modifier.offset {
                IntOffset(
                    x = targetRedPillX,
                    y = targetRedPillY
                )
            }
        )

        // 4. Blue Pill (Deterministic Text Editor)
        FloatingActorPill(
            config = editorConfig,
            status = editorStatus,
            onSingleClick = { manager.onPillSingleClick(editorConfig.id) },
            onDoubleClick = { manager.onPillDoubleClick(editorConfig.id) },
            onPauseStaging = { manager.cadenceEngine.pauseStaging() },
            onResumeStaging = { manager.cadenceEngine.resumeStaging() },
            onAbortStaging = { manager.cadenceEngine.abortCurrentAction() },
            modifier = Modifier.offset {
                IntOffset(
                    x = targetRedPillX + with(density) { 72.dp.roundToPx() },
                    y = targetRedPillY
                )
            }
        )

        // 5. Green Pill (Generative AI Worker)
        FloatingActorPill(
            config = aiConfig,
            status = aiStatus,
            onSingleClick = { manager.onPillSingleClick(aiConfig.id) },
            onDoubleClick = { manager.onPillDoubleClick(aiConfig.id) },
            modifier = Modifier.offset {
                IntOffset(
                    x = targetRedPillX + with(density) { 144.dp.roundToPx() },
                    y = targetRedPillY
                )
            }
        )

        // 6. Custom Pills
        var customOffsetIndex = 1
        for ((_, customConfig) in customPills) {
            FloatingActorPill(
                config = customConfig,
                status = ActorPillStatus.WaitingAmbient,
                onSingleClick = { manager.onPillSingleClick(customConfig.id) },
                onDoubleClick = { manager.onPillDoubleClick(customConfig.id) },
                modifier = Modifier.offset {
                    IntOffset(
                        x = targetRedPillX + with(density) { (144 + 68 * customOffsetIndex).dp.roundToPx() },
                        y = targetRedPillY
                    )
                }
            )
            customOffsetIndex++
        }

        // 7. 1-click Happenings HUD (Floats near the pills)
        AnimatedVisibility(
            visible = isHudVisible,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { -20 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -20 }),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = (targetRedPillX - 20).coerceAtLeast(16),
                        y = (targetRedPillY + 54).coerceAtLeast(16)
                    )
                }
        ) {
            HappeningsHUD(
                events = happenings,
                onDismiss = { manager.dismissHappeningsHud() }
            )
        }

        // 8. 2-click Split Transcript Drawer (Docked to right edge)
        AnimatedVisibility(
            visible = isTranscriptOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            TranscriptSplitDrawer(
                entries = transcript,
                onClose = { manager.closeTranscriptDrawer() },
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}
