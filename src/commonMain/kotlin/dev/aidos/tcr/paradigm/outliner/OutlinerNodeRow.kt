package dev.aidos.tcr.paradigm.outliner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.aidos.tcr.atom.code.StepCounterAtom
import dev.aidos.tcr.intent.TcrDerivedStatus
import dev.aidos.tcr.intent.TcrIntentActorKind
import dev.aidos.tcr.theme.AidosTypography
import dev.aidos.tcr.theme.ColorTokens
import dev.aidos.tcr.theme.CornerTokens
import dev.aidos.tcr.theme.SpacingTokens
import dev.aidos.tcr.theme.colorForTcrActor
import dev.aidos.tcr.theme.colorForTcrStatus

data class TcrOutlinerNodeItem(
    val id: String,
    val title: String,
    val actorKind: TcrIntentActorKind = TcrIntentActorKind.AGENT,
    val derivedStatus: TcrDerivedStatus = TcrDerivedStatus.NOT_STARTED,
    val estimatedSteps: Int? = null,
    val depth: Int = 0,
    val isExpanded: Boolean = true,
    val hasChildren: Boolean = false,
    val unmetPrerequisites: List<Pair<String, String>> = emptyList(), // Pair(id, title)
)

@Composable
fun OutlinerNodeRow(
    item: TcrOutlinerNodeItem,
    onToggleExpand: (String) -> Unit,
    onNodeClick: (String) -> Unit,
    onCausalJump: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actorColor = colorForTcrActor(item.actorKind)
    val statusColor = colorForTcrStatus(item.derivedStatus)
    val indentPadding = (item.depth * 20).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNodeClick(item.id) }
            .padding(start = indentPadding, end = SpacingTokens.base, top = 4.dp, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Expand / collapse arrow if children exist
            if (item.hasChildren) {
                Box(
                    modifier = Modifier
                        .clickable { onToggleExpand(item.id) }
                        .padding(SpacingTokens.xxs)
                ) {
                    Text(
                        text = if (item.isExpanded) "▼" else "▶",
                        style = AidosTypography.baseTypography.labelSmall,
                        color = Color(0xFF94A3B8),
                    )
                }
            } else {
                Box(modifier = Modifier.size(16.dp))
            }

            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = statusColor, shape = CircleShape)
            )

            // Actor pill indicator
            Box(
                modifier = Modifier
                    .background(actorColor.copy(alpha = 0.2f), shape = RoundedCornerShape(CornerTokens.xs))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = item.actorKind.name.take(1),
                    style = AidosTypography.baseTypography.labelSmall,
                    color = actorColor,
                )
            }

            // Title
            Text(
                text = item.title,
                style = AidosTypography.baseTypography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )

            // Step Counter
            if (item.estimatedSteps != null) {
                StepCounterAtom(
                    currentStep = 1,
                    totalSteps = item.estimatedSteps,
                )
            }
        }

        // Causal Jump Pills for unmet prerequisites
        if (item.unmetPrerequisites.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                modifier = Modifier
                    .padding(start = 32.dp, top = SpacingTokens.xxs)
            ) {
                item.unmetPrerequisites.forEach { (prereqId, prereqTitle) ->
                    CausalJumpPill(
                        prerequisiteId = prereqId,
                        prerequisiteTitle = prereqTitle,
                        onJump = onCausalJump,
                    )
                }
            }
        }
    }
}
