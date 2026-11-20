package dev.aidos.tcr.engine

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock

private enum class UsageInlineView(val label: String) {
    CURRENT_LOAD("Current Load"),
    SESSION_TOTALS("Session Totals"),
    LIFETIME_TOTALS("Lifetime Totals");

    fun next(): UsageInlineView = when (this) {
        CURRENT_LOAD -> SESSION_TOTALS
        SESSION_TOTALS -> LIFETIME_TOTALS
        LIFETIME_TOTALS -> CURRENT_LOAD
    }
}

/**
 * Organism TCR Component: StatusPane
 *
 * Engine status, resident models, memory budget, and load/usage dashboard:
 * - Engine running/stopped state banner with host-specific control toggle
 * - Resident memory models list
 * - Device RAM budget gauge
 * - Live concurrency load & session/lifetime usage telemetry
 * - Free storage volume telemetry
 * - Inference Groups peer link counts (RFC-0105)
 * - Authorized connected caller apps tally
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusPane(
    state: StatusUiState,
    engineControl: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onExportUsageJson: () -> Unit = {},
    onExportUsageCsv: () -> Unit = {},
) {
    var inlineView by remember { mutableStateOf(UsageInlineView.CURRENT_LOAD) }
    var showUsageDetails by remember { mutableStateOf(false) }
    val usage = state.usage ?: EngineUsageSnapshot()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Engine Control",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Box(Modifier.padding(top = 4.dp)) { engineControl() }
        }

        item {
            Text(
                "Resident Models",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (state.residentModels.isEmpty()) {
                Text(
                    when {
                        !state.isEngineRunning -> "Start the Engine to load and list models."
                        else -> "No model loaded. Models load automatically when requested, or you can load one from Cookbook."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                state.residentModels.forEach { model ->
                    ResidentModelCard(model = model)
                }
            }
        }

        item {
            Text(
                "Memory",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            MemoryBudgetIndicator(state.memory, modifier = Modifier.padding(vertical = 4.dp))
        }

        item {
            LoadAndUsageSection(
                usage = usage,
                inlineView = inlineView,
                onToggleView = { inlineView = inlineView.next() },
                onOpenDetails = { showUsageDetails = true },
            )
        }

        item {
            Column {
                Text(
                    "Inference Groups",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    when {
                        !state.isEngineRunning -> "Start the Engine to see linked devices"
                        state.linkedPeerCount == 0 -> "No peer devices linked (local inference only)"
                        state.linkedPeerCount == 1 -> "1 peer device linked for virtual model delegation"
                        else -> "${state.linkedPeerCount} peer devices linked for cluster inference"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            Column {
                Text(
                    "Storage",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    state.freeStorageGb?.let { "$it GB free" } ?: "Storage space unknown",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            Column {
                Text(
                    "Connected Apps",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                val appsSummary = when {
                    !state.isEngineRunning -> "Start the Engine to see connected apps"
                    state.approvedApps.isEmpty() && state.pendingAppCount == 0 -> "No apps connected yet"
                    state.approvedApps.isNotEmpty() && state.pendingAppCount > 0 ->
                        "${state.approvedApps.size} approved · ${state.pendingAppCount} pending approval"
                    state.approvedApps.isNotEmpty() -> "${state.approvedApps.size} approved"
                    else -> "${state.pendingAppCount} pending approval"
                }
                Text(
                    appsSummary,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (state.approvedApps.isNotEmpty()) {
                    Text(
                        state.approvedApps.joinToString(", "),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }

    if (showUsageDetails) {
        UsageDetailsDialog(
            usage = usage,
            onDismiss = { showUsageDetails = false },
            onExportJson = onExportUsageJson,
            onExportCsv = onExportUsageCsv,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoadAndUsageSection(
    usage: EngineUsageSnapshot,
    inlineView: UsageInlineView,
    onToggleView: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Load & Usage",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    inlineView.label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(
                    onClick = onOpenDetails,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Usage details",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                )
                .combinedClickable(
                    onClick = onToggleView,
                    onLongClick = onOpenDetails,
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (inlineView) {
                UsageInlineView.CURRENT_LOAD -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Active: ${usage.activeRequests} / ${usage.maxConcurrency}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "Queued: ${usage.queuedRequests}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { usage.loadFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                UsageInlineView.SESSION_TOTALS -> {
                    UsageTotalsInline(
                        emptyText = "No session usage yet",
                        totals = usage.session,
                    )
                }

                UsageInlineView.LIFETIME_TOTALS -> {
                    UsageTotalsInline(
                        emptyText = "No lifetime usage yet",
                        totals = usage.lifetime,
                    )
                }
            }

            Text(
                text = formatLastUsage(usage.lastUsage, Clock.System.now().toEpochMilliseconds()),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UsageTotalsInline(
    emptyText: String,
    totals: Map<UsageCategory, CategoryUsage>,
) {
    val nonEmpty = UsageCategory.entries.mapNotNull { cat ->
        val entry = totals[cat]
        if (entry != null && (entry.requests > 0 || entry.tokens > 0 || entry.audioSeconds > 0.0 || entry.images > 0)) {
            cat to entry
        } else {
            null
        }
    }
    if (nonEmpty.isEmpty()) {
        Text(
            emptyText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            nonEmpty.forEach { (cat, entry) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(cat.displayName(), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text(
                        formatCategoryUsage(entry),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageDetailsDialog(
    usage: EngineUsageSnapshot,
    onDismiss: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Usage Details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                UsageCategory.entries.forEach { category ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            category.displayName(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Session: ${formatCategoryUsage(usage.session[category])}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Lifetime: ${formatCategoryUsage(usage.lifetime[category])}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onExportJson) { Text("Export JSON") }
                TextButton(onClick = onExportCsv) { Text("Export CSV") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
