package dev.aidos.tcr.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.TcrColors

/**
 * Molecular TCR Component: AgentTaskGraphView (RFC-0019 & RFC-0011)
 *
 * Renders the execution graph DAG:
 * - Tasks in dependency/ordinal sequence with status badges
 * - Cascading SKIPPED indicators when prerequisites fail
 * - DEPENDS_ON edge links between tasks
 * - COMPOSITE task subagent worker delegation cards with links to child runs
 */
@Composable
fun AgentTaskGraphView(
    tasks: List<TaskGraphNodeUi>,
    workerSpawns: List<WorkerSpawnUi> = emptyList(),
    onInspectRun: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No explicit tasks recorded for this run.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tasks.sortedBy { it.ordinal }.forEachIndexed { index, task ->
            val spawn = workerSpawns.firstOrNull { it.taskId == task.taskId }
            val hasDependencies = task.dependsOnTaskIds.isNotEmpty()

            val statusColor = when (task.state) {
                "COMPLETED" -> TcrColors.EmeraldGreen
                "RUNNING" -> TcrColors.ElectricBlue
                "AWAITING_INPUT", "AWAITING_APPROVAL" -> Color(0xFFD97706)
                "SKIPPED" -> Color.Gray
                "FAILED" -> TcrColors.EmberRed
                else -> Color.DarkGray
            }

            val cardBg = when (task.state) {
                "COMPLETED" -> Color(0xFFF0FDF4)
                "RUNNING" -> Color(0xFFEFF6FF)
                "AWAITING_INPUT" -> Color(0xFFFFFBEB)
                "SKIPPED" -> Color(0xFFF9FAFB)
                "FAILED" -> Color(0xFFFEF2F2)
                else -> Color.White
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Task Row Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Ordinal circle
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(statusColor.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${task.ordinal}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }

                            // Task Kind
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = task.kind,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF374151)
                                )
                            }

                            // Task Description
                            Text(
                                text = task.description,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (task.isSkipped) Color.Gray else Color(0xFF111827),
                                textDecoration = if (task.isSkipped) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (task.state) {
                                    "COMPLETED" -> "✓ Completed"
                                    "RUNNING" -> "▶ Running"
                                    "AWAITING_INPUT" -> "⏸ Parked"
                                    "SKIPPED" -> "⏭ Skipped"
                                    "FAILED" -> "✕ Failed"
                                    else -> task.state
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    // Dependencies line
                    if (hasDependencies) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "↳ Depends on:", fontSize = 10.sp, color = Color.Gray)
                            task.dependsOnTaskIds.forEach { depId ->
                                Box(
                                    modifier = Modifier
                                        .background(Color.Gray.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = depId,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF4B5563)
                                    )
                                }
                            }
                        }
                    }

                    // Cascaded Skip Note
                    if (task.isSkipped) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = "Cascaded skip: Prerequisite task failed or skipped (RFC-0019)",
                                fontSize = 10.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    // Composite Worker Delegation Box
                    if (spawn != null || task.kind == "COMPOSITE") {
                        val brief = spawn?.brief ?: task.description
                        val split = spawn?.budgetSplitWays ?: 1

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF3C7).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFD97706).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "🛠️ Subagent Worker Spawn", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                    if (split > 1) {
                                        Text(text = "(budget split $split ways)", fontSize = 9.sp, color = Color(0xFF92400E))
                                    }
                                }

                                if (spawn?.childRunId != null && onInspectRun != null) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White, RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0xFFD97706).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .clickable { onInspectRun(spawn.childRunId) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Inspect Child Run ↗",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Brief: $brief",
                                fontSize = 10.sp,
                                color = Color(0xFF78350F)
                            )

                            if (spawn?.delegatedCapabilities?.isNotEmpty() == true) {
                                Text(
                                    text = "Delegated: ${spawn.delegatedCapabilities.joinToString()}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
