package dev.aidos.tcr.engine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.TcrColors

/** What the user can do to a peer on this pane (RFC-0107, RFC-0108). Every callback names the peer by id. */
data class RemotePeersActions(
    val onSetHostsAgentLoops: (peerId: String, enabled: Boolean) -> Unit,
    val onSetPreferred: (peerId: String, preferred: Boolean) -> Unit,
    val onSetRemoteAccess: (peerId: String, allowed: Boolean) -> Unit,
    val onAddHint: (peerId: String, host: String, port: Int) -> Unit,
    val onRemoveHint: (peerId: String, hint: String) -> Unit,
    val onSetToolAllowlist: (peerId: String, app: String, tools: List<String>) -> Unit,
    val onDecideToolApproval: (approvalId: String, allow: Boolean) -> Unit,
)

/**
 * Organism TCR Component: RemotePeersPane (RFC-0107 remote agent hosting, RFC-0108 remote routes).
 *
 * One card per paired peer with the switches that are off until the user turns them on:
 * - **Run agent loops for this peer** (the `agent` scope).
 * - **Prefer this peer for my agent turns** (ordered; the local loop is always the last resort).
 * - **Allow remote access** (reach this Engine over a tunnel or overlay address) and the `host:port`
 *   hints used to dial the peer when it is not on the LAN.
 * - **Tools this peer's agent may use**, per app (none by default).
 *
 * Effectful tool calls on a tainted remote run appear at the top as approval prompts; an unanswered
 * prompt is a refusal.
 */
@Composable
fun RemotePeersPane(
    state: RemoteSharingUiState,
    actions: RemotePeersActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.error?.let { item { Text(it, color = TcrColors.EmberRed, fontSize = 12.sp) } }

        if (state.pendingApprovals.isNotEmpty()) {
            item { Text("Waiting for your approval", fontWeight = FontWeight.SemiBold, color = TcrColors.AmberWarning) }
            items(state.pendingApprovals, key = { it.id }) { a ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${a.peerLabel} wants ${a.app} to run ${a.tool}", fontWeight = FontWeight.Medium)
                            Text(
                                "This run has already received tool results, so a tool that changes things needs your approval. No answer counts as no.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(onClick = { actions.onDecideToolApproval(a.id, true) }, colors = ButtonDefaults.buttonColors(containerColor = TcrColors.EmeraldGreen)) { Text("Approve") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { actions.onDecideToolApproval(a.id, false) }) { Text("Deny", color = TcrColors.EmberRed) }
                    }
                }
            }
        }

        if (!state.aidinRunning) {
            item {
                Text(
                    "Group sharing is off, so there are no peers. Turn it on in Groups to pair another device.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (state.peers.isEmpty()) {
            item { Text("No device is paired yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        items(state.peers, key = { it.id }) { peer -> PeerCard(peer, state.knownApps, actions) }

        if (state.auditLines.isNotEmpty()) {
            item { Text("Recent activity", fontWeight = FontWeight.SemiBold) }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        state.auditLines.takeLast(12).forEach { Text(it, fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeerCard(peer: RemotePeerUi, knownApps: List<String>, actions: RemotePeersActions) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(peer.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                RouteChip(peer)
            }
            Text(peer.id, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            SwitchRow("Run agent loops for this device", "It can start an agent here. Its tools still run on its side.", peer.hostsAgentLoops) {
                actions.onSetHostsAgentLoops(peer.id, it)
            }
            SwitchRow(
                "Use this device for my agent turns",
                if (peer.preferredRank != null) "Preferred host #${peer.preferredRank}. Only turns that allow the cluster leave this device; this Engine is the fallback." else "Off. Agent turns run here.",
                peer.preferredRank != null,
            ) { actions.onSetPreferred(peer.id, it) }
            SwitchRow(
                "Allow remote access",
                "Lets this device reach this Engine over a tunnel or overlay address. It never carries split inference.",
                peer.remoteAccessAllowed,
            ) { actions.onSetRemoteAccess(peer.id, it) }

            HintsEditor(peer, actions)
            if (peer.hostsAgentLoops || peer.preferredRank != null || peer.toolAllowlists.isNotEmpty()) {
                ToolAllowlists(peer, knownApps, actions)
            }
        }
    }
}

@Composable
private fun RouteChip(peer: RemotePeerUi) {
    val (text, color) = when {
        !peer.connected -> "Offline" to MaterialTheme.colorScheme.onSurfaceVariant
        peer.route == PeerRouteUi.LAN || peer.route == null -> "LAN" to TcrColors.EmeraldGreen
        peer.route == PeerRouteUi.DIRECT -> "Direct" to TcrColors.ElectricBlue
        else -> "SSH tunnel" to TcrColors.VioletProtocol
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
    }
}

@Composable
private fun SwitchRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun HintsEditor(peer: RemotePeerUi, actions: RemotePeersActions) {
    var text by remember(peer.id) { mutableStateOf("") }
    val parsed = parseHostPort(text)
    Text("Addresses to dial when it is not on the LAN", fontSize = 12.sp, fontWeight = FontWeight.Medium)
    peer.hints.forEach { hint ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(hint, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { actions.onRemoveHint(peer.id, hint) }) { Text("Remove", color = TcrColors.EmberRed, fontSize = 12.sp) }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            text, { text = it }, label = { Text("host:port (Tailscale, forwarded port, tunnel end)") },
            singleLine = true, isError = text.isNotBlank() && parsed == null, modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = { parsed?.let { (h, p) -> actions.onAddHint(peer.id, h, p); text = "" } }, enabled = parsed != null) { Text("Add") }
    }
    if (peer.hints.isNotEmpty() && !peer.remoteAccessAllowed) {
        Text("Remote access is off, so these addresses are not used.", fontSize = 11.sp, color = TcrColors.AmberWarning)
    }
}

@Composable
private fun ToolAllowlists(peer: RemotePeerUi, knownApps: List<String>, actions: RemotePeersActions) {
    Text("Tools this device's agent may use", fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Text("Per app, none by default. A call to anything else is refused here, before the app sees it.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val apps = (peer.toolAllowlists.map { it.app } + knownApps).distinct()
    if (apps.isEmpty()) {
        Text("No app has connected yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    apps.forEach { app ->
        val current = peer.toolAllowlists.firstOrNull { it.app == app }?.tools.orEmpty()
        var text by remember(peer.id, app, current) { mutableStateOf(current.joinToString(", ")) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                text, { text = it }, label = { Text(app) }, placeholder = { Text("search, read_note") },
                singleLine = true, modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { actions.onSetToolAllowlist(peer.id, app, parseToolNames(text)) }, enabled = parseToolNames(text) != current) { Text("Save") }
        }
    }
}
