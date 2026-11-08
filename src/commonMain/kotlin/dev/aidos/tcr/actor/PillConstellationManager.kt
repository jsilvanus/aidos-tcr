package dev.aidos.tcr.actor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the multi-pill constellation (Option B: Red, Blue, Green, and Custom pills).
 * Orchestrates interactions, click gestures, telemetry logging, and UI overlays.
 */
class PillConstellationManager(
    private val scope: CoroutineScope,
    initialOrchestratorName: String = "Sanelu",
    initialEditorName: String = "Muokkain",
    initialAiName: String = "Tekoäly"
) {
    // Configurations (renameable & color-changeable)
    private val _orchestratorConfig = MutableStateFlow(
        ActorPillConfig.createDefaultOrchestrator(name = initialOrchestratorName)
    )
    val orchestratorConfig: StateFlow<ActorPillConfig> = _orchestratorConfig.asStateFlow()

    private val _editorConfig = MutableStateFlow(
        ActorPillConfig.createDefaultEditor(name = initialEditorName)
    )
    val editorConfig: StateFlow<ActorPillConfig> = _editorConfig.asStateFlow()

    private val _aiConfig = MutableStateFlow(
        ActorPillConfig.createDefaultAi(name = initialAiName)
    )
    val aiConfig: StateFlow<ActorPillConfig> = _aiConfig.asStateFlow()

    private val _customPills = MutableStateFlow<Map<String, ActorPillConfig>>(emptyMap())
    val customPills: StateFlow<Map<String, ActorPillConfig>> = _customPills.asStateFlow()

    // Subsystems
    val cadenceEngine = CadenceEngine(scope)
    val selectionHolder = ActorSelectionHolder()

    // Statuses
    private val _orchestratorStatus = MutableStateFlow<ActorPillStatus>(ActorPillStatus.WaitingAmbient)
    val orchestratorStatus: StateFlow<ActorPillStatus> = _orchestratorStatus.asStateFlow()

    val editorStatus: StateFlow<ActorPillStatus> = cadenceEngine.currentStatus

    private val _aiStatus = MutableStateFlow<ActorPillStatus>(ActorPillStatus.WaitingAmbient)
    val aiStatus: StateFlow<ActorPillStatus> = _aiStatus.asStateFlow()

    // Overlay visibility states
    private val _isHappeningsHudVisible = MutableStateFlow(false)
    val isHappeningsHudVisible: StateFlow<Boolean> = _isHappeningsHudVisible.asStateFlow()

    private val _isTranscriptDrawerOpen = MutableStateFlow(false)
    val isTranscriptDrawerOpen: StateFlow<Boolean> = _isTranscriptDrawerOpen.asStateFlow()

    // Telemetry & Logs
    private val _happenings = MutableStateFlow<List<HappeningsEvent>>(emptyList())
    val happenings: StateFlow<List<HappeningsEvent>> = _happenings.asStateFlow()

    private val _transcript = MutableStateFlow<List<TranscriptEntry>>(emptyList())
    val transcript: StateFlow<List<TranscriptEntry>> = _transcript.asStateFlow()

    // Active Projection & Target Cone
    private val _activeProjection = MutableStateFlow<PillProjectionEvent?>(null)
    val activeProjection: StateFlow<PillProjectionEvent?> = _activeProjection.asStateFlow()

    private val _activeTargetCone = MutableStateFlow<PillTargetCone?>(null)
    val activeTargetCone: StateFlow<PillTargetCone?> = _activeTargetCone.asStateFlow()

    private val _orchestratorPosition = MutableStateFlow(SpatialPoint(120f, 120f))
    val orchestratorPosition: StateFlow<SpatialPoint> = _orchestratorPosition.asStateFlow()

    // --- Gestures & Interactivity ---

    /**
     * Single-click on a pill: Toggles the Happenings HUD hovering over the document.
     */
    fun onPillSingleClick(pillId: String) {
        _isHappeningsHudVisible.update { !it }
    }

    /**
     * Double-click on a pill: Opens the full Transcript Drawer in half of the screen.
     */
    fun onPillDoubleClick(pillId: String) {
        _isTranscriptDrawerOpen.update { !it }
    }

    fun dismissHappeningsHud() {
        _isHappeningsHudVisible.value = false
    }

    fun closeTranscriptDrawer() {
        _isTranscriptDrawerOpen.value = false
    }

    // --- Customization ---

    fun renamePill(pillId: String, newName: String) {
        when (pillId) {
            _orchestratorConfig.value.id -> _orchestratorConfig.update { it.copy(displayName = newName) }
            _editorConfig.value.id -> _editorConfig.update { it.copy(displayName = newName) }
            _aiConfig.value.id -> _aiConfig.update { it.copy(displayName = newName) }
            else -> _customPills.update { current ->
                current[pillId]?.let { current + (pillId to it.copy(displayName = newName)) } ?: current
            }
        }
    }

    fun changePillColor(pillId: String, newColorArgb: Long) {
        when (pillId) {
            _orchestratorConfig.value.id -> _orchestratorConfig.update { it.copy(colorArgb = newColorArgb) }
            _editorConfig.value.id -> _editorConfig.update { it.copy(colorArgb = newColorArgb) }
            _aiConfig.value.id -> _aiConfig.update { it.copy(colorArgb = newColorArgb) }
            else -> _customPills.update { current ->
                current[pillId]?.let { current + (pillId to it.copy(colorArgb = newColorArgb)) } ?: current
            }
        }
    }

    fun registerCustomPill(config: ActorPillConfig) {
        _customPills.update { it + (config.id to config) }
    }

    fun unregisterCustomPill(pillId: String) {
        _customPills.update { it - pillId }
    }

    // --- Telemetry Logging ---

    fun logHappening(
        recognizedUtterance: String,
        actorRole: ActorRole,
        actorColorArgb: Long,
        actionName: String,
        details: String,
        status: HappeningsEvent.EventStatus
    ) {
        val event = HappeningsEvent(
            id = "event_${System.currentTimeMillis()}",
            timestampMs = System.currentTimeMillis(),
            recognizedUtterance = recognizedUtterance,
            actorRole = actorRole,
            actorColorArgb = actorColorArgb,
            actionName = actionName,
            details = details,
            status = status
        )
        _happenings.update { listOf(event) + it.take(49) }
    }

    fun appendTranscript(
        speaker: String,
        text: String,
        confidence: Float = 0.95f,
        isFinal: Boolean = true
    ) {
        val entry = TranscriptEntry(
            id = "tr_${System.currentTimeMillis()}",
            timestampMs = System.currentTimeMillis(),
            speaker = speaker,
            text = text,
            confidence = confidence,
            isFinal = isFinal
        )
        _transcript.update { it + entry }
    }

    // --- Projections & Target Cones ---

    /**
     * Trigger a new worker pill projection animation from the Orchestrator to a target canvas coordinate.
     */
    fun projectPill(
        targetConfig: ActorPillConfig,
        targetPoint: SpatialPoint,
        style: PillProjectionStyle,
        durationMs: Long = 650L
    ) {
        _activeProjection.value = PillProjectionEvent(
            id = "proj_${System.currentTimeMillis()}",
            sourcePoint = _orchestratorPosition.value,
            targetPoint = targetPoint,
            targetPillConfig = targetConfig,
            style = style,
            durationMs = durationMs
        )
    }

    fun clearActiveProjection() {
        _activeProjection.value = null
    }

    /**
     * Highlight a target document word/phrase with a volumetric projection cone.
     */
    fun showTargetCone(
        targetBounds: TextRangeBounds,
        targetRect: SpatialRect,
        label: String? = null,
        colorArgb: Long = 0xFFF97316L
    ) {
        _activeTargetCone.value = PillTargetCone(
            pillPosition = _orchestratorPosition.value,
            targetBounds = targetBounds,
            targetRect = targetRect,
            colorArgb = colorArgb,
            targetLabel = label
        )
    }

    fun dismissTargetCone() {
        _activeTargetCone.value = null
    }

    /**
     * Relocate the orchestrator anchor (e.g. from caret movements or autonomous snap transit).
     */
    fun updateOrchestratorPosition(point: SpatialPoint) {
        _orchestratorPosition.value = point
    }

    fun triggerAutonomousTransit(targetPoint: SpatialPoint) {
        val currentPoint = _orchestratorPosition.value
        _orchestratorStatus.value = ActorPillStatus.AutonomousTransit(
            from = currentPoint,
            to = targetPoint,
            progress = 0f
        )
        _orchestratorPosition.value = targetPoint
    }
}
