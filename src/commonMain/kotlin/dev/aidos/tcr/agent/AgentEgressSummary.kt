package dev.aidos.tcr.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.TcrColors

/**
 * Atomic TCR Component: AgentEgressSummary
 *
 * Visualizes RFC-0042 mediated egress telemetry:
 * - Air-Gapped seal when 0 outside calls were made
 * - Destination host, transfer volumes, and payload SHA-256 hashes when outside calls occurred.
 */
@Composable
fun AgentEgressSummary(
    egressSummary: AgentEgressSummaryUi,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (egressSummary.isAirGapped) {
            // Air-Gapped / Zero-Egress Verified Badge
            Row(
                modifier = Modifier
                    .background(TcrColors.EmeraldGreen.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                    .border(1.dp, TcrColors.EmeraldGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🛡️", fontSize = 14.sp)
                Column {
                    Text(
                        text = "Zero Egress · Air-Gapped Verified",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TcrColors.EmeraldGreen
                    )
                    Text(
                        text = "No outside network connections were made during this run.",
                        fontSize = 10.sp,
                        color = Color(0xFF047857)
                    )
                }
            }
        } else {
            // Outside Connections Summary
            val kbOut = (egressSummary.totalBytesOut / 1024f)
            val kbIn = (egressSummary.totalBytesIn / 1024f)

            Column(
                modifier = Modifier
                    .background(Color(0xFFFEF3C7).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .border(1.dp, TcrColors.AmberWarning.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🌐", fontSize = 14.sp)
                        Column {
                            Text(
                                text = "Outside Network Calls: ${egressSummary.totalCalls}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "${formatKb(kbOut)} sent · ${formatKb(kbIn)} received",
                                fontSize = 10.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                    Text(
                        text = if (expanded) "Hide ▲" else "Inspect ▼",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TcrColors.ElectricBlue
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        egressSummary.records.forEach { record ->
                            EgressRecordRow(record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EgressRecordRow(record: AgentEgressRecordUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = record.method,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TcrColors.ElectricBlue
                )
                Text(
                    text = record.destinationHost,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                )
            }
            if (record.payloadHash.isNotEmpty()) {
                Text(
                    text = "SHA-256: ${record.payloadHash.take(16)}…",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${record.durationMs}ms",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Text(
                text = record.outcome,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (record.outcome == "ALLOWED") TcrColors.EmeraldGreen else TcrColors.EmberRed
            )
        }
    }
}

private fun formatKb(kb: Float): String = if (kb < 1f) "${(kb * 1024).toInt()} B" else "%.1f KB".format(kb)
