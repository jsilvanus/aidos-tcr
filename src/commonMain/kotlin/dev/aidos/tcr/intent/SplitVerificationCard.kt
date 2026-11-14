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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Terminal
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
 * Organism TCR Component: SplitVerificationCard
 *
 * Desktop shell test trigger and verification review card.
 * Governed by RFC-0110 (§Coding Management: Atomic Decomposition & Split Verification).
 *
 * Invariant:
 * When a constrained Mobile agent completes work requiring SHELL_COMMAND or platform-restricted checks
 * (unavailable on Android per RFC-0049), the node parks in NEEDS_REVIEW. Downstream dependencies remain
 * BLOCKED until this Desktop card triggers verification and confirms the implementation.
 */
@Composable
fun SplitVerificationCard(
    verification: TcrSplitVerificationUiModel,
    modifier: Modifier = Modifier,
    onRunVerification: (nodeId: String, checkSpec: String) -> Unit = { _, _ -> },
    onConfirm: (nodeId: String) -> Unit = {},
    onReject: (nodeId: String, reason: String?) -> Unit = { _, _ -> },
) {
    var isRejecting by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    val outcome = verification.outcome
    val borderColor = when (outcome) {
        TcrVerificationOutcome.PASSED -> TcrColors.EmeraldGreen
        TcrVerificationOutcome.FAILED -> TcrColors.EmberRed
        TcrVerificationOutcome.RUNNING -> TcrColors.ElectricBlue
        TcrVerificationOutcome.PENDING -> TcrColors.AmberWarning
    }

    val bannerBg = when (outcome) {
        TcrVerificationOutcome.PASSED -> Color(0xFFD1FAE5)
        TcrVerificationOutcome.FAILED -> Color(0xFFFEE2E2)
        TcrVerificationOutcome.RUNNING -> Color(0xFFE0F2FE)
        TcrVerificationOutcome.PENDING -> Color(0xFFFEF3C7)
    }

    val bannerFg = when (outcome) {
        TcrVerificationOutcome.PASSED -> Color(0xFF065F46)
        TcrVerificationOutcome.FAILED -> Color(0xFF991B1B)
        TcrVerificationOutcome.RUNNING -> Color(0xFF0369A1)
        TcrVerificationOutcome.PENDING -> Color(0xFF92400E)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.5.dp, color = borderColor.copy(alpha = 0.8f), shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Strip
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
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = bannerFg,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RFC-0110 Split Verification Gate",
                        color = bannerFg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Origin -> Target Device Path
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(bannerFg.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = bannerFg,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(" Mobile", color = bannerFg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = bannerFg,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = null,
                        tint = bannerFg,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(" Desktop", color = bannerFg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Task Title and ID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = verification.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Node: ${verification.nodeId} • Project: ${verification.projectId}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    IntentStatusBadge(status = verification.status)
                }

                // Scope Paths
                if (verification.scopePaths.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Files:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        verification.scopePaths.take(2).forEach { path ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = path,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Check Command Box
                Text(
                    text = "DESKTOP VERIFICATION COMMAND (${verification.checkKind})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E2E))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "$ ${verification.checkSpec.ifBlank { "./gradlew check" }}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFA6E3A1),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Verification Output / Console
                if (verification.isVerifying) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(TcrColors.ElectricBlue.copy(alpha = 0.08f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = TcrColors.ElectricBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Executing verification command on Desktop host...",
                            fontSize = 11.sp,
                            color = TcrColors.ElectricBlue
                        )
                    }
                } else if (!verification.outputLog.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF181825))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = verification.outputLog,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (outcome == TcrVerificationOutcome.PASSED) Color(0xFFA6E3A1) else Color(0xFFF38BA8),
                            maxLines = 4
                        )
                    }
                }

                // Rejection Prompt
                AnimatedVisibility(visible = isRejecting) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = rejectReason,
                            onValueChange = { rejectReason = it },
                            label = { Text("Reason for rejecting implementation") },
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
                                    onReject(verification.nodeId, rejectReason.ifBlank { null })
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TcrColors.EmberRed)
                            ) {
                                Text("Confirm Rejection")
                            }
                        }
                    }
                }

                // Action Buttons
                if (!isRejecting) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { isRejecting = true },
                            enabled = !verification.isVerifying,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = TcrColors.EmberRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject / Fix", color = TcrColors.EmberRed, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { onRunVerification(verification.nodeId, verification.checkSpec) },
                            enabled = !verification.isVerifying,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (outcome == TcrVerificationOutcome.PENDING) Icons.Default.PlayArrow else Icons.Default.Refresh,
                                contentDescription = null,
                                tint = TcrColors.ElectricBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (outcome == TcrVerificationOutcome.PENDING) "Run Check" else "Re-run Check",
                                color = TcrColors.ElectricBlue,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onConfirm(verification.nodeId) },
                            enabled = !verification.isVerifying && outcome != TcrVerificationOutcome.FAILED,
                            colors = ButtonDefaults.buttonColors(containerColor = TcrColors.EmeraldGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Confirm & Done", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
