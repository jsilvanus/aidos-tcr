package dev.aidos.tcr.frontdoors.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.aidos.tcr.atom.code.ScopePathChipAtom
import dev.aidos.tcr.atom.code.StepCounterAtom
import dev.aidos.tcr.atom.core.ActionButtonAtom
import dev.aidos.tcr.atom.core.ActionButtonKind
import dev.aidos.tcr.intent.TcrDerivedStatus
import dev.aidos.tcr.theme.AidosTypography
import dev.aidos.tcr.theme.ColorTokens
import dev.aidos.tcr.theme.CornerTokens
import dev.aidos.tcr.theme.SpacingTokens

data class TcrActionItemUiModel(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: TcrDerivedStatus = TcrDerivedStatus.NOT_STARTED,
    val projectName: String,
    val isLifeProject: Boolean = false,
    val batonDirection: BatonTransferDirection = BatonTransferDirection.AGENT_TO_HUMAN,
    val scopePaths: List<String> = emptyList(),
    val estimatedSteps: Int? = null,
)

@Composable
fun ActionItemCard(
    item: TcrActionItemUiModel,
    onComplete: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = if (item.isLifeProject) ColorTokens.ActorHuman else ColorTokens.ActorAgent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .background(Color(0xFF1E293B), shape = RoundedCornerShape(CornerTokens.md))
            .border(width = 1.dp, color = accentColor.copy(alpha = 0.4f), shape = RoundedCornerShape(CornerTokens.md))
            .clickable(onClick = onClick)
            .padding(SpacingTokens.base)
            .semantics {
                contentDescription = "Action item: ${item.title}, Status: ${item.status.name}"
            }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BatonHandoffBadge(direction = item.batonDirection)

                    Text(
                        text = if (item.isLifeProject) "🛡 [${item.projectName}]" else "💻 [${item.projectName}]",
                        style = AidosTypography.baseTypography.labelSmall,
                        color = Color(0xFF94A3B8),
                    )
                }

                ActionButtonAtom(
                    text = "Complete",
                    onClick = { onComplete(item.id) },
                    kind = ActionButtonKind.TACTILE_MICRO,
                )
            }

            Text(
                text = item.title,
                style = AidosTypography.baseTypography.titleMedium,
                color = Color.White,
            )

            if (!item.description.isNullOrBlank()) {
                Text(
                    text = item.description,
                    style = AidosTypography.baseTypography.bodySmall,
                    color = Color(0xFF94A3B8),
                )
            }

            if (item.scopePaths.isNotEmpty() || item.estimatedSteps != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xs)) {
                        item.scopePaths.take(2).forEach { path ->
                            ScopePathChipAtom(path = path)
                        }
                    }

                    if (item.estimatedSteps != null) {
                        StepCounterAtom(
                            currentStep = 1,
                            totalSteps = item.estimatedSteps,
                        )
                    }
                }
            }
        }
    }
}
