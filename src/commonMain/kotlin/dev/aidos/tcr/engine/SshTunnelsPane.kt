package dev.aidos.tcr.engine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.TcrColors

/** Where an SSH reverse tunnel is in its life (RFC-0108). */
enum class TunnelStatusUi(val label: String) {
    /** Created, but the server's host key has not been confirmed, so it cannot start. */
    NEEDS_HOST_KEY("Confirm host key"),
    STOPPED("Stopped"),
    CONNECTING("Connecting"),
    UP("Up"),
    RECONNECTING("Reconnecting"),
    HOST_KEY_CHANGED("Host key changed"),
    AUTH_REFUSED("Server refused the key"),
    FORWARD_REFUSED("Server refused the forward"),
}

data class SshTunnelUi(
    val id: String,
    val name: String,
    val server: String,
    val user: String,
    /** The port on the server that this Engine is published on. */
    val publishedPort: Int,
    val status: TunnelStatusUi,
    val detail: String?,
    /** The restricted line to paste into the server's `authorized_keys`. Never a private key. */
    val authorizedKeysLine: String?,
    val pinnedHostKey: String?,
    /** A fingerprint just read from the server, waiting for the user to compare and confirm. */
    val pendingHostKey: String?,
)

data class SshTunnelsUiState(
    val tunnels: List<SshTunnelUi> = emptyList(),
    /** False while group sharing is off: there is no peer listener to publish. */
    val canPublish: Boolean = false,
    val error: String? = null,
)

data class SshTunnelsActions(
    val onCreate: (name: String, host: String, port: Int, user: String, publishedPort: Int) -> Unit,
    val onProbeHostKey: (id: String) -> Unit,
    val onConfirmHostKey: (id: String) -> Unit,
    val onStart: (id: String) -> Unit,
    val onStop: (id: String) -> Unit,
    val onRotateKey: (id: String) -> Unit,
    val onDelete: (id: String) -> Unit,
)

/**
 * Organism TCR Component: SshTunnelsPane (RFC-0108 phase 3).
 *
 * An SSH reverse tunnel publishes this Engine's peer listener on a server the user owns, so a paired
 * device away from home can reach it. The pane is the whole setup, in the order it has to happen:
 * 1. Create the tunnel (server, user, the port to publish on). A key is generated and kept in the vault; only
 *    its restricted `authorized_keys` line is shown, and that is what you paste on the server.
 * 2. Check the server: its host key fingerprint is read and shown. Compare it with the one you know and confirm.
 *    There is no trust on first use, and a later change stops the tunnel.
 * 3. Start it. A peer arriving through the tunnel still needs "Allow remote access" on the Peers tab.
 */
@Composable
fun SshTunnelsPane(
    state: SshTunnelsUiState,
    actions: SshTunnelsActions,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var user by remember { mutableStateOf("") }
    var published by remember { mutableStateOf("47100") }
    val portN = port.toIntOrNull()?.takeIf { it in 1..65535 }
    val publishedN = published.toIntOrNull()?.takeIf { it in 1..65535 }

    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.error?.let { item { Text(it, color = TcrColors.EmberRed, fontSize = 12.sp) } }
        if (!state.canPublish) {
            item {
                Text(
                    "Group sharing is off, so there is no peer listener to publish. Turn it on in Groups first.",
                    fontSize = 12.sp, color = TcrColors.AmberWarning,
                )
            }
        }

        items(state.tunnels, key = { it.id }) { t -> TunnelCard(t, actions) }
        if (state.tunnels.isEmpty()) {
            item { Text("No tunnels. Add one below if you want a paired device to reach this Engine from outside your network.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add a tunnel", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(host, { host = it }, label = { Text("Server") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(port, { port = it.filter(Char::isDigit) }, label = { Text("SSH port") }, singleLine = true, isError = portN == null, modifier = Modifier.width(110.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(user, { user = it }, label = { Text("User on the server") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(published, { published = it.filter(Char::isDigit) }, label = { Text("Publish on port") }, singleLine = true, isError = publishedN == null, modifier = Modifier.width(140.dp))
                    }
                    Button(
                        onClick = { actions.onCreate(name, host, portN!!, user, publishedN!!); name = ""; host = ""; user = "" },
                        enabled = state.canPublish && name.isNotBlank() && host.isNotBlank() && user.isNotBlank() && portN != null && publishedN != null,
                    ) { Text("Create tunnel and key") }
                }
            }
        }
    }
}

@Composable
private fun TunnelCard(t: SshTunnelUi, actions: SshTunnelsActions) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(t.name, fontWeight = FontWeight.SemiBold)
                    Text("${t.user}@${t.server} publishes this Engine on port ${t.publishedPort}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusChip(t.status)
            }
            t.detail?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            // Step 1: the server line.
            t.authorizedKeysLine?.let { line ->
                Text("Add this line to ~/.ssh/authorized_keys on the server", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(line, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                Text(
                    "It only lets this key publish port ${t.publishedPort}: no shell, no other forwarding. The private key stays in this computer's vault.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Step 2: the host key.
            when {
                t.pinnedHostKey != null -> Text("Server host key pinned  ${t.pinnedHostKey}", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                t.pendingHostKey != null -> {
                    Text("The server presented this host key. Confirm only if it matches the one you know.", fontSize = 12.sp, color = TcrColors.AmberWarning)
                    Text(t.pendingHostKey, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Button(onClick = { actions.onConfirmHostKey(t.id) }) { Text("It matches: pin it") }
                }
                else -> Text("The server's host key has not been checked yet.", fontSize = 12.sp, color = TcrColors.AmberWarning)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (t.pinnedHostKey == null) OutlinedButton(onClick = { actions.onProbeHostKey(t.id) }) { Text("Check server") }
                val running = t.status == TunnelStatusUi.UP || t.status == TunnelStatusUi.CONNECTING || t.status == TunnelStatusUi.RECONNECTING
                if (running) OutlinedButton(onClick = { actions.onStop(t.id) }) { Text("Stop") }
                else Button(onClick = { actions.onStart(t.id) }, enabled = t.pinnedHostKey != null) { Text("Start") }
                OutlinedButton(onClick = { actions.onRotateKey(t.id) }) { Text("New key") }
                TextButton(onClick = { actions.onDelete(t.id) }) { Text("Delete", color = TcrColors.EmberRed) }
            }
        }
    }
}

@Composable
private fun StatusChip(status: TunnelStatusUi) {
    val color: Color = when (status) {
        TunnelStatusUi.UP -> TcrColors.EmeraldGreen
        TunnelStatusUi.CONNECTING, TunnelStatusUi.RECONNECTING, TunnelStatusUi.NEEDS_HOST_KEY -> TcrColors.AmberWarning
        TunnelStatusUi.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> TcrColors.EmberRed
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text(status.label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
    }
}
