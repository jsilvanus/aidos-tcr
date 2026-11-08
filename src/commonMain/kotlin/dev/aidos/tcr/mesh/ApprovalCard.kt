package dev.aidos.tcr.mesh

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.model.DisclosureStatus

/**
 * Molecular TCR component: ApprovalCard
 *
 * 1-Tap Progressive Disclosure Authorization Card.
 */
@Composable
fun ApprovalCard(
    requestId: String,
    topicKey: String,
    promptText: String,
    proposedValue: String? = null,
    status: DisclosureStatus = DisclosureStatus.PENDING,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onApprove: (requestId: String) -> Unit = {},
    onDecline: (requestId: String) -> Unit = {}
) {
    val borderColor = when (status) {
        DisclosureStatus.PENDING -> Color(0xFFF59E0B) // Amber
        DisclosureStatus.APPROVED -> Color(0xFF10B981) // Emerald
        DisclosureStatus.DECLINED -> Color(0xFF6B7280) // Gray
    }

    val bannerBg = when (status) {
        DisclosureStatus.PENDING -> Color(0xFFFEF3C7)
        DisclosureStatus.APPROVED -> Color(0xFFD1FAE5)
        DisclosureStatus.DECLINED -> Color(0xFFF3F4F6)
    }

    val bannerFg = when (status) {
        DisclosureStatus.PENDING -> Color(0xFF92400E)
        DisclosureStatus.APPROVED -> Color(0xFF065F46)
        DisclosureStatus.DECLINED -> Color(0xFF374151)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.5.dp,
                color = borderColor.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bannerBg)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (status) {
                        DisclosureStatus.PENDING -> "⚡ Action Required"
                        DisclosureStatus.APPROVED -> "✅ Approved & Disclosed"
                        DisclosureStatus.DECLINED -> "✕ Declined"
                    },
                    color = bannerFg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• Topic: $topicKey",
                    color = bannerFg.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
            }

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = promptText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (proposedValue != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Proposed data to disclose:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = proposedValue,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions
                if (status == DisclosureStatus.PENDING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onApprove(requestId) },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "Share Data",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { onDecline(requestId) },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Decline",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (status == DisclosureStatus.APPROVED) {
                            "Encrypted envelope turn dispatched to requesting agent."
                        } else {
                            "Request declined. Zero data was shared."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
