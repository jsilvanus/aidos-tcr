package dev.aidos.tcr.paradigm.kanban

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import dev.aidos.tcr.intent.TcrIntentNodeUiModel
import dev.aidos.tcr.theme.AidosTypography
import dev.aidos.tcr.theme.CornerTokens
import dev.aidos.tcr.theme.SpacingTokens
import dev.aidos.tcr.theme.colorForTcrActor
import dev.aidos.tcr.theme.colorForTcrStatus

@Composable
fun TaskCartridgeView(
    title: String,
    actorKind: TcrIntentActorKind,
    status: TcrDerivedStatus,
    estimatedSteps: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = colorForTcrStatus(status)
    val actorColor = colorForTcrActor(actorKind)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), shape = RoundedCornerShape(CornerTokens.sm))
            .border(width = 1.dp, color = Color(0xFF334155), shape = RoundedCornerShape(CornerTokens.sm))
            .clickable(onClick = onClick)
            .padding(SpacingTokens.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // LED Status indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor, shape = CircleShape)
                    )
                    Text(
                        text = status.name,
                        style = AidosTypography.baseTypography.labelSmall,
                        color = statusColor,
                    )
                }

                // Actor badge
                Box(
                    modifier = Modifier
                        .background(actorColor.copy(alpha = 0.2f), shape = RoundedCornerShape(CornerTokens.xs))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = actorKind.name,
                        style = AidosTypography.baseTypography.labelSmall,
                        color = actorColor,
                    )
                }
            }

            Text(
                text = title,
                style = AidosTypography.baseTypography.titleSmall,
                color = Color.White,
            )

            if (estimatedSteps != null) {
                StepCounterAtom(currentStep = 1, totalSteps = estimatedSteps)
            }
        }
    }
}

@Composable
fun TaskCartridgeView(
    node: TcrIntentNodeUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TaskCartridgeView(
        title = node.title,
        actorKind = node.actorKind,
        status = node.status,
        estimatedSteps = node.estimatedSteps,
        onClick = onClick,
        modifier = modifier,
    )
}
