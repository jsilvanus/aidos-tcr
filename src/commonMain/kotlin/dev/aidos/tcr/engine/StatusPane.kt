package dev.aidos.tcr.engine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Organism TCR Component: StatusPane
 *
 * Engine status, resident models, and memory budget dashboard:
 * - Engine running/stopped state banner with host-specific control toggle
 * - Resident memory models list
 * - Device RAM budget gauge
 * - Free storage volume telemetry
 * - Inference Groups peer link counts (RFC-0105)
 * - Authorized connected caller apps tally
 */
@Composable
fun StatusPane(
    state: StatusUiState,
    engineControl: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
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
}
