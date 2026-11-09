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
 * Atomic TCR Component: WorkerOutcomeCard (RFC-0011)
 *
 * Visualizes treeless worker Git commit outcome summary on refs/aidos/workers/<workerSessionId>.
 */
@Composable
fun WorkerOutcomeCard(
    outcome: WorkerOutcomeUi,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
            .border(1.dp, TcrColors.EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                Text(text = "🌿", fontSize = 12.sp)
                Text(
                    text = "Treeless Worker Commit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF065F46)
                )
            }

            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.dp, TcrColors.EmeraldGreen.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = outcome.commitRef,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF047857)
                )
            }
        }

        Text(
            text = outcome.outcomeSummary,
            fontSize = 11.sp,
            color = Color(0xFF064E3B),
            lineHeight = 16.sp
        )
    }
}
