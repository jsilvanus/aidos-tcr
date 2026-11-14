package dev.aidos.tcr.intent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.TcrColors

/**
 * Molecular TCR Component: IntentTaskCard
 *
 * Rich card representing an Intent Graph node across Outliner, Kanban, and Pipeline views.
 * Reflects RFC-0110 & RFC-0111 Dual-Mode Intent Graph presentation contracts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntentTaskCard(
    node: TcrIntentNodeUiModel,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    isSelected: Boolean = false,
    onClick: ((String) -> Unit)? = null,
    onComplete: ((String) -> Unit)? = null,
    onDispatch: ((String) -> Unit)? = null,
    onReview: ((String) -> Unit)? = null,
) {
    val actorColor = when (node.actorKind) {
        TcrIntentActorKind.HUMAN -> IntentBadgeTokens.HumanGold
        TcrIntentActorKind.AGENT -> IntentBadgeTokens.AgentCyan
        TcrIntentActorKind.HYBRID -> IntentBadgeTokens.HybridPurple
    }

    val statusBorder = when (node.status) {
        TcrDerivedStatus.DONE -> IntentBadgeTokens.StatusDone.copy(alpha = 0.4f)
        TcrDerivedStatus.BLOCKED -> IntentBadgeTokens.StatusBlocked.copy(alpha = 0.4f)
        TcrDerivedStatus.NEEDS_REVIEW -> IntentBadgeTokens.StatusNeedsReview.copy(alpha = 0.6f)
        TcrDerivedStatus.IN_PROGRESS -> IntentBadgeTokens.StatusInProgress.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, actorColor, RoundedCornerShape(12.dp))
                } else {
                    Modifier.border(1.dp, statusBorder, RoundedCornerShape(12.dp))
                }
            )
            .clickable(enabled = onClick != null) { onClick?.invoke(node.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Actor Accent Strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (isCompact) 64.dp else 112.dp)
                    .background(actorColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isCompact) 8.dp else 12.dp)
            ) {
                // Header: Badges Row
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IntentStatusBadge(status = node.status)
                    IntentActorBadge(actorKind = node.actorKind)
                    if (!isCompact) {
                        GranularityBadge(granularity = node.granularity)
                        DispatchPolicyBadge(policy = node.dispatchPolicy)
                        node.targetProfile?.let { TargetProfileBadge(profile = it) }
                        node.minLocality?.let { MinLocalityBadge(locality = it) }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title
                Text(
                    text = node.title,
                    fontSize = if (isCompact) 13.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (node.status == TcrDerivedStatus.DONE) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (node.status == TcrDerivedStatus.DONE) {
                        TextDecoration.LineThrough
                    } else {
                        null
                    },
                    maxLines = if (isCompact) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Optional Description (expanded mode)
                if (!isCompact && !node.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = node.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Scope Paths & Coding Metadata
                if (!isCompact && node.scopePaths.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        node.scopePaths.take(3).forEach { path ->
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
                        if (node.scopePaths.size > 3) {
                            Text(
                                text = "+${node.scopePaths.size - 3} more",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Extra Metadata Row (dueAt, estimatedSteps)
                if (!isCompact && (node.estimatedSteps != null || node.dueAt != null)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        node.estimatedSteps?.let { steps ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$steps steps budget",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        node.dueAt?.let { due ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = due,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // Blocked Warning Banner
                if (node.status == TcrDerivedStatus.BLOCKED && node.unmetDependencyIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(TcrColors.EmberRed.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = TcrColors.EmberRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Blocked by ${node.unmetDependencyIds.size} pending dependency",
                            fontSize = 11.sp,
                            color = TcrColors.EmberRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Active Run Spinner Banner
                if (node.activeRunIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(TcrColors.ElectricBlue.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = TcrColors.ElectricBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Agent active (run ${node.activeRunIds.first().take(8)}...)",
                            fontSize = 11.sp,
                            color = TcrColors.ElectricBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action Buttons
                val onCompleteAction = onComplete
                val onDispatchAction = onDispatch
                val onReviewAction = onReview

                val showComplete = onCompleteAction != null &&
                    (node.actorKind == TcrIntentActorKind.HUMAN || node.actorKind == TcrIntentActorKind.HYBRID) &&
                    node.status != TcrDerivedStatus.DONE
                val showDispatch = onDispatchAction != null &&
                    (node.actorKind == TcrIntentActorKind.AGENT || node.actorKind == TcrIntentActorKind.HYBRID) &&
                    node.status == TcrDerivedStatus.NOT_STARTED
                val showReview = onReviewAction != null && node.status == TcrDerivedStatus.NEEDS_REVIEW

                if (showComplete || showDispatch || showReview) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showReview) {
                            Button(
                                onClick = { onReviewAction(node.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IntentBadgeTokens.StatusNeedsReview
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RateReview,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Review & Signoff", fontSize = 12.sp)
                            }
                        }

                        if (showDispatch) {
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = { onDispatchAction(node.id) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dispatch Agent", fontSize = 12.sp)
                            }
                        }

                        if (showComplete) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { onCompleteAction(node.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IntentBadgeTokens.HumanGold
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Complete Task", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
