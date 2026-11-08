package dev.aidos.tcr.mesh

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.model.MeshProvenanceType

/**
 * Molecular TCR component: CalendarCopilotCard
 *
 * Implements Scenario 3: Inline Calendar Co-Pilot.
 * Renders Aidos's automated calendar availability check and offers
 * a 1-tap quick action to commit the calendar event and respond to the contact.
 */
@Composable
fun CalendarCopilotCard(
    eventTitle: String,
    proposedTime: String,
    conflictSummary: String? = null,
    isActionTaken: Boolean = false,
    modifier: Modifier = Modifier,
    onConfirmAction: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProvenanceBadge(
                    provenanceType = MeshProvenanceType.AGENT_RUNNER,
                    label = "Aidos Calendar Co-Pilot"
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "📅 Smart Suggestion",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Proposed: $eventTitle at $proposedTime",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (conflictSummary != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conflictSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isActionTaken) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFD1FAE5))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "📅 Calendar updated & confirmed with contact",
                        color = Color(0xFF065F46),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Button(
                    onClick = onConfirmAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Confirm & Update Calendar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
