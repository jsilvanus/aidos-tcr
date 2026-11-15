package dev.aidos.tcr.engine

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.TcrColors

/** One background service as the user sees it. */
data class BackgroundServiceUi(val name: String, val registered: Boolean)

/**
 * What the "Run in background" pane shows (RFC-0115, D56). [supported] is false on a platform where the app cannot
 * register services; the pane then only explains. [busy] disables the switch while a change is being applied.
 */
data class BackgroundUiState(
    val supported: Boolean = true,
    val services: List<BackgroundServiceUi> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    /** On when every service is registered. */
    val enabled: Boolean get() = services.isNotEmpty() && services.all { it.registered }
}

/**
 * Organism TCR Component: BackgroundPane (RFC-0115).
 *
 * One switch: keep the Engine running when the window is closed, and show a notification-area icon for quick
 * approvals. Installs for the signed-in user only; machine-wide installs are a command-line choice because they
 * need an administrator.
 */
@Composable
fun BackgroundPane(
    state: BackgroundUiState,
    onSetEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(checked = state.enabled, onCheckedChange = onSetEnabled, enabled = state.supported && !state.busy)
            Column {
                Text("Keep Aidos running in the background", fontWeight = FontWeight.SemiBold)
                Text(
                    if (state.enabled) "The Engine starts when you sign in and keeps running with the window closed."
                    else "Off. The Engine runs only while this window is open.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!state.supported) Text("This system cannot register background services from the app.", fontSize = 12.sp)
        state.services.forEach {
            Text("${it.name}: ${if (it.registered) "registered" else "not registered"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.message?.let { Text(it, fontSize = 12.sp) }
        state.error?.let { Text(it, color = TcrColors.EmberRed, fontSize = 12.sp) }
        Text(
            "The notification icon shows when something waits for your answer: a new app, or a remote agent's tool call. " +
                "Notifications never show content. For a machine-wide service, run the install command from an administrator shell.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
