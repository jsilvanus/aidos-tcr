package dev.aidos.tcr.atom.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.aidos.tcr.atom.core.MetricBadgeAtom
import dev.aidos.tcr.theme.SpacingTokens

data class MetricItem(
    val label: String,
    val value: String,
    val accentColor: Color = Color(0xFF06B6D4),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetricClusterMolecule(
    metrics: List<MetricItem>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
    ) {
        metrics.forEach { metric ->
            MetricBadgeAtom(
                label = metric.label,
                value = metric.value,
                accentColor = metric.accentColor,
            )
        }
    }
}
