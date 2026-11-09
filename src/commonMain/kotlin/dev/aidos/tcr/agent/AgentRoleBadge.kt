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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.TcrColors

/**
 * Atomic TCR Component: AgentRoleBadge (RFC-0011)
 *
 * Visual chip representing whether an agent run is a Driver or delegated Worker.
 */
@Composable
fun AgentRoleBadge(
    role: AgentRoleUi,
    parentSessionId: String? = null,
    workerBrief: String? = null,
    modifier: Modifier = Modifier
) {
    val (badgeColor, bgColor) = when (role) {
        AgentRoleUi.DRIVER -> Pair(TcrColors.VioletProtocol, TcrColors.VioletProtocol.copy(alpha = 0.12f))
        AgentRoleUi.WORKER -> Pair(Color(0xFFD97706), Color(0xFFFEF3C7).copy(alpha = 0.7f))
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Role Chip
        Box(
            modifier = Modifier
                .background(bgColor, RoundedCornerShape(4.dp))
                .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = role.badgeEmoji, fontSize = 11.sp)
                Text(
                    text = role.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }

        // Parent Link / Worker Brief Chip if Worker
        if (role == AgentRoleUi.WORKER && parentSessionId != null) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.04f), RoundedCornerShape(4.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Subagent of ${parentSessionId.take(8)}...",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }
        }

        if (workerBrief != null && workerBrief.isNotBlank()) {
            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Task: $workerBrief",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = badgeColor,
                    maxLines = 1
                )
            }
        }
    }
}
