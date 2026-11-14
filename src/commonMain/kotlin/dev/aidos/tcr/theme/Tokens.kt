package dev.aidos.tcr.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aidos.tcr.intent.TcrDerivedStatus
import dev.aidos.tcr.intent.TcrIntentActorKind

/**
 * Spatial and dimensional tokens for the Trusted Component Registry (RFC-0106, RFC-0112).
 */
object SpacingTokens {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val base: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp

    // Android accessibility guideline: minimum interactive touch target is 48dp
    val minTouchTarget: Dp = 48.dp
}

object CornerTokens {
    val none: Dp = 0.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val pill: Dp = 999.dp
}

object ElevationTokens {
    val level0: Dp = 0.dp
    val level1: Dp = 1.dp
    val level2: Dp = 3.dp
    val level3: Dp = 6.dp
    val level4: Dp = 8.dp
}

object ColorTokens {
    // Actor Semantic Colors
    val ActorHuman: Color = Color(0xFFF59E0B)       // Warm Amber/Gold
    val ActorAgent: Color = Color(0xFF06B6D4)       // Electric Cyan
    val ActorHybrid: Color = Color(0xFF8B5CF6)      // Amethyst Purple

    // Status Semantic Colors
    val StatusReview: Color = Color(0xFFF97316)     // Warning Flame (NEEDS_REVIEW)
    val StatusBlocked: Color = Color(0xFFEF4444)    // Muted Crimson (BLOCKED)
    val StatusDone: Color = Color(0xFF10B981)       // Emerald Green (DONE)
    val StatusInProgress: Color = Color(0xFF3B82F6) // Vibrant Blue (IN_PROGRESS)
    val StatusNotStarted: Color = Color(0xFF6B7280) // Neutral Slate (NOT_STARTED)
    val StatusStale: Color = Color(0xFF9CA3AF)      // Dim Gray (STALE)
    val StatusConflicted: Color = Color(0xFFDC2626) // Alert Red (CONFLICTED)

    // Subagent Pill Visual Identities
    val PillOrchestratorRed: Color = Color(0xFFEF4444)
    val PillWorkerCodeBlue: Color = Color(0xFF3B82F6)
    val PillVerifierGreen: Color = Color(0xFF10B981)
    val PillResearchViolet: Color = Color(0xFF8B5CF6)

    // Line Lock Washes & Waves
    val BlueWashReservation: Color = Color(0x1F3B82F6)
    val WaveRippleEmission: Color = Color(0x6606B6D4)

    // OLED Dark / High Contrast Neutrals
    val OledBlack: Color = Color(0xFF000000)
    val SurfaceDark: Color = Color(0xFF0F172A)
    val SurfaceCardDark: Color = Color(0xFF1E293B)
    val SurfaceLight: Color = Color(0xFFF8FAFC)
    val SurfaceCardLight: Color = Color(0xFFFFFFFF)
}

fun colorForTcrActor(actorKind: TcrIntentActorKind): Color = when (actorKind) {
    TcrIntentActorKind.HUMAN -> ColorTokens.ActorHuman
    TcrIntentActorKind.AGENT -> ColorTokens.ActorAgent
    TcrIntentActorKind.HYBRID -> ColorTokens.ActorHybrid
}

fun colorForTcrStatus(status: TcrDerivedStatus): Color = when (status) {
    TcrDerivedStatus.NOT_STARTED -> ColorTokens.StatusNotStarted
    TcrDerivedStatus.IN_PROGRESS -> ColorTokens.StatusInProgress
    TcrDerivedStatus.BLOCKED -> ColorTokens.StatusBlocked
    TcrDerivedStatus.NEEDS_REVIEW -> ColorTokens.StatusReview
    TcrDerivedStatus.DONE -> ColorTokens.StatusDone
    TcrDerivedStatus.STALE -> ColorTokens.StatusStale
    TcrDerivedStatus.CONFLICTED -> ColorTokens.StatusConflicted
}
