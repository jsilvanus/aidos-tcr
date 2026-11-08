package dev.aidos.tcr.actor

import kotlinx.serialization.Serializable

/**
 * The functional role and archetype of an Actor Pill.
 */
@Serializable
enum class ActorRole {
    /** Voice dictation orchestrator (Red pill, anchored at cursor). */
    ORCHESTRATOR_DICTATION,

    /** Deterministic text editor (Blue pill, executes text commands with cadence). */
    DETERMINISTIC_EDITOR,

    /** Generative AI agent (Green pill, writes/rewrites text streaming from its tip). */
    GENERATIVE_AI,

    /** User-defined or plugin-supplied actor. */
    CUSTOM
}

/**
 * Visual glyph / icon represented within the pill.
 */
@Serializable
enum class GlyphType {
    MIC,
    TEXT_CARET,
    SPARK,
    DELETE,
    SCISSORS,
    SELECTION,
    CHECK,
    CLOCK,
    QUESTION
}

/**
 * Operation impact severity used by the CadenceEngine to determine human-verification time.
 * "Work, as a concept, requires time."
 */
@Serializable
enum class OperationSeverity {
    /** Deleting paragraphs, wiping sections, clearing documents (800ms - 1200ms). */
    DESTRUCTIVE,

    /** Moving text, rephrasing, formatting blocks, wrapping structures (500ms). */
    STRUCTURAL,

    /** Single word edit, punctuation fix, small caret jump (250ms). */
    MINOR
}

/**
 * Character range in a text document.
 */
@Serializable
data class TextRangeBounds(
    val startChar: Int,
    val endChar: Int
) {
    val length: Int get() = kotlin.math.max(0, endChar - startChar)
}

/**
 * Spatial coordinate anchor for pills in the editor canvas.
 */
@Serializable
data class SpatialPoint(
    val x: Float,
    val y: Float
)

/**
 * 2D bounding rectangle in document canvas coordinates.
 */
@Serializable
data class SpatialRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = kotlin.math.max(0f, right - left)
    val height: Float get() = kotlin.math.max(0f, bottom - top)
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f
}

/**
 * Configuration and customization properties for an Actor Pill.
 * All pills are renameable and color-changeable.
 */
@Serializable
data class ActorPillConfig(
    val id: String,
    val role: ActorRole,
    val displayName: String,
    val colorArgb: Long,
    val defaultGlyph: GlyphType,
    val isRenameable: Boolean = true,
    val isColorChangeable: Boolean = true
) {
    companion object {
        const val DEFAULT_RED_ORCHESTRATOR = 0xFFEF4444L
        const val DEFAULT_BLUE_EDITOR = 0xFF3B82F6L
        const val DEFAULT_GREEN_AI = 0xFF10B981L

        fun createDefaultOrchestrator(
            id: String = "actor_orchestrator",
            name: String = "Sanelu"
        ) = ActorPillConfig(
            id = id,
            role = ActorRole.ORCHESTRATOR_DICTATION,
            displayName = name,
            colorArgb = DEFAULT_RED_ORCHESTRATOR,
            defaultGlyph = GlyphType.MIC
        )

        fun createDefaultEditor(
            id: String = "actor_editor",
            name: String = "Muokkain"
        ) = ActorPillConfig(
            id = id,
            role = ActorRole.DETERMINISTIC_EDITOR,
            displayName = name,
            colorArgb = DEFAULT_BLUE_EDITOR,
            defaultGlyph = GlyphType.TEXT_CARET
        )

        fun createDefaultAi(
            id: String = "actor_ai",
            name: String = "Tekoäly"
        ) = ActorPillConfig(
            id = id,
            role = ActorRole.GENERATIVE_AI,
            displayName = name,
            colorArgb = DEFAULT_GREEN_AI,
            defaultGlyph = GlyphType.SPARK
        )
    }
}

/**
 * Execution and animation state of an Actor Pill.
 */
sealed interface ActorPillStatus {
    /** Active but waiting: Ambient sinusoidal pulse at its anchor. */
    object WaitingAmbient : ActorPillStatus

    /** In flight: Gliding across the document surface to a new target. */
    data class InFlight(
        val from: SpatialPoint,
        val to: SpatialPoint,
        val progress: Float = 0f
    ) : ActorPillStatus

    /** Autonomous transit: High-speed snap relocation across document coordinates ("fast go, fast stop"). */
    data class AutonomousTransit(
        val from: SpatialPoint,
        val to: SpatialPoint,
        val progress: Float = 0f,
        val motionStretch: Float = 1.0f
    ) : ActorPillStatus

    /** Active and working: Processing, streaming tokens, or running pipeline. */
    data class WorkingGeneric(
        val progress: Float? = null,
        val statusMessage: String? = null
    ) : ActorPillStatus

    /** Active and working on THIS: Directional notch pointing at target text range. */
    data class WorkingOnTarget(
        val targetRange: TextRangeBounds,
        val glyph: GlyphType
    ) : ActorPillStatus

    /**
     * Cadence Staging: Human-friendly countdown before execution.
     * Displays a radial countdown ring and soft target highlight.
     */
    data class CadenceStaging(
        val targetRange: TextRangeBounds,
        val durationMs: Long,
        val remainingMs: Long,
        val progress: Float, // 1.0 down to 0.0
        val actionDescription: String,
        val glyph: GlyphType,
        val isPaused: Boolean = false
    ) : ActorPillStatus

    /** Commit Flash: 100ms crisp execution snap / feedback beat. */
    data class CommitFlash(
        val targetRange: TextRangeBounds
    ) : ActorPillStatus

    /** Holding Selection: Blue pill anchoring selected text in its shelf. */
    data class HoldingSelection(
        val selectedRange: TextRangeBounds,
        val wordCount: Int
    ) : ActorPillStatus

    /** Ambiguity / Disambiguation: Gentle wobble asking for user confirmation. */
    data class Disambiguation(
        val query: String
    ) : ActorPillStatus
}

/**
 * Event recorded for the 1-click Happenings HUD.
 */
@Serializable
data class HappeningsEvent(
    val id: String,
    val timestampMs: Long,
    val recognizedUtterance: String,
    val actorRole: ActorRole,
    val actorColorArgb: Long,
    val actionName: String,
    val details: String,
    val status: EventStatus
) {
    enum class EventStatus {
        STAGED,
        COMMITTED,
        ABORTED
    }
}

/**
 * Entry for the 2-click 50% split transcript drawer.
 */
@Serializable
data class TranscriptEntry(
    val id: String,
    val timestampMs: Long,
    val speaker: String,
    val text: String,
    val confidence: Float,
    val isFinal: Boolean
)

/**
 * Display modes for textual presentation anchored to an Actor Pill (Red Pill).
 */
@Serializable
enum class RedPillTextMode {
    /** No bubble displayed; compact resting pill. */
    NONE,

    /** Mode a: Warm beige-ish thought bubble with dashed outline showing ambient thought / basic message. */
    THOUGHT_BUBBLE,

    /** Mode b: Warm beige-ish question bubble where the pill turns into question symbol ('?'), with interactive input box. */
    QUESTION_BUBBLE,

    /** Mode c: Full blown conversation history drawer and entry box. */
    CONVERSATION_HISTORY
}

/**
 * Geometric shape for the indicator dot (pip) in the Actor Pill.
 */
@Serializable
enum class PipSymbolShape {
    CIRCLE,
    DIAMOND,
    STAR,
    QUESTION,
    CHECK,
    MIC,
    CLOCK
}

/**
 * Visual and operational state of the Red Pill's indicator pip.
 */
@Serializable
data class RedPillPipState(
    val colorArgb: Long = 0xFFEF4444L,
    val shape: PipSymbolShape = PipSymbolShape.CIRCLE,
    val isPulsing: Boolean = false
) {
    companion object {
        const val COLOR_IDLE_AMBER = 0xFFF59E0BL
        const val COLOR_IDLE_ROSE = 0xFFFF8A80L
        const val COLOR_LISTENING_RED = 0xFFEF4444L
        const val COLOR_THINKING_VIOLET = 0xFFA855F7L
        const val COLOR_QUESTION_GOLD = 0xFFFBBF24L
        const val COLOR_DONE_EMERALD = 0xFF10B981L

        fun idle() = RedPillPipState(
            colorArgb = COLOR_IDLE_ROSE,
            shape = PipSymbolShape.CIRCLE,
            isPulsing = true
        )

        fun listening() = RedPillPipState(
            colorArgb = COLOR_LISTENING_RED,
            shape = PipSymbolShape.MIC,
            isPulsing = true
        )

        fun thinking() = RedPillPipState(
            colorArgb = COLOR_THINKING_VIOLET,
            shape = PipSymbolShape.STAR,
            isPulsing = true
        )

        fun question() = RedPillPipState(
            colorArgb = COLOR_QUESTION_GOLD,
            shape = PipSymbolShape.QUESTION,
            isPulsing = false
        )

        fun done() = RedPillPipState(
            colorArgb = COLOR_DONE_EMERALD,
            shape = PipSymbolShape.CHECK,
            isPulsing = false
        )
    }
}

/**
 * A recorded conversation turn for the Red Pill's multi-turn history (Mode c).
 */
@Serializable
data class RedPillTurn(
    val id: String,
    val sender: String,
    val text: String,
    val timestampMs: Long,
    val isFromUser: Boolean,
    val executedTools: List<String> = emptyList()
)

/**
 * Complete UI state for the enhanced Red Pill.
 */
@Serializable
data class RedPillState(
    val textMode: RedPillTextMode = RedPillTextMode.NONE,
    val thoughtText: String = "",
    val questionPrompt: String = "",
    val pipState: RedPillPipState = RedPillPipState.idle(),
    val conversationHistory: List<RedPillTurn> = emptyList(),
    val isHovering: Boolean = true
)

/**
 * 3 distinct animation styles for how the Orchestrator (Red Pill) projects a new pill into existence.
 */
@Serializable
enum class PillProjectionStyle {
    /** Radiant laser conduit connecting source to target with photon pulse & collapse. */
    BEAM_TELEPORT,

    /** Elastic budding from orchestrator body, slingshot ejection, and damped bounce. */
    MITOSIS_EJECT,

    /** Quantum holographic wireframe reticle and scan-line sweeps assembling the pill. */
    HOLOGRAPHIC_MATERIALIZE
}

/**
 * Active pill projection/spawning event emitted by the orchestrator.
 */
@Serializable
data class PillProjectionEvent(
    val id: String,
    val sourcePoint: SpatialPoint,
    val targetPoint: SpatialPoint,
    val targetPillConfig: ActorPillConfig,
    val style: PillProjectionStyle,
    val durationMs: Long = 650L,
    val progress: Float = 0f
)

/**
 * Volumetric projection cone emitted by an actor pill (e.g. orange-ish cone) to highlight
 * a target object (word, phrase, or paraphrase) in the document.
 */
@Serializable
data class PillTargetCone(
    val pillPosition: SpatialPoint,
    val targetBounds: TextRangeBounds,
    val targetRect: SpatialRect,
    val colorArgb: Long = 0xFFF97316L, // Warm orange-ish cone default
    val targetLabel: String? = null,
    val isPulsing: Boolean = true
)

/**
 * Active projected worker pill spawned by the Red Pill Orchestrator.
 */
@Serializable
data class ProjectedPillInstance(
    val id: String,
    val name: String,
    val roleColorArgb: Long,
    val targetOffset: SpatialPoint,
    val style: PillProjectionStyle
)

/**
 * Target document object highlighted by the volumetric projection cone.
 */
@Serializable
data class TargetConeHighlight(
    val label: String,
    val targetOffset: SpatialPoint,
    val targetWidth: Float,
    val targetHeight: Float,
    val colorArgb: Long = 0xFFF97316L
)

/**
 * Recorded multi-turn interaction entry for Mode c (Conversation History).
 */
@Serializable
data class RedPillHistoryEntry(
    val id: String,
    val speaker: String,
    val text: String,
    val timestampMs: Long = 0L,
    val isFromUser: Boolean = false,
    val executedTools: List<String> = emptyList()
)
