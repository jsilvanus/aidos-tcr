package dev.aidos.tcr.intent

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import dev.aidos.tcr.theme.TcrColors

/**
 * Organism TCR Component: EgressGateCard
 *
 * Explicit cryptographic declassification gate card for zero-egress Life message delivery.
 * Governed by RFC-0113 §4.3 (The Blinded Egress Bridge).
 *
 * Invariant:
 * Messages originating from air-gapped personal/LIFE projects are quarantined in memory.
 * No network socket or provider may receive the payload until a single-use HMAC declassification
 * token is signed by the human operator.
 */
@Composable
fun EgressGateCard(
    parkedMessage: TcrParkedOutboundMessage,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onApprove: (messageId: String) -> Unit = {},
    onReject: (messageId: String, reason: String?) -> Unit = { _, _ -> },
) {
    var isRejecting by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    val status = parkedMessage.status
    val request = parkedMessage.request

    val borderColor = when (status) {
        TcrParkedMessageStatus.PARKED -> TcrColors.AmberWarning
        TcrParkedMessageStatus.APPROVED -> TcrColors.EmeraldGreen
        TcrParkedMessageStatus.TRANSMITTED -> Color(0xFF0284C7)
        TcrParkedMessageStatus.REJECTED -> TcrColors.EmberRed
    }

    val bannerBg = when (status) {
        TcrParkedMessageStatus.PARKED -> Color(0xFFFEF3C7) // Soft amber
        TcrParkedMessageStatus.APPROVED -> Color(0xFFD1FAE5) // Soft emerald
        TcrParkedMessageStatus.TRANSMITTED -> Color(0xFFE0F2FE) // Soft sky
        TcrParkedMessageStatus.REJECTED -> Color(0xFFFEE2E2) // Soft red
    }

    val bannerFg = when (status) {
        TcrParkedMessageStatus.PARKED -> Color(0xFF92400E)
        TcrParkedMessageStatus.APPROVED -> Color(0xFF065F46)
        TcrParkedMessageStatus.TRANSMITTED -> Color(0xFF0369A1)
        TcrParkedMessageStatus.REJECTED -> Color(0xFF991B1B)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.5.dp, color = borderColor.copy(alpha = 0.8f), shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Shield + RFC-0113 Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bannerBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (status) {
                            TcrParkedMessageStatus.PARKED -> Icons.Default.Security
                            TcrParkedMessageStatus.APPROVED -> Icons.Default.CheckCircle
                            TcrParkedMessageStatus.TRANSMITTED -> Icons.AutoMirrored.Filled.Send
                            TcrParkedMessageStatus.REJECTED -> Icons.Default.Block
                        },
                        contentDescription = null,
                        tint = bannerFg,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (status) {
                            TcrParkedMessageStatus.PARKED -> "RFC-0113 Blinded Egress Gate • Pending Approval"
                            TcrParkedMessageStatus.APPROVED -> "RFC-0113 Declassified • HMAC Token Signed"
                            TcrParkedMessageStatus.TRANSMITTED -> "RFC-0113 Egress Transmitted"
                            TcrParkedMessageStatus.REJECTED -> "RFC-0113 Egress Blocked & Discarded"
                        },
                        color = bannerFg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Air-Gapped Indicator Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(bannerFg.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ZERO-EGRESS",
                        color = bannerFg,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Metadata Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PROJECT & DOMAIN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${request.projectId} (${request.projectType.uppercase()})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CHANNEL & DESTINATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${request.channel} -> ${request.recipient}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Taint & Reason Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TcrColors.AmberWarning.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = TcrColors.AmberWarning,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Taint: ${request.taintLevel}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TcrColors.AmberWarning
                        )
                    }

                    if (!request.reason.isNullOrBlank()) {
                        Text(
                            text = "Reason: ${request.reason}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payload Preview Monospace Box
                Text(
                    text = "OUTBOUND MESSAGE PAYLOAD (${request.payload.length} chars)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        text = request.payload,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Security Invariant Guarantee Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Blinded Invariant: Held in local memory. Packets blocked until HMAC-SHA256 signature is verified.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // If Rejection details present
                if (status == TcrParkedMessageStatus.REJECTED && !parkedMessage.rejectionReason.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rejection Reason: ${parkedMessage.rejectionReason}",
                        fontSize = 11.sp,
                        color = TcrColors.EmberRed
                    )
                }

                // Rejection prompt input
                AnimatedVisibility(visible = isRejecting) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = rejectReason,
                            onValueChange = { rejectReason = it },
                            label = { Text("Rejection reason (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = { isRejecting = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    isRejecting = false
                                    onReject(parkedMessage.id, rejectReason.ifBlank { null })
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TcrColors.EmberRed)
                            ) {
                                Text("Confirm Block")
                            }
                        }
                    }
                }

                // Action Bar for PARKED state
                if (status == TcrParkedMessageStatus.PARKED && !isRejecting) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { isRejecting = true },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = TcrColors.EmberRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject & Discard", color = TcrColors.EmberRed, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = { onApprove(parkedMessage.id) },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = TcrColors.EmeraldGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Signing HMAC Grant...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sign & Release Egress", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
