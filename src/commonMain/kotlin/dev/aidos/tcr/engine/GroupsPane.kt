package dev.aidos.tcr.engine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Organism TCR Component: GroupsPane
 *
 * Inference groups (RFC-0105): turn sharing on, pair with or enrol another device by exchanging an
 * invite, compare the six-digit code, inspect linked devices and serve a model across a group.
 * Plain state in, callbacks out: the host owns the AIDIN node and does the network work.
 *
 * @param initialInvite an invite to prefill, e.g. from an `aidin:` link the user opened.
 * @param qrCode renders the invite as a QR code; the library has no platform QR encoder, so the host supplies it.
 */
@Composable
fun GroupsPane(
    state: GroupsUiState,
    actions: GroupsActions,
    modifier: Modifier = Modifier,
    initialInvite: String = "",
    qrCode: @Composable (String) -> Unit = {},
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Share with devices on this network", fontWeight = FontWeight.SemiBold)
                Text(
                    "Lets paired devices and group members find and link to this Engine for virtual model resolution (aidos:chat) and cooperative split inference. Off by default.",
                    fontSize = 12.sp,
                )
            }
            Switch(checked = state.sharingEnabled, onCheckedChange = actions.onSharingChange)
        }
        state.error?.let { Text("Could not start sharing: $it", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        when {
            !state.sharingEnabled -> Unit
            !state.isEngineRunning -> Text("Start the Engine to use device groups.")
            !state.nodeStarted -> Text("Starting…")
            else -> GroupsContent(state, actions, initialInvite, qrCode)
        }
    }
}

@Composable
private fun GroupsContent(state: GroupsUiState, actions: GroupsActions, initialInvite: String, qrCode: @Composable (String) -> Unit) {
    Text("This device", fontWeight = FontWeight.SemiBold)
    SelectionContainer { Text(state.thisDevice, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }

    state.pairings.forEach { PairingCard(it, actions) }
    state.lastPairingFailure?.let {
        Text("Pairing failed: $it", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    }
    state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

    HorizontalDivider()
    InviteSection(state, actions, initialInvite, qrCode)

    HorizontalDivider()
    Text("Groups", fontWeight = FontWeight.SemiBold)
    state.groups.forEach { group ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(group.label.ifBlank { group.id } + if (group.isAdmin) " (admin)" else "", modifier = Modifier.weight(1f))
            TextButton(onClick = { actions.onLeaveGroup(group.id) }) { Text("Leave") }
        }
    }
    var newGroup by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(newGroup, { newGroup = it }, label = { Text("New group name") }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = { val name = newGroup.trim(); newGroup = ""; actions.onCreateGroup(name) },
            enabled = newGroup.isNotBlank(),
        ) { Text("Create group") }
    }

    HorizontalDivider()
    Text("Devices", fontWeight = FontWeight.SemiBold)
    if (state.peers.isEmpty()) Text("No paired or linked devices yet.", fontSize = 12.sp)
    state.peers.forEach { PeerRow(it, actions) }

    state.split?.let {
        HorizontalDivider()
        SplitSection(it, state.groups, actions)
    }
}

/** Serve one model across a group: this device keeps some layers, members that offer RAM take the rest. */
@Composable
private fun SplitSection(split: GroupSplitUi, groups: List<GroupUi>, actions: GroupsActions) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var group by remember(groups) { mutableStateOf(groups.firstOrNull()?.id) }
    var model by remember(split.models) { mutableStateOf(split.models.firstOrNull()) }

    Text("Run a model across a group", fontWeight = FontWeight.SemiBold)
    if (!split.available) {
        Text(
            "This device needs the pinned llama-server and ggml-rpc-server to split a model. " +
                "Until they are installed it neither serves nor helps with split models.",
            fontSize = 12.sp,
        )
        return
    }
    val active = split.activeModelId
    if (active != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Serving $active across the group", modifier = Modifier.weight(1f))
            TextButton(onClick = { scope.launch { actions.onStopSplit() } }) { Text("Stop") }
        }
    }
    val onStart = actions.onStartSplit ?: return
    if (groups.isEmpty() || split.models.isEmpty()) {
        Text("Join a group and install a GGUF model to run it across the group.", fontSize = 12.sp)
        return
    }
    ChoiceRow("Group", groups.map { it.id to it.label.ifBlank { it.id } }, group) { group = it }
    ChoiceRow("Model", split.models.map { it to it }, model) { model = it }
    Button(
        onClick = {
            val g = group ?: return@Button
            val m = model ?: return@Button
            busy = true
            scope.launch {
                result = try {
                    onStart(g, m)
                } catch (e: Exception) {
                    e.message ?: e::class.simpleName
                }
                busy = false
            }
        },
        enabled = !busy && active == null && group != null && model != null,
    ) { Text(if (busy) "Loading…" else "Run across group") }
    (result ?: split.status)?.let { Text(it, fontSize = 12.sp) }
}

@Composable
private fun <T> ChoiceRow(label: String, options: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.width(56.dp), fontSize = 12.sp)
        Box {
            OutlinedButton(onClick = { open = true }) { Text(options.firstOrNull { it.first == selected }?.second ?: "Choose") }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(value); open = false })
                }
            }
        }
    }
}

@Composable
private fun PairingCard(pairing: GroupPairingUi, actions: GroupsActions) {
    var label by remember(pairing.id) { mutableStateOf("") }
    val what = when (pairing.kind) {
        GroupPairingKind.PAIR -> "Pair with another device"
        GroupPairingKind.ENROL_OTHER -> "Add a device to your group"
        GroupPairingKind.ENROLLED_BY_OTHER -> "Join a group run by another device"
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(what, fontWeight = FontWeight.SemiBold)
            Text("Check that the other screen shows the same code:", fontSize = 12.sp)
            Text(pairing.code.chunked(3).joinToString(" "), fontSize = 28.sp, fontFamily = FontFamily.Monospace)
            if (pairing.confirmedHere) {
                Text(if (pairing.confirmedThere) "Finishing…" else "Waiting for the other device to confirm.", fontSize = 12.sp)
            } else {
                OutlinedTextField(label, { label = it }, label = { Text("Name for this device or group") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { actions.onConfirmPairing(pairing.id, label.trim()) }) { Text("Codes match") }
                    OutlinedButton(onClick = { actions.onRejectPairing(pairing.id) }) { Text("They differ") }
                }
            }
        }
    }
}

@Composable
private fun InviteSection(state: GroupsUiState, actions: GroupsActions, initialInvite: String, qrCode: @Composable (String) -> Unit) {
    var pasted by remember(initialInvite) { mutableStateOf(initialInvite) }
    val adminGroups = state.groups.filter { it.isAdmin }

    Text("Connect a device", fontWeight = FontWeight.SemiBold)
    Text(
        "To pair or to be added to a group, show an invite here and scan or paste it on the other device. " +
            "It works once and expires in five minutes.",
        fontSize = 12.sp,
    )
    OutlinedButton(onClick = actions.onShowInvite) { Text("Show invite") }
    state.shownInvite?.let {
        qrCode(it)
        SelectionContainer { Text(it, fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
    }

    OutlinedTextField(pasted, { pasted = it }, label = { Text("Paste another device's invite") }, modifier = Modifier.fillMaxWidth())
    val valid = remember(pasted) { pasted.isNotBlank() && actions.isValidInvite(pasted.trim()) }
    if (pasted.isNotBlank() && !valid) Text("That is not an invite this version understands.", fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { val i = pasted.trim(); pasted = ""; actions.onPair(i) }, enabled = valid) { Text("Pair") }
        adminGroups.forEach { group ->
            OutlinedButton(onClick = { val i = pasted.trim(); pasted = ""; actions.onEnrol(i, group.id) }, enabled = valid) {
                Text("Add to ${group.label.ifBlank { "group" }}")
            }
        }
    }
}

@Composable
private fun PeerRow(peer: GroupPeerUi, actions: GroupsActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(peer.label?.ifBlank { null } ?: peer.shortId, fontWeight = FontWeight.SemiBold)
                if (peer.connected) {
                    Surface(color = Color(0xFF22C55E).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "LINKED",
                            color = Color(0xFF16A34A),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(peer.statusLine(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            peer.capacityLine()?.let {
                Text(it, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (peer.isPaired) TextButton(onClick = { actions.onUnpair(peer.id) }) { Text("Unpair") }
        peer.removableFromGroupIds.forEach { g ->
            TextButton(onClick = { actions.onRemoveFromGroup(g, peer.id) }) { Text("Remove from group") }
        }
    }
}
