package dev.aidos.tcr.pill

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.actor.ActorPillConfig
import dev.aidos.tcr.actor.ActorPillStatus
import dev.aidos.tcr.actor.ActorRole
import dev.aidos.tcr.actor.GlyphType
import dev.aidos.tcr.actor.PipSymbolShape
import dev.aidos.tcr.actor.RedPillState
import dev.aidos.tcr.actor.RedPillTextMode
import dev.aidos.tcr.actor.RedPillTurn
import kotlin.math.roundToInt

/**
 * Molecular TCR component: Redesigned interactive floating AI actor pill.
 *
 * Visual System:
 * - Ember (Cursor Actor): Warm coral/vermilion capsule with subtle highlight, restrained shadow,
 *   soft ambient halo, and luminous inset pip. Follows caret with smooth spring feedback.
 * - Orbit (Worker Actors): Theme-adaptive capsule (dark charcoal in light mode, warm neutral in dark mode)
 *   with actor-coded accent pips (blue/green/custom) and radial countdown rings.
 *
 * Touch & Accessibility:
 * - Ultra-compact visible capsule (~24-28dp width, ~14-16dp height at rest) centered inside a
 *   generous 48dp minimum interactive touch target.
 *
 * Gestures & Functional Behavior:
 * 1. Ember: Drag-to-Move Cursor (relocates text caret with elevation boost & scale feedback).
 * 2. Orbit: Elastic Drag-to-Pause (20dp) & Tear-to-Abort (80dp) with Spring & Freeze controls.
 * 3. Tap: Happenings HUD; Double-tap: Transcript Drawer.
 */
@Composable
fun FloatingActorPill(
    config: ActorPillConfig,
    status: ActorPillStatus,
    onSingleClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier,
    redPillState: RedPillState? = null,
    onThoughtDismiss: (() -> Unit)? = null,
    onQuestionSubmit: ((String) -> Unit)? = null,
    onConversationSubmit: ((String) -> Unit)? = null,
    onDragCursor: ((dragDelta: Offset) -> Unit)? = null,
    onPauseStaging: (() -> Unit)? = null,
    onResumeStaging: (() -> Unit)? = null,
    onAbortStaging: (() -> Unit)? = null,
    isDarkTheme: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f
) {
    val density = LocalDensity.current

    // Drag states
    var isCursorDragging by remember { mutableStateOf(false) }
    var isWorkerDragging by remember { mutableStateOf(false) }
    var rawDragOffset by remember { mutableStateOf(Offset.Zero) }

    val isDragging = isCursorDragging || isWorkerDragging

    val pauseThresholdPx = with(density) { 20.dp.toPx() }
    val abortThresholdPx = with(density) { 80.dp.toPx() }

    // Animated spring return when released in pause zone (Spring & Freeze)
    val springTarget = if (isWorkerDragging) rawDragOffset * 0.45f else Offset.Zero
    val animatedOffset by animateOffsetAsState(
        targetValue = springTarget,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "elasticOffset"
    )

    // Tactile scale feedback during dragging
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
        label = "dragScale"
    )

    // Status checks
    val isPulsing = status is ActorPillStatus.WaitingAmbient
    val isCommitFlash = status is ActorPillStatus.CommitFlash
    val isDisambiguation = status is ActorPillStatus.Disambiguation

    // Visual Tokens Resolution (Ember vs Orbit, theme adaptation, status-driven)
    val tokens = PillVisualTokens.resolve(
        config = config,
        status = status,
        isDarkTheme = isDarkTheme,
        isDragging = isDragging
    )

    val activeGlyph = when {
        redPillState?.textMode == RedPillTextMode.QUESTION_BUBBLE -> GlyphType.QUESTION
        status is ActorPillStatus.CadenceStaging -> status.glyph
        status is ActorPillStatus.WorkingOnTarget -> status.glyph
        status is ActorPillStatus.CommitFlash -> GlyphType.CHECK
        status is ActorPillStatus.Disambiguation -> GlyphType.QUESTION
        status is ActorPillStatus.HoldingSelection -> GlyphType.SELECTION
        else -> config.defaultGlyph
    }

    // Pointer input configuration depending on pill role & state
    val dragModifier = if (config.role == ActorRole.ORCHESTRATOR_DICTATION && onDragCursor != null) {
        // Ember (Cursor Actor): Dragging relocates text cursor with responsive tactile feedback
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { isCursorDragging = true },
                onDragEnd = { isCursorDragging = false },
                onDragCancel = { isCursorDragging = false },
                onDrag = { change, dragAmount ->
                    change.consume()
                    onDragCursor(dragAmount)
                }
            )
        }
    } else if (status is ActorPillStatus.CadenceStaging) {
        // Orbit (Worker Actor): Elastic drag-to-pause (20dp) & tear-to-abort (80dp)
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    isWorkerDragging = true
                    rawDragOffset = Offset.Zero
                },
                onDragEnd = {
                    isWorkerDragging = false
                    rawDragOffset = Offset.Zero
                },
                onDragCancel = {
                    isWorkerDragging = false
                    rawDragOffset = Offset.Zero
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    rawDragOffset += dragAmount
                    val dist = rawDragOffset.getDistance()

                    if (dist >= abortThresholdPx) {
                        // Exceeded 80dp: Tear-to-Abort
                        isWorkerDragging = false
                        rawDragOffset = Offset.Zero
                        onAbortStaging?.invoke()
                    } else if (dist >= pauseThresholdPx) {
                        // Exceeded 20dp: Pause countdown (Spring & Freeze)
                        onPauseStaging?.invoke()
                    }
                }
            )
        }
    } else {
        Modifier
    }

    // Interactive container: provides a generous 48dp minimum touch target
    // while centering the compact visible pill capsule.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset {
                IntOffset(
                    animatedOffset.x.roundToInt(),
                    animatedOffset.y.roundToInt()
                )
            }
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .then(dragModifier)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleClick() },
                    onDoubleTap = { onDoubleClick() }
                )
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(dragScale)
        ) {
            // Mode a) Thought bubble (dashed outline), Mode b) Question bubble, or Mode c) Conversation History
            if (redPillState != null) {
                when (redPillState.textMode) {
                    RedPillTextMode.THOUGHT_BUBBLE -> {
                        RedPillThoughtBubble(
                            text = redPillState.thoughtText,
                            onDismiss = onThoughtDismiss
                        )
                    }
                    RedPillTextMode.QUESTION_BUBBLE -> {
                        RedPillQuestionBubble(
                            prompt = redPillState.questionPrompt,
                            onSubmit = { onQuestionSubmit?.invoke(it) }
                        )
                    }
                    RedPillTextMode.CONVERSATION_HISTORY -> {
                        RedPillConversationHistoryPanel(
                            history = redPillState.conversationHistory,
                            onSubmit = { onConversationSubmit?.invoke(it) }
                        )
                    }
                    RedPillTextMode.NONE -> Unit
                }
            }

            ActorPillCapsule(
                tokens = tokens,
                isPulsing = isPulsing,
                isCommitFlash = isCommitFlash,
                isDisambiguation = isDisambiguation
            ) {
                // Micro Glyph / Radial Countdown Ring Container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(14.dp)
                ) {
                    if (status is ActorPillStatus.CadenceStaging) {
                        RadialCountdownRing(
                            progress = status.progress,
                            ringColor = if (tokens.isEmber) Color.White else tokens.actorAccentColor,
                            trackColor = tokens.ringTrackColor,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp
                        )
                    } else if (status is ActorPillStatus.WorkingGeneric && status.progress != null) {
                        RadialCountdownRing(
                            progress = status.progress,
                            ringColor = if (tokens.isEmber) Color.White else tokens.actorAccentColor,
                            trackColor = tokens.ringTrackColor,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp
                        )
                    }

                    ActionGlyph(
                        glyphType = activeGlyph,
                        tint = tokens.contentColor,
                        modifier = Modifier.size(9.dp)
                    )
                }

                // Small Inset Indicator Pip (Symbol-Information UI states)
                Spacer(modifier = Modifier.width(3.5.dp))
                val pipColor = if (redPillState != null) Color(redPillState.pipState.colorArgb) else tokens.pipColor
                val pipShape = redPillState?.pipState?.shape ?: PipSymbolShape.CIRCLE
                val pipPulsing = redPillState?.pipState?.isPulsing ?: (isPulsing && !tokens.isEmber)
                ActorPillPip(
                    color = pipColor,
                    isPulsing = pipPulsing,
                    shape = pipShape
                )

                // Status text / badge information (holding selection, staging, working)
                when (status) {
                    is ActorPillStatus.HoldingSelection -> {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${status.wordCount} sanaa",
                            color = tokens.contentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    is ActorPillStatus.CadenceStaging -> {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (status.isPaused) "Tauko: ${status.actionDescription}" else status.actionDescription,
                            color = tokens.contentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    is ActorPillStatus.WorkingGeneric -> {
                        status.statusMessage?.let { msg ->
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = msg,
                                color = tokens.contentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> Unit
                }
            }

            // Directional pointer beak when locked onto a text target
            if (status is ActorPillStatus.WorkingOnTarget || status is ActorPillStatus.CadenceStaging) {
                Canvas(
                    modifier = Modifier.size(width = 8.dp, height = 4.dp)
                ) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height)
                        close()
                    }
                    drawPath(path = path, color = tokens.beakColor)
                }
            }

            // Spring & Freeze Paused Controls: [▶ Jatka] [✕ Peru]
            if (status is ActorPillStatus.CadenceStaging && status.isPaused) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            color = if (isDarkTheme) Color(0xFF1F242D).copy(alpha = 0.94f) else Color(0xFF2B303A).copy(alpha = 0.94f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 0.5.dp,
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "▶ Jatka",
                        color = Color(0xFF4ADE80),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onResumeStaging?.invoke() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "✕ Peru",
                        color = Color(0xFFF87171),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onAbortStaging?.invoke() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Small inset indicator pip inside the pill capsule.
 * Gives the pill distinct AI character, morphing color & shape to signal
 * informational status (idle, listening, thinking, question, done).
 */
@Composable
fun ActorPillPip(
    color: Color,
    isPulsing: Boolean,
    shape: PipSymbolShape = PipSymbolShape.CIRCLE,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PipPulse")
    val pipAlpha by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pipAlpha"
        )
    } else {
        remember { mutableFloatStateOf(1.0f) }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(when (shape) {
                PipSymbolShape.CIRCLE -> 3.5.dp
                PipSymbolShape.DIAMOND -> 4.5.dp
                else -> 8.dp
            })
            .alpha(pipAlpha)
    ) {
        when (shape) {
            PipSymbolShape.CIRCLE -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(color = color, shape = CircleShape)
                )
            }
            PipSymbolShape.DIAMOND -> {
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .rotate(45f)
                        .background(color = color, shape = RoundedCornerShape(0.5.dp))
                )
            }
            PipSymbolShape.STAR -> {
                Text(
                    text = "✦",
                    fontSize = 7.sp,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 7.sp
                )
            }
            PipSymbolShape.QUESTION -> {
                Text(
                    text = "?",
                    fontSize = 7.sp,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 7.sp
                )
            }
            PipSymbolShape.CHECK -> {
                Text(
                    text = "✓",
                    fontSize = 7.sp,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 7.sp
                )
            }
            PipSymbolShape.MIC -> {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Mic",
                    tint = color,
                    modifier = Modifier.size(7.dp)
                )
            }
            PipSymbolShape.CLOCK -> {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Clock",
                    tint = color,
                    modifier = Modifier.size(7.dp)
                )
            }
        }
    }
}

/**
 * Mode a: Beige-ish thought bubble with dashed outline anchored to the Red Pill.
 */
@Composable
fun RedPillThoughtBubble(
    text: String,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(14.dp)
    val strokeWidthDp = 1.25.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .shadow(elevation = 3.dp, shape = bubbleShape)
                .background(color = Color(0xFFFBF8F2), shape = bubbleShape)
                .drawWithContent {
                    drawContent()
                    val strokeWidth = strokeWidthDp.toPx()
                    val halfStroke = strokeWidth / 2f
                    val stroke = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
                    )
                    drawRoundRect(
                        color = Color(0xFFCBBFAF),
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        cornerRadius = CornerRadius((14.dp.toPx() - halfStroke).coerceAtLeast(0f)),
                        style = stroke
                    )
                }
                .clickable { onDismiss?.invoke() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                color = Color(0xFF3E3832),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 15.sp
            )
        }

        // Thought trail pips
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

/**
 * Mode b: Beige-ish question bubble with question text, interactive input box, and submit action.
 */
@Composable
fun RedPillQuestionBubble(
    prompt: String,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var responseInput by remember { mutableStateOf("") }
    var isInputRevealed by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(bottom = 6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFBF8F2),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFE0C9A6)),
            modifier = Modifier
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 160.dp, max = 240.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isInputRevealed = !isInputRevealed }
                ) {
                    Text(
                        text = "?",
                        color = Color(0xFFB45309),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = prompt,
                        color = Color(0xFF292524),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp
                    )
                }

                if (isInputRevealed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BasicTextField(
                            value = responseInput,
                            onValueChange = { responseInput = it },
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
                                    if (responseInput.isNotBlank()) {
                                        onSubmit(responseInput)
                                        responseInput = ""
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Vastaa", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Pointer trail beak
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(Color(0xFFFBF8F2), shape = CircleShape)
                .border(0.75.dp, Color(0xFFE0C9A6), shape = CircleShape)
        )
    }
}

/**
 * Mode c: Full blown multi-turn conversation history HUD and entry box.
 */
@Composable
fun RedPillConversationHistoryPanel(
    history: List<RedPillTurn>,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFBF8F2),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFFE0C9A6)),
        modifier = modifier
            .padding(bottom = 6.dp)
            .widthIn(min = 240.dp, max = 320.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "💬 Keskusteluhistoria",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1917)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                for (turn in history.takeLast(5)) {
                    val isUser = turn.isFromUser
                    Column(
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isUser) Color(0xFFEF4444) else Color.White,
                            border = if (isUser) null else BorderStroke(0.5.dp, Color(0xFFE5E5E5)),
                            modifier = Modifier
                        ) {
                            Text(
                                text = turn.text,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                color = if (isUser) Color.White else Color(0xFF1C1917),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White, shape = RoundedCornerShape(6.dp))
                        .border(0.5.dp, Color(0xFFD6C8B4), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    textStyle = TextStyle(fontSize = 11.sp, color = Color(0xFF1C1917))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEF4444))
                        .clickable {
                            if (promptInput.isNotBlank()) {
                                onSubmit(promptInput)
                                promptInput = ""
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
