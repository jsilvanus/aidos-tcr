package dev.aidos.tcr.pill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.actor.ActorPillConfig
import dev.aidos.tcr.actor.PipSymbolShape
import dev.aidos.tcr.actor.PillProjectionEvent
import dev.aidos.tcr.actor.PillProjectionStyle
import dev.aidos.tcr.actor.PillTargetCone
import dev.aidos.tcr.actor.ProjectedPillInstance
import dev.aidos.tcr.actor.RedPillHistoryEntry
import dev.aidos.tcr.actor.RedPillTextMode
import dev.aidos.tcr.actor.SpatialPoint
import dev.aidos.tcr.actor.SpatialRect
import dev.aidos.tcr.actor.TargetConeHighlight
import dev.aidos.tcr.actor.TextRangeBounds
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Molecular TCR component: Canonical Persistent Red Pill (Ember Caret Orchestrator 0xFFEF4444).
 *
 * AUTHORITATIVE OWNERSHIP: Dictator Ecosystem & TCR (D46 / RFC-0051)
 *
 * Canonical Features:
 * - Orchestrator Pill Projection: 3 animation styles:
 *     1. Beam-Teleport (laser conduit & photon collapse)
 *     2. Mitosis / Fission Ejection (elastic budding & slingshot)
 *     3. Holographic Materialization (cybernetic reticle & scanlines)
 * - Autonomous Fast-Go / Fast-Stop relocation across the workspace
 * - Volumetric Orange-ish Projection Cone highlighting target document objects (words, paraphrases)
 * - 3 basic text display UI modes (Thought bubble, Question bubble, Conversation history)
 * - Symbol-information morphing indicator pip (idle, listening, thinking, question, done)
 * - Hover physics with dynamic elevation, dual-tone shadows, and ground cast shadow
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersistentRedPill(
    modifier: Modifier = Modifier,
    initialMode: RedPillTextMode = RedPillTextMode.NONE,
    ambientThoughtText: String = "Orchestrator monitoring contacts & pending actions...",
    pendingQuestionPrompt: String = "Would you like me to draft a follow-up reply?",
    isExecuting: Boolean = false,
    executionError: String? = null,
    suggestedActions: List<String> = emptyList(),
    onExecutePrompt: ((prompt: String) -> Unit)? = null,
    historyEntries: List<RedPillHistoryEntry> = emptyList()
) {
    val scope = rememberCoroutineScope()
    var currentTextMode by remember { mutableStateOf(initialMode) }
    var userPromptInput by remember { mutableStateOf("") }

    // Active pill projection & target cone highlight states
    var activeProjection by remember { mutableStateOf<PillProjectionEvent?>(null) }
    var activeTargetCone by remember { mutableStateOf<PillTargetCone?>(null) }
    val projectedPills = remember { mutableStateListOf<ProjectedPillInstance>() }

    // Autonomous fast-go, fast-stop relocation offset
    var isSnapMoved by remember { mutableStateOf(false) }
    val autonomousOffsetX = remember { Animatable(0f) }

    fun triggerAutonomousSnapRelocation() {
        scope.launch {
            val targetX = if (isSnapMoved) 0f else 60f
            isSnapMoved = !isSnapMoved
            autonomousOffsetX.animateTo(
                targetValue = targetX,
                animationSpec = tween(durationMillis = 320, easing = FastGoFastStopEasing)
            )
        }
    }

    fun projectNewPill(style: PillProjectionStyle) {
        val (name, color, offset) = when (style) {
            PillProjectionStyle.BEAM_TELEPORT -> Triple("Editor", 0xFF3B82F6L, SpatialPoint(240f, 60f))
            PillProjectionStyle.MITOSIS_EJECT -> Triple("AI Gen", 0xFF10B981L, SpatialPoint(180f, 75f))
            PillProjectionStyle.HOLOGRAPHIC_MATERIALIZE -> Triple("Analyst", 0xFF8B5CF6L, SpatialPoint(300f, 70f))
        }
        val event = PillProjectionEvent(
            id = "proj_${System.currentTimeMillis()}",
            sourcePoint = SpatialPoint(120f, 0f),
            targetPoint = offset,
            targetPillConfig = ActorPillConfig(
                id = "proj_${System.currentTimeMillis()}",
                role = dev.aidos.tcr.actor.ActorRole.CUSTOM,
                displayName = name,
                colorArgb = color,
                defaultGlyph = dev.aidos.tcr.actor.GlyphType.SPARK
            ),
            style = style
        )
        activeProjection = event
    }

    // Hover Float Oscillation
    val infiniteTransition = rememberInfiniteTransition(label = "redPillHover")
    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hoverOffset"
    )

    // Pulsing animation for the indicator pip
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Symbol-Information UI state resolution
    val (pipColor, pipShape) = when {
        isExecuting -> Color(0xFFA855F7) to PipSymbolShape.STAR // Thinking / reasoning violet
        currentTextMode == RedPillTextMode.QUESTION_BUBBLE -> Color(0xFFFBBF24) to PipSymbolShape.QUESTION // Question gold
        userPromptInput.isNotBlank() -> Color(0xFFEF4444) to PipSymbolShape.MIC // Listening / user typing red
        historyEntries.isNotEmpty() && !isExecuting -> Color(0xFF10B981) to PipSymbolShape.CHECK // Done emerald
        else -> Color(0xFFFF8A80) to PipSymbolShape.CIRCLE // Ambient resting rose
    }

    // Dynamic elevation that rises as the pill hovers
    val dynamicElevation = (6.dp + (-hoverOffset * 1.2f).dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Volumetric Orange-ish Target Projection Cone
        activeTargetCone?.let { cone ->
            TargetObjectProjectionCone(
                cone = cone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )
        }

        // Active Spawning Animation Overlay (Beam, Mitosis, or Holographic)
        activeProjection?.let { event ->
            PillProjectionOverlay(
                event = event,
                onAnimationComplete = {
                    projectedPills.add(
                        ProjectedPillInstance(
                            id = event.id,
                            name = event.targetPillConfig.displayName,
                            roleColor = Color(event.targetPillConfig.colorArgb.toULong()),
                            targetOffset = Offset(event.targetPoint.x, event.targetPoint.y),
                            style = event.style
                        )
                    )
                    activeProjection = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        }

        // Mode a) Beige-ish Thought Bubble (Dashed outline)
        AnimatedVisibility(
            visible = currentTextMode == RedPillTextMode.THOUGHT_BUBBLE,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFBF8F2),
                    modifier = Modifier
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp))
                        .drawBehind {
                            val stroke = Stroke(
                                width = 1.25.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
                            )
                            drawRoundRect(
                                color = Color(0xFFCBBFAF),
                                size = size,
                                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                                style = stroke
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clickable { currentTextMode = RedPillTextMode.NONE }
                ) {
                    Text(
                        text = "💭 $ambientThoughtText",
                        color = Color(0xFF3E3832),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(Color(0xFFFBF8F2), shape = CircleShape)
                        .border(0.75.dp, Color(0xFFCBBFAF), shape = CircleShape)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(Color(0xFFFBF8F2), shape = CircleShape)
                        .border(0.75.dp, Color(0xFFCBBFAF), shape = CircleShape)
                )
            }
        }

        // Mode b) Beige-ish Question Bubble (Interactive input box)
        AnimatedVisibility(
            visible = currentTextMode == RedPillTextMode.QUESTION_BUBBLE,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            var questionResponse by remember { mutableStateOf("") }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFBF8F2),
                    modifier = Modifier
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFE0C9A6), shape = RoundedCornerShape(14.dp))
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.widthIn(min = 220.dp, max = 340.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "?",
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = pendingQuestionPrompt,
                                color = Color(0xFF292524),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = questionResponse,
                                onValueChange = { questionResponse = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.White, shape = RoundedCornerShape(6.dp))
                                    .border(0.5.dp, Color(0xFFD6C8B4), shape = RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                textStyle = TextStyle(fontSize = 11.sp, color = Color(0xFF1C1917))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEF4444))
                                    .clickable {
                                        if (questionResponse.isNotBlank()) {
                                            onExecutePrompt?.invoke(questionResponse)
                                            questionResponse = ""
                                            currentTextMode = RedPillTextMode.CONVERSATION_HISTORY
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Reply", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(Color(0xFFFBF8F2), shape = CircleShape)
                        .border(0.75.dp, Color(0xFFE0C9A6), shape = CircleShape)
                )
            }
        }

        // 1. The Floating Red Pill Capsule (Hover physics, body gradient, and fast-go snap motion)
        Box(
            modifier = Modifier
                .offset { IntOffset(x = autonomousOffsetX.value.roundToInt(), y = hoverOffset.roundToInt()) }
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = dynamicElevation,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f))
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFFFF6253), // Top Coral highlight
                                Color(0xFFEF4444), // Vibrant Vermilion body
                                Color(0xFFB91C1C)  // Deep Crimson base
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        // Toggle between resting and conversation history drawer
                        currentTextMode = if (currentTextMode == RedPillTextMode.CONVERSATION_HISTORY) {
                            RedPillTextMode.NONE
                        } else {
                            RedPillTextMode.CONVERSATION_HISTORY
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Morphing Indicator Pip (Symbol-Information UI states)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(12.dp)
                                .alpha(pulseAlpha)
                        ) {
                            when (pipShape) {
                                PipSymbolShape.CIRCLE -> {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(pipColor, shape = CircleShape)
                                    )
                                }
                                PipSymbolShape.DIAMOND -> {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .rotate(45f)
                                            .background(pipColor, shape = RoundedCornerShape(1.dp))
                                    )
                                }
                                PipSymbolShape.STAR -> {
                                    Text(
                                        text = "✦",
                                        fontSize = 10.sp,
                                        color = pipColor,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 10.sp
                                    )
                                }
                                PipSymbolShape.QUESTION -> {
                                    Text(
                                        text = "?",
                                        fontSize = 10.sp,
                                        color = pipColor,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 10.sp
                                    )
                                }
                                PipSymbolShape.CHECK -> {
                                    Text(
                                        text = "✓",
                                        fontSize = 10.sp,
                                        color = pipColor,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 10.sp
                                    )
                                }
                                PipSymbolShape.MIC -> {
                                    Text(
                                        text = "🎙",
                                        fontSize = 9.sp,
                                        lineHeight = 9.sp
                                    )
                                }
                                PipSymbolShape.CLOCK -> {
                                    Text(
                                        text = "⏱",
                                        fontSize = 9.sp,
                                        lineHeight = 9.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentTextMode == RedPillTextMode.QUESTION_BUBBLE) "❓ Aidos Orchestrator" else "🔴 Aidos Orchestrator",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• Cross-Chat AI",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.88f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isExecuting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Quick Mode Switcher Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = if (currentTextMode == RedPillTextMode.THOUGHT_BUBBLE) 0.35f else 0.15f))
                                    .clickable {
                                        currentTextMode = if (currentTextMode == RedPillTextMode.THOUGHT_BUBBLE) RedPillTextMode.NONE else RedPillTextMode.THOUGHT_BUBBLE
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("💭 Thought", fontSize = 10.sp, color = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = if (currentTextMode == RedPillTextMode.QUESTION_BUBBLE) 0.35f else 0.15f))
                                    .clickable {
                                        currentTextMode = if (currentTextMode == RedPillTextMode.QUESTION_BUBBLE) RedPillTextMode.NONE else RedPillTextMode.QUESTION_BUBBLE
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("❓ Ask", fontSize = 10.sp, color = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = if (currentTextMode == RedPillTextMode.CONVERSATION_HISTORY) 0.35f else 0.15f))
                                    .clickable {
                                        currentTextMode = if (currentTextMode == RedPillTextMode.CONVERSATION_HISTORY) RedPillTextMode.NONE else RedPillTextMode.CONVERSATION_HISTORY
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(if (currentTextMode == RedPillTextMode.CONVERSATION_HISTORY) "▲ Close" else "▼ History", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Ground cast shadow underneath the hover capsule
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .height(3.dp)
                .width((72.dp - (-hoverOffset * 4f).dp).coerceAtLeast(36.dp))
                .alpha((0.35f - (-hoverOffset * 0.05f)).coerceIn(0.1f, 0.4f))
                .background(Color.Black.copy(alpha = 0.45f), shape = CircleShape)
        )

        // Render Projected Child Pills spawned by Orchestrator
        if (projectedPills.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (proj in projectedPills) {
                    val color = Color(proj.roleColorArgb)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = color.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, color),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(color, shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "✨ ${proj.name}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }
            }
        }

        // Orchestrator Action Toolbar: Pill Projections, Cone Highlight & Snap Move
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.Black.copy(alpha = 0.05f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Orchestrator Actions:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                // 3 Pill Projections
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF3B82F6).copy(alpha = 0.18f))
                        .clickable { projectNewPill(PillProjectionStyle.BEAM_TELEPORT) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("⚡ Beam Pill", fontSize = 9.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.18f))
                        .clickable { projectNewPill(PillProjectionStyle.MITOSIS_EJECT) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("🧬 Mitosis Pill", fontSize = 9.sp, color = Color(0xFF047857), fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.18f))
                        .clickable { projectNewPill(PillProjectionStyle.HOLOGRAPHIC_MATERIALIZE) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("📡 Holo Pill", fontSize = 9.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.Medium)
                }

                // Orange Target Projection Cone
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF97316).copy(alpha = if (activeTargetCone != null) 0.35f else 0.18f))
                        .clickable {
                            activeTargetCone = if (activeTargetCone != null) null else PillTargetCone(
                                pillPosition = SpatialPoint(120f, 0f),
                                targetBounds = TextRangeBounds(0, 10),
                                targetRect = SpatialRect(80f, 60f, 220f, 84f),
                                colorArgb = 0xFFF97316L,
                                targetLabel = "Key Action"
                            )
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(if (activeTargetCone != null) "🔦 Cone Active" else "🔦 Cone", fontSize = 9.sp, color = Color(0xFFC2410C), fontWeight = FontWeight.Medium)
                }

                // Autonomous Fast-Go / Fast-Stop Relocation
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.18f))
                        .clickable { triggerAutonomousSnapRelocation() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("⚡ Snap Move", fontSize = 9.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Medium)
                }
            }
        }

        // Mode c) Full-blown Conversation History HUD and entry box
        AnimatedVisibility(
            visible = currentTextMode == RedPillTextMode.CONVERSATION_HISTORY,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 2.dp, end = 2.dp)
                    .border(
                        width = 1.dp,
                        color = Color(0xFFEF4444).copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "💬 Multi-Turn Conversation History (TCR Orchestrator):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // History message feed
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Color.Black.copy(alpha = 0.04f), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (turn in historyEntries.takeLast(6)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (turn.isFromUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (turn.isFromUser) Color(0xFFEF4444) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .shadow(1.dp, shape = RoundedCornerShape(8.dp))
                                        .widthIn(max = 280.dp)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = turn.text,
                                            fontSize = 11.sp,
                                            color = if (turn.isFromUser) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (turn.executedTools.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Tools: ${turn.executedTools.joinToString()}",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (turn.isFromUser) Color.White.copy(alpha = 0.8f) else Color(0xFF166534)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input & Run Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userPromptInput,
                            onValueChange = { userPromptInput = it },
                            placeholder = {
                                Text(
                                    "e.g. 'Tell Bob I\'ll be 10 min late', 'Summarize document'",
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (userPromptInput.isNotBlank()) {
                                    onExecutePrompt?.invoke(userPromptInput)
                                    userPromptInput = ""
                                }
                            },
                            enabled = !isExecuting && userPromptInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Run", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    if (suggestedActions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Suggested actions:",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (chipText in suggestedActions) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                        .clickable {
                                            val prompt = chipText.substringAfter(" ")
                                            userPromptInput = prompt
                                            onExecutePrompt?.invoke(prompt)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(chipText, fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // Execution Errors
                    if (executionError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEE2E2), shape = RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text("⚠️ $executionError", fontSize = 11.sp, color = Color(0xFF991B1B))
                        }
                    }
                }
            }
        }
    }
}
