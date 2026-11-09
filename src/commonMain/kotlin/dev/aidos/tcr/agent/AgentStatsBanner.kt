package dev.aidos.tcr.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.TcrColors

/**
 * Organism TCR Component: AgentStatsBanner
 *
 * Displays top-level Agent Runtime metrics:
 * - Active agents vs concurrency capacity
 * - Runs awaiting tool inputs
 * - Client apps count
 * - Outside network connections tally
 */
@Composable
fun AgentStatsBanner(
    stats: AgentRuntimeOverviewStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Active Capacity Card
        StatCard(
            title = "Active Runs",
            value = "${stats.activeRuns} / ${stats.maxConcurrentRuns}",
            caption = "Concurrency slot usage",
            accentColor = if (stats.activeRuns > 0) TcrColors.EmeraldGreen else Color.Gray,
            modifier = Modifier.weight(1f)
        )

        // Waiting on Tools Card
        StatCard(
            title = "Awaiting Tools",
            value = stats.waitingToolCalls.toString(),
            caption = "Pending client IPC",
            accentColor = if (stats.waitingToolCalls > 0) TcrColors.AmberWarning else Color.Gray,
            modifier = Modifier.weight(1f)
        )

        // Client Apps Card
        StatCard(
            title = "Connected Apps",
            value = stats.appsCount.toString(),
            caption = "Initiating clients",
            accentColor = TcrColors.VioletProtocol,
            modifier = Modifier.weight(1f)
        )

        // Outside Calls Card
        StatCard(
            title = "Outside Calls",
            value = stats.totalEgressCalls.toString(),
            caption = if (stats.totalEgressCalls == 0) "100% Air-Gapped" else "Network egress",
            accentColor = if (stats.totalEgressCalls == 0) TcrColors.EmeraldGreen else TcrColors.ElectricBlue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    caption: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Text(text = caption, fontSize = 9.sp, color = Color.Gray)
        }
    }
}
