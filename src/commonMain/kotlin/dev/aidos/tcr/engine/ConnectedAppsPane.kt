package dev.aidos.tcr.engine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Organism TCR Component: ConnectedAppsPane
 *
 * Connected Apps approval & authorization management pane (RFC-0103, Phase D).
 *
 * Displays caller apps requesting Engine access, grouped by status:
 * - Pending user authorization
 * - Approved apps with revoke capability
 * - Denied apps with undo-deny capability
 */
@Composable
fun ConnectedAppsPane(
    approvals: List<AppApprovalRecordUi>,
    isEngineRunning: Boolean,
    onApprove: (String) -> Unit,
    onDeny: (String) -> Unit,
    onRevoke: (String) -> Unit,
    onUndoDeny: (String) -> Unit,
    modifier: Modifier = Modifier,
    notRunningText: String = "The Engine is not running. Start it from Home to manage app access.",
    /** D49: grant or withdraw an approved app's agent access; null hides the control. */
    onSetAgentAllowed: ((packageName: String, allowed: Boolean) -> Unit)? = null,
    /** RFC-0104: set or clear a per-app locality cap; null hides the control. */
    onSetLocalityLimit: ((packageName: String, limit: String?) -> Unit)? = null,
) {
    val pending = approvals.filter { it.status == AppApprovalStatus.PENDING }
    val approved = approvals.filter { it.status == AppApprovalStatus.APPROVED }
    val denied = approvals.filter { it.status == AppApprovalStatus.DENIED }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!isEngineRunning) {
            item { EmptyNote(notRunningText) }
            return@LazyColumn
        }

        if (pending.isNotEmpty()) {
            item { SectionTitle("Approval Pending", Color(0xFFEAB308)) }
            items(pending, key = { it.packageName }) { app ->
                AppCard(app) {
                    TextButton(onClick = { onApprove(app.packageName) }) {
                        Text("Approve", color = Color(0xFF22C55E), fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { onDeny(app.packageName) }) {
                        Text("Deny", color = Color(0xFFEF4444))
                    }
                }
            }
        }

        if (approved.isNotEmpty()) {
            item { SectionTitle("Approved", Color(0xFF22C55E)) }
            items(approved, key = { it.packageName }) { app ->
                AppCard(app) {
                    if (onSetLocalityLimit != null) {
                        TextButton(onClick = { onSetLocalityLimit(app.packageName, nextLocalityLimit(app.localityLimit)) }) {
                            Text("Locality: ${localityLimitLabel(app.localityLimit)}")
                        }
                    }
                    if (onSetAgentAllowed != null) {
                        TextButton(onClick = { onSetAgentAllowed(app.packageName, !app.agentAllowed) }) {
                            Text(if (app.agentAllowed) "Withdraw agent access" else "Allow agent runs")
                        }
                    }
                    TextButton(onClick = { onRevoke(app.packageName) }) {
                        Text("Revoke", color = Color(0xFFEF4444))
                    }
                }
            }
        }

        if (denied.isNotEmpty()) {
            item { SectionTitle("Denied", Color(0xFFEF4444)) }
            items(denied, key = { it.packageName }) { app ->
                AppCard(app) {
                    TextButton(onClick = { onUndoDeny(app.packageName) }) {
                        Text("Undo Deny")
                    }
                }
            }
        }

        if (approvals.isEmpty()) {
            item {
                EmptyNote("No apps have requested access yet. Apps using the Aidos SDK will appear here for approval when they connect.")
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun EmptyNote(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AppCard(app: AppApprovalRecordUi, actions: @Composable () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(app.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                app.packageName + clientVersionsSuffix(app),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            val stats = buildList {
                add("${app.attemptCount} connection${if (app.attemptCount == 1) "" else "s"}")
                if (app.requestCount > 0) add("${app.requestCount} request${if (app.requestCount == 1) "" else "s"}")
                if (app.firstSeenAt.isNotBlank()) add("First seen: ${app.firstSeenAt}")
            }.joinToString(" · ")
            Text(
                stats,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (app.localityLimit != null || app.localityRefusalCount > 0) {
                Text(
                    "Locality cap: ${localityLimitLabel(app.localityLimit)} · locality refusals: ${app.localityRefusalCount}" +
                        (app.lastLocalityRefusalAt?.let { " (last $it)" } ?: ""),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            // FlowRow so three actions (locality, agent access, revoke) wrap on a phone instead of clipping.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
                content = { actions() },
            )
        }
    }
}

internal fun clientVersionsSuffix(app: AppApprovalRecordUi): String {
    val sdk = app.sdkVersion?.let { "sdk v$it" }
    val api = app.apiVersion?.let { "api $it" }
    val versions = listOfNotNull(sdk, api).joinToString(", ")
    return if (versions.isEmpty()) "" else " · $versions"
}

internal fun localityLimitLabel(limit: String?): String = when (limit) {
    "local_only" -> "local only"
    "trusted_cluster" -> "trusted cluster"
    "any" -> "any"
    else -> "engine default"
}

/** Cycles the per-app cap: default → local only → trusted cluster → any → default. */
internal fun nextLocalityLimit(current: String?): String? = when (current) {
    null -> "local_only"
    "local_only" -> "trusted_cluster"
    "trusted_cluster" -> "any"
    else -> null
}
