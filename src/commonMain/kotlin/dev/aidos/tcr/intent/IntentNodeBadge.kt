package dev.aidos.tcr.intent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.TcrColors

/**
 * Status color and icon palette aligned with RFC-0110 & RFC-0111 intent state contracts.
 */
object IntentBadgeTokens {
    // Actor Colors
    val HumanGold = Color(0xFFF59E0B)
    val AgentCyan = Color(0xFF06B6D4)
    val HybridPurple = Color(0xFF8B5CF6)

    // Status Colors
    val StatusNotStarted = Color(0xFF64748B)
    val StatusInProgress = Color(0xFF0284C7)
    val StatusBlocked = Color(0xFFEF4444)
    val StatusNeedsReview = Color(0xFFF97316)
    val StatusDone = Color(0xFF10B981)
    val StatusStale = Color(0xFF94A3B8)
    val StatusConflicted = Color(0xFFE11D48)
}

/**
 * Pill badge displaying the derived execution status of an Intent node.
 */
@Composable
fun IntentStatusBadge(
    status: TcrDerivedStatus,
    modifier: Modifier = Modifier,
) {
    val (color, icon, label) = when (status) {
        TcrDerivedStatus.NOT_STARTED -> Triple(
            IntentBadgeTokens.StatusNotStarted,
            Icons.Default.HourglassEmpty,
            "Not Started"
        )
        TcrDerivedStatus.IN_PROGRESS -> Triple(
            IntentBadgeTokens.StatusInProgress,
            Icons.Default.Autorenew,
            "In Progress"
        )
        TcrDerivedStatus.BLOCKED -> Triple(
            IntentBadgeTokens.StatusBlocked,
            Icons.Default.Block,
            "Blocked"
        )
        TcrDerivedStatus.NEEDS_REVIEW -> Triple(
            IntentBadgeTokens.StatusNeedsReview,
            Icons.Default.RateReview,
            "Needs Review"
        )
        TcrDerivedStatus.DONE -> Triple(
            IntentBadgeTokens.StatusDone,
            Icons.Default.CheckCircle,
            "Done"
        )
        TcrDerivedStatus.STALE -> Triple(
            IntentBadgeTokens.StatusStale,
            Icons.Default.HourglassEmpty,
            "Stale"
        )
        TcrDerivedStatus.CONFLICTED -> Triple(
            IntentBadgeTokens.StatusConflicted,
            Icons.Default.Warning,
            "Conflicted"
        )
    }

    BaseChipBadge(
        label = label,
        color = color,
        icon = icon,
        modifier = modifier
    )
}

/**
 * Badge indicating actor ownership: HUMAN, AGENT, or HYBRID (RFC-0110).
 */
@Composable
fun IntentActorBadge(
    actorKind: TcrIntentActorKind,
    modifier: Modifier = Modifier,
) {
    val (color, icon, label) = when (actorKind) {
        TcrIntentActorKind.HUMAN -> Triple(
            IntentBadgeTokens.HumanGold,
            Icons.Default.Person,
            "Human"
        )
        TcrIntentActorKind.AGENT -> Triple(
            IntentBadgeTokens.AgentCyan,
            Icons.Default.SmartToy,
            "Agent"
        )
        TcrIntentActorKind.HYBRID -> Triple(
            IntentBadgeTokens.HybridPurple,
            Icons.Default.Sync,
            "Hybrid"
        )
    }

    BaseChipBadge(
        label = label,
        color = color,
        icon = icon,
        modifier = modifier
    )
}

/**
 * Chip indicating effective dispatch policy: AUTO_WHEN_READY vs MANUAL_START.
 */
@Composable
fun DispatchPolicyBadge(
    policy: TcrEffectiveDispatchPolicy,
    modifier: Modifier = Modifier,
) {
    val (color, icon, label) = when (policy) {
        TcrEffectiveDispatchPolicy.AUTO_WHEN_READY -> Triple(
            TcrColors.EmeraldGreen,
            Icons.Default.PlayArrow,
            "Auto Dispatch"
        )
        TcrEffectiveDispatchPolicy.MANUAL_START -> Triple(
            Color(0xFF6B7280),
            Icons.Default.Lock,
            "Manual Start"
        )
    }

    BaseChipBadge(
        label = label,
        color = color,
        icon = icon,
        modifier = modifier
    )
}

/**
 * Chip indicating decomposition granularity (GROUP, GOAL, SUB_GOAL, ATOMIC).
 */
@Composable
fun GranularityBadge(
    granularity: TcrIntentGranularity,
    modifier: Modifier = Modifier,
) {
    val (color, label) = when (granularity) {
        TcrIntentGranularity.ATOMIC -> Color(0xFF0284C7) to "Atomic Task"
        TcrIntentGranularity.SUB_GOAL -> Color(0xFF6366F1) to "Sub-Goal"
        TcrIntentGranularity.GOAL -> Color(0xFF8B5CF6) to "Goal"
        TcrIntentGranularity.GROUP -> Color(0xFF64748B) to "Group"
    }

    BaseChipBadge(
        label = label,
        color = color,
        modifier = modifier
    )
}

/**
 * Chip indicating target platform profile: MOBILE, DESKTOP, HEADLESS_SERVER, ANY.
 */
@Composable
fun TargetProfileBadge(
    profile: TcrIntentTargetProfile,
    modifier: Modifier = Modifier,
) {
    val (icon, label) = when (profile) {
        TcrIntentTargetProfile.MOBILE -> Icons.Default.Smartphone to "Mobile"
        TcrIntentTargetProfile.DESKTOP -> Icons.Default.Computer to "Desktop"
        TcrIntentTargetProfile.HEADLESS_SERVER -> Icons.Default.Storage to "Server"
        TcrIntentTargetProfile.ANY -> Icons.Default.Computer to "Any Platform"
    }

    BaseChipBadge(
        label = label,
        color = Color(0xFF475569),
        icon = icon,
        modifier = modifier
    )
}

/**
 * Chip indicating network locality requirement (LOCAL_ONLY, TRUSTED_CLUSTER, ANY).
 */
@Composable
fun MinLocalityBadge(
    locality: TcrIntentMinLocality,
    modifier: Modifier = Modifier,
) {
    val (color, label) = when (locality) {
        TcrIntentMinLocality.LOCAL_ONLY -> TcrColors.AmberWarning to "Air-Gapped (Local)"
        TcrIntentMinLocality.TRUSTED_CLUSTER -> TcrColors.ElectricBlue to "Trusted Cluster"
        TcrIntentMinLocality.ANY -> Color(0xFF64748B) to "Any Network"
    }

    BaseChipBadge(
        label = label,
        color = color,
        icon = Icons.Default.Lock,
        modifier = modifier
    )
}

@Composable
private fun BaseChipBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.35f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
