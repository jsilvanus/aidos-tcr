package dev.aidos.tcr.actor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Orchestrates the temporal cadence for actor actions.
 * "Work, as a concept, requires time."
 *
 * Provides radial countdown progress, pause/resume (Spring & Freeze),
 * safety aborts, and staging state transitions.
 */
class CadenceEngine(
    private val scope: CoroutineScope,
    private val defaultTickIntervalMs: Long = 25L
) {
    private val _currentStatus = MutableStateFlow<ActorPillStatus>(ActorPillStatus.WaitingAmbient)
    val currentStatus: StateFlow<ActorPillStatus> = _currentStatus.asStateFlow()

    private var stagingJob: Job? = null

    // Paused state tracking
    private var activeTargetRange: TextRangeBounds? = null
    private var activeDurationMs: Long = 0L
    private var activeRemainingMs: Long = 0L
    private var activeDescription: String = ""
    private var activeGlyph: GlyphType = GlyphType.DELETE
    private var activeCommitAction: (suspend () -> Unit)? = null
    private var isCurrentlyPaused: Boolean = false

    /**
     * Determines duration in milliseconds based on operation severity.
     */
    fun calculateCadenceMs(severity: OperationSeverity): Long = when (severity) {
        OperationSeverity.DESTRUCTIVE -> 900L
        OperationSeverity.STRUCTURAL -> 500L
        OperationSeverity.MINOR -> 250L
    }

    /**
     * Stages an action with calibrated cadence.
     */
    fun stageAndExecute(
        targetRange: TextRangeBounds,
        severity: OperationSeverity,
        description: String,
        glyph: GlyphType,
        onCommit: suspend () -> Unit
    ): Job {
        abortCurrentAction()

        val totalDurationMs = calculateCadenceMs(severity)
        activeTargetRange = targetRange
        activeDurationMs = totalDurationMs
        activeRemainingMs = totalDurationMs
        activeDescription = description
        activeGlyph = glyph
        activeCommitAction = onCommit
        isCurrentlyPaused = false

        return startCountdown(totalDurationMs, totalDurationMs, targetRange, description, glyph, onCommit)
    }

    private fun startCountdown(
        totalDurationMs: Long,
        initialRemainingMs: Long,
        targetRange: TextRangeBounds,
        description: String,
        glyph: GlyphType,
        onCommit: suspend () -> Unit
    ): Job {
        val job = scope.launch {
            try {
                var remainingMs = initialRemainingMs
                while (remainingMs > 0) {
                    if (isCurrentlyPaused) {
                        delay(defaultTickIntervalMs)
                        continue
                    }

                    val progress = remainingMs.toFloat() / totalDurationMs.toFloat()
                    activeRemainingMs = remainingMs
                    _currentStatus.value = ActorPillStatus.CadenceStaging(
                        targetRange = targetRange,
                        durationMs = totalDurationMs,
                        remainingMs = remainingMs,
                        progress = progress,
                        actionDescription = description,
                        glyph = glyph,
                        isPaused = false
                    )
                    delay(defaultTickIntervalMs)
                    remainingMs -= defaultTickIntervalMs
                }

                // Countdown finished: Commit Flash beat
                _currentStatus.value = ActorPillStatus.CommitFlash(targetRange)
                delay(100L)

                // Execute mutation
                onCommit()

                // Reset to ambient waiting
                _currentStatus.value = ActorPillStatus.WaitingAmbient
            } catch (e: CancellationException) {
                // Aborted or cancelled cleanly
                _currentStatus.value = ActorPillStatus.WaitingAmbient
                throw e
            }
        }
        stagingJob = job
        return job
    }

    /**
     * Pauses the countdown ring (Spring & Freeze gesture at >= 20dp displacement).
     */
    fun pauseStaging() {
        val current = _currentStatus.value
        if (current is ActorPillStatus.CadenceStaging && !current.isPaused) {
            isCurrentlyPaused = true
            _currentStatus.value = current.copy(isPaused = true)
        }
    }

    /**
     * Resumes a paused countdown from where it froze.
     */
    fun resumeStaging() {
        val current = _currentStatus.value
        if (current is ActorPillStatus.CadenceStaging && current.isPaused) {
            isCurrentlyPaused = false
            _currentStatus.value = current.copy(isPaused = false)
        }
    }

    /**
     * Aborts the active staged action (Tear-to-Abort gesture at >= 80dp displacement).
     */
    fun abortCurrentAction() {
        stagingJob?.cancel()
        stagingJob = null
        isCurrentlyPaused = false
        activeCommitAction = null
        _currentStatus.value = ActorPillStatus.WaitingAmbient
    }

    /**
     * Reverts to ambient waiting.
     */
    fun resetToAmbient() {
        abortCurrentAction()
    }
}
