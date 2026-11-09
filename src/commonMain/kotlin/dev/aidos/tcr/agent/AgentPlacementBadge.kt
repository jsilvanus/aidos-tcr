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
 * Atomic TCR Component: AgentPlacementBadge
 *
 * Displays the model name, execution placement (Local, Cluster, Remote),
 * and enforced locality limit according to RFC-0104.
 */
@Composable
fun AgentPlacementBadge(
    modelId: String,
    placement: AgentPlacement,
    locality: AgentLocality = AgentLocality.LOCAL_ONLY,
    modifier: Modifier = Modifier
) {
    val (placementColor, placementBg, icon) = when (placement) {
        AgentPlacement.LOCAL -> Triple(TcrColors.EmeraldGreen, TcrColors.EmeraldGreen.copy(alpha = 0.12f), "🟢")
        AgentPlacement.CLUSTER -> Triple(TcrColors.ElectricBlue, TcrColors.ElectricBlue.copy(alpha = 0.12f), "🔵")
        AgentPlacement.REMOTE -> Triple(TcrColors.VioletProtocol, TcrColors.VioletProtocol.copy(alpha = 0.12f), "🟣")
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Model Name Chip
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "🤖 $modelId",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
        }

        // Placement Chip
        Box(
            modifier = Modifier
                .background(placementBg, RoundedCornerShape(4.dp))
                .border(1.dp, placementColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$icon ${placement.label}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = placementColor
            )
        }

        // Locality Ceiling Chip
        Box(
            modifier = Modifier
                .background(Color.Gray.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Ceiling: ${locality.label}",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}
