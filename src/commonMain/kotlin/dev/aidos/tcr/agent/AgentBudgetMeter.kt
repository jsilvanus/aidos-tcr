package dev.aidos.tcr.agent

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.engine.formatElapsed
import dev.aidos.tcr.theme.TcrColors
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Atomic TCR Component: AgentBudgetMeter (RFC-0028 & RFC-0011)
 *
 * Visualizes capability budget allocation and quota consumption:
 * - Reasoning steps spent vs ceiling
 * - Model calls and token bounds
 * - Budget delegation splits (fan-out protection)
 */
@Composable
fun AgentBudgetMeter(
    budget: AgentBudgetUi,
    currentSteps: Int = 0,
    modifier: Modifier = Modifier
) {
    val maxSteps = budget.maxSteps ?: budget.steps ?: 8
    val stepsFraction = if (maxSteps > 0) (currentSteps.toFloat() / maxSteps).coerceIn(0f, 1f) else 0f

    val progressColor = when {
        stepsFraction >= 0.9f -> TcrColors.EmberRed
        stepsFraction >= 0.7f -> TcrColors.AmberWarning
        else -> TcrColors.EmeraldGreen
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "📊", fontSize = 12.sp)
                Text(
                    text = "Delegated Budget & Spend Ceiling",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
            }

            if (budget.splitWays != null && budget.splitWays > 1) {
                Box(
                    modifier = Modifier
                        .background(TcrColors.ElectricBlue.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .border(1.dp, TcrColors.ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Split ${budget.splitWays} ways",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TcrColors.ElectricBlue
                    )
                }
            }
        }

        // Steps Progress Bar
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Reasoning Steps",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Text(
                    text = "$currentSteps / $maxSteps steps (${(stepsFraction * 100).toInt()}%)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
            }

            LinearProgressIndicator(
                progress = { stepsFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = progressColor,
                trackColor = Color.LightGray.copy(alpha = 0.3f),
            )
        }

        // Quota chips (Model calls, Tokens, Cost)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            budget.modelCalls?.let { calls ->
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Max Calls: $calls",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray
                    )
                }
            }

            budget.inputTokens?.let { tokens ->
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Tokens: ${tokens / 1000}k cap",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray
                    )
                }
            }

            budget.costUnits?.let { cost ->
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Cost Cap: $cost units",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

/**
 * Usage and locality telemetry for an agent run and project-wide totals.
 */
@Serializable
data class AgentUsageMeterUi(
    val currentSteps: Int = 0,
    val maxSteps: Int = 8,
    val tokensUsed: Long = 0L,
    val tokenCap: Long? = null,
    val costUnits: Long = 0L,
    val localCalls: Long = 0L,
    val remoteCalls: Long = 0L,
    val projectTotalCalls: Long = 0L,
    val projectTotalTokens: Long = 0L,
    val projectCostUnits: Long = 0L,
    val lastUsageSummary: String? = null,
    val lastUsageTimestampMs: Long? = null,
) {
    val totalCalls: Long get() = localCalls + remoteCalls
    val localRatio: Float get() = if (totalCalls > 0) (localCalls.toFloat() / totalCalls).coerceIn(0f, 1f) else 1f
    val costLabel: String get() = if (costUnits == 0L) "Local · free" else "$costUnits units"
}

private enum class AgentMeterView(val label: String) {
    ACTIVE_RUN("Active Run"),
    PROJECT_TOTALS("Project Totals"),
    LOCAL_VS_REMOTE("Local vs Remote");

    fun next(): AgentMeterView = when (this) {
        ACTIVE_RUN -> PROJECT_TOTALS
        PROJECT_TOTALS -> LOCAL_VS_REMOTE
        LOCAL_VS_REMOTE -> ACTIVE_RUN
    }
}

/**
 * Formats [AgentUsageMeterUi] last-usage line relative to [nowMs].
 */
fun formatAgentLastUsage(usage: AgentUsageMeterUi, nowMs: Long): String {
    val summary = usage.lastUsageSummary ?: return "No usage yet"
    val ts = usage.lastUsageTimestampMs ?: return "Last: $summary"
    val elapsed = formatElapsed((nowMs - ts).coerceAtLeast(0L))
    return "Last: $summary · $elapsed"
}

/**
 * Atomic TCR Component: AgentMeterGauge
 *
 * Interactive usage and budget gauge for agent runs:
 * - Active run steps, tokens, and cost gauge (displaying "Local · free" when costUnits == 0L)
 * - Local-vs-remote call ratio bar
 * - Small-font last-usage line
 * - Tap to cycle inline view (Active Run / Project Totals / Local vs Remote)
 * - Long-press or info button to open Usage & Budget Details dialog with JSON/CSV export
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AgentMeterGauge(
    usage: AgentUsageMeterUi,
    modifier: Modifier = Modifier,
    onExportJson: () -> Unit = {},
    onExportCsv: () -> Unit = {},
) {
    var inlineView by remember { mutableStateOf(AgentMeterView.ACTIVE_RUN) }
    var showDetails by remember { mutableStateOf(false) }

    val stepsFraction = if (usage.maxSteps > 0) (usage.currentSteps.toFloat() / usage.maxSteps).coerceIn(0f, 1f) else 0f
    val progressColor = when {
        stepsFraction >= 0.9f -> TcrColors.EmberRed
        stepsFraction >= 0.7f -> TcrColors.AmberWarning
        else -> TcrColors.EmeraldGreen
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF9FAFB))
            .border(1.dp, Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { inlineView = inlineView.next() },
                onLongClick = { showDetails = true },
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = inlineView.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = usage.costLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (usage.costUnits == 0L) TcrColors.EmeraldGreen else Color(0xFF374151),
                )
                IconButton(
                    onClick = { showDetails = true },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Usage & budget details",
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray,
                    )
                }
            }
        }

        when (inlineView) {
            AgentMeterView.ACTIVE_RUN -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Steps: ${usage.currentSteps} / ${usage.maxSteps}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray,
                        )
                        val tokenText = usage.tokenCap?.let { "${usage.tokensUsed} / $it tok" } ?: "${usage.tokensUsed} tok"
                        Text(
                            text = tokenText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { stepsFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = progressColor,
                        trackColor = Color.LightGray.copy(alpha = 0.3f),
                    )
                    LocalVsRemoteBar(usage)
                }
            }

            AgentMeterView.PROJECT_TOTALS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${usage.projectTotalCalls} calls · ${usage.projectTotalTokens} tok",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray,
                    )
                    Text(
                        text = if (usage.projectCostUnits == 0L) "Local · free" else "${usage.projectCostUnits} units",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray,
                    )
                }
            }

            AgentMeterView.LOCAL_VS_REMOTE -> {
                LocalVsRemoteBar(usage)
            }
        }

        Text(
            text = formatAgentLastUsage(usage, Clock.System.now().toEpochMilliseconds()),
            fontSize = 10.sp,
            color = Color.Gray,
        )
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Usage & Budget Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Active Run", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Steps: ${usage.currentSteps} / ${usage.maxSteps} · Tokens: ${usage.tokensUsed} · Cost: ${usage.costLabel}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text("Locality", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Local calls: ${usage.localCalls} · Remote calls: ${usage.remoteCalls} (${(usage.localRatio * 100).toInt()}% local)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text("Project Totals", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Calls: ${usage.projectTotalCalls} · Tokens: ${usage.projectTotalTokens} · Cost: ${if (usage.projectCostUnits == 0L) "Local · free" else "${usage.projectCostUnits} units"}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onExportJson) { Text("Export JSON") }
                    TextButton(onClick = onExportCsv) { Text("Export CSV") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun LocalVsRemoteBar(usage: AgentUsageMeterUi) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Local ${usage.localCalls} · Remote ${usage.remoteCalls}",
                fontSize = 10.sp,
                color = Color.Gray,
            )
            Text(
                text = "${(usage.localRatio * 100).toInt()}% local",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.DarkGray,
            )
        }
        LinearProgressIndicator(
            progress = { usage.localRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = TcrColors.EmeraldGreen,
            trackColor = TcrColors.AmberWarning.copy(alpha = 0.5f),
        )
    }
}
