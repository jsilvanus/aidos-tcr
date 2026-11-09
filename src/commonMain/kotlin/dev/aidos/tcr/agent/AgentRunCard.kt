package dev.aidos.tcr.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.mesh.PresenceIndicator
import dev.aidos.tcr.model.PresenceState
import dev.aidos.tcr.theme.TcrColors

/**
 * Molecular TCR Component: AgentRunCard
 *
 * Renders a complete Agent Run state card:
 * - Execution state (Running, Awaiting Tools, Completed, Failed, Cancelled)
 * - Owner App identifier badge
 * - Model Placement & Locality ceiling
 * - RFC-0042 Egress summary (outside network calls or air-gapped badge)
 * - Reasoning steps progress & prompt preview
 * - Pending tool calls & executed tool history
 * - Cancellation action
 */
@Composable
fun AgentRunCard(
    run: AgentRunUiModel,
    onCancel: ((String) -> Unit)? = null,
    onInspect: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showTools by remember { mutableStateOf(false) }

    val statusBorderColor = when (run.status) {
        AgentRunStatus.RUNNING -> TcrColors.EmeraldGreen.copy(alpha = 0.5f)
        AgentRunStatus.TOOL_CALLS -> TcrColors.AmberWarning.copy(alpha = 0.6f)
        AgentRunStatus.COMPLETED -> Color.LightGray.copy(alpha = 0.3f)
        AgentRunStatus.FAILED -> TcrColors.EmberRed.copy(alpha = 0.4f)
        AgentRunStatus.CANCELLED -> Color.Gray.copy(alpha = 0.2f)
    }

    val presenceState = when (run.status) {
        AgentRunStatus.RUNNING -> PresenceState.ONLINE
        AgentRunStatus.TOOL_CALLS -> PresenceState.BUSY
        AgentRunStatus.COMPLETED -> PresenceState.IDLE
        AgentRunStatus.FAILED, AgentRunStatus.CANCELLED -> PresenceState.OFFLINE
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, statusBorderColor, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Status indicator + Run ID + Owner App + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresenceIndicator(
                        presence = presenceState,
                        pulseAnimation = run.status == AgentRunStatus.RUNNING
                    )

                    // Owner App Badge
                    Box(
                        modifier = Modifier
                            .background(TcrColors.VioletProtocol.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = run.ownerApp,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TcrColors.VioletProtocol
                        )
                    }

                    // Agent Role Badge (RFC-0011 Driver / Worker)
                    AgentRoleBadge(
                        role = run.role,
                        parentSessionId = run.parentSessionId,
                        workerBrief = run.workerBrief
                    )

                    // Run ID
                    Text(
                        text = "ID: ${run.runId.take(12)}…",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Step Counter
                    Text(
                        text = "Step ${run.steps}/${run.maxSteps}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )

                    // Cancel button if non-terminal
                    if (!run.status.isTerminal && onCancel != null) {
                        OutlinedButton(
                            onClick = { onCancel(run.runId) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(text = "Cancel", fontSize = 11.sp, color = TcrColors.EmberRed)
                        }
                    }
                }
            }

            // Model Placement Badge (RFC-0104)
            AgentPlacementBadge(
                modelId = run.modelId,
                placement = run.placement,
                locality = run.locality
            )

            // Prompt Preview
            if (run.promptPreview.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = run.promptPreview,
                        fontSize = 12.sp,
                        color = Color(0xFF1F2937),
                        maxLines = 2
                    )
                }
            }

            // Pending Tool Call (if awaiting client response)
            if (run.status == AgentRunStatus.TOOL_CALLS && run.pendingToolCall != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                        .border(1.dp, TcrColors.AmberWarning, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⏳", fontSize = 14.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Awaiting Tool Call: ${run.pendingToolCall.toolName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Waiting for host or client application to return results",
                            fontSize = 10.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }

            // Parked on Child Worker Run (RFC-0011 continuation)
            if (run.isParkedOnChild) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7).copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFD97706), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⏸️", fontSize = 14.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Parked on Subagent Worker",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Awaiting child worker run ${run.awaitingChildRunId?.take(12) ?: "execution"} to finish before resuming.",
                            fontSize = 10.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }

            // Task Graph (DAG) Progress Summary (RFC-0019)
            if (run.tasks.isNotEmpty()) {
                val completedTasks = run.tasks.count { it.state == "COMPLETED" }
                val skippedTasks = run.tasks.count { it.state == "SKIPPED" }
                val totalTasks = run.tasks.size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "⚡", fontSize = 12.sp)
                        Text(
                            text = "Execution Graph: $completedTasks/$totalTasks completed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )
                    }
                    if (skippedTasks > 0) {
                        Text(
                            text = "($skippedTasks skipped)",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Worker Outcome Commit Summary (RFC-0011)
            run.workerOutcome?.let { outcome ->
                WorkerOutcomeCard(outcome = outcome)
            }

            // Outside Connections / Egress Telemetry (RFC-0042)
            AgentEgressSummary(egressSummary = run.egressSummary)

            // Executed Tools & Results
            if (run.executedTools.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTools = !showTools },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🛠️ ${run.executedTools.size} tool calls executed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4B5563)
                    )
                    Text(
                        text = if (showTools) "Hide ▲" else "View Tools ▼",
                        fontSize = 11.sp,
                        color = TcrColors.ElectricBlue
                    )
                }

                AnimatedVisibility(visible = showTools) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        run.executedTools.forEach { tool ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF9FAFB), RoundedCornerShape(4.dp))
                                    .padding(6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = tool.toolName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TcrColors.ElectricBlue
                                    )
                                    Text(
                                        text = tool.resultText.take(120),
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error Message (if failed)
            if (run.status == AgentRunStatus.FAILED && !run.errorMessage.isNullOrBlank()) {
                Text(
                    text = "❌ Error: ${run.errorMessage}",
                    fontSize = 11.sp,
                    color = TcrColors.EmberRed,
                    fontWeight = FontWeight.Medium
                )
            }

            // Inspection footer link
            if (onInspect != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onInspect(run.runId) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Inspect Details →", fontSize = 11.sp, color = TcrColors.ElectricBlue)
                    }
                }
            }
        }
    }
}
