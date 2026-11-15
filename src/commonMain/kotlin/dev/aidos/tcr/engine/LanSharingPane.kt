package dev.aidos.tcr.engine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.TcrColors

/**
 * Organism TCR Component: LanSharingPane (RFC-0051).
 *
 * Switch LAN sharing on, add a device by code/QR or by manual connection details, and revoke it.
 * A device added here also appears under Connected Apps as `LAN: <label>`, where its agent access and
 * locality limit are set: adding it approves it for model use only.
 *
 * [qrCode] draws the pairing link as a QR code. It is a slot because the generator is a platform
 * dependency of the consumer app, not of this registry; with null only the link and code are shown.
 */
@Composable
fun LanSharingPane(
    state: LanSharingUiState,
    onSetEnabled: (Boolean) -> Unit,
    onAddByCode: (label: String, lifetimeHours: Long) -> Unit,
    onAddManually: (label: String, lifetimeHours: Long) -> Unit,
    onRevoke: (String) -> Unit,
    onDismissPairing: () -> Unit,
    modifier: Modifier = Modifier,
    qrCode: (@Composable (String) -> Unit)? = null,
) {
    var label by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("24") }
    val lifetime = hours.toLongOrNull()?.coerceIn(1, 24 * 365)

    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(checked = state.enabled, onCheckedChange = onSetEnabled)
                Column {
                    Text("Share this Engine on the local network", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (state.enabled) "HTTPS on port ${state.port}. Only devices you add below can use it."
                        else "Off. Nothing is listening beyond this computer.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        state.fingerprintSha256?.let {
            item {
                Text("Certificate fingerprint (SHA-256)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(it, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
        state.error?.let { item { Text(it, color = TcrColors.EmberRed, fontSize = 12.sp) } }

        if (state.enabled) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Add a device", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(label, { label = it }, label = { Text("Name (e.g. Kitchen tablet)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(hours, { hours = it.filter(Char::isDigit) }, label = { Text("Valid for (hours)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onAddByCode(label, lifetime!!) }, enabled = label.isNotBlank() && lifetime != null) { Text("Pair with a code / QR") }
                            OutlinedButton(onClick = { onAddManually(label, lifetime!!) }, enabled = label.isNotBlank() && lifetime != null) { Text("Show connection details") }
                        }
                    }
                }
            }
            state.pairing?.let { pairing -> item { PairingCard(pairing, qrCode, onDismissPairing) } }
        }

        item { Text("Devices", fontWeight = FontWeight.SemiBold) }
        if (state.devices.isEmpty()) {
            item { Text("No device has been added.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.devices, key = { it.id }) { d ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.label, fontWeight = FontWeight.Medium)
                            Text(
                                when {
                                    d.revoked -> "Revoked"
                                    d.expired -> "Expired ${d.expiresAt}"
                                    else -> "Expires ${d.expiresAt}"
                                },
                                fontSize = 12.sp,
                                color = if (d.revoked) TcrColors.EmberRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!d.revoked) OutlinedButton(onClick = { onRevoke(d.id) }) { Text("Revoke", color = TcrColors.EmberRed) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingCard(pairing: LanPairingUi, qrCode: (@Composable (String) -> Unit)?, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (pairing) {
                is LanPairingUi.Code -> {
                    Text("Pair ${pairing.label}", fontWeight = FontWeight.SemiBold)
                    Text("Scan this on the device, or open the link. The code works once and expires in five minutes.", fontSize = 12.sp)
                    qrCode?.invoke(pairing.uri)
                    Text("Code ${pairing.code}", fontFamily = FontFamily.Monospace, fontSize = 18.sp)
                    Text(pairing.uri, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                is LanPairingUi.Manual -> {
                    Text("Connection details for ${pairing.label}", fontWeight = FontWeight.SemiBold)
                    Text("Shown once. Enter them in the other client; it must trust this certificate fingerprint.", fontSize = 12.sp)
                    Text("URL  ${pairing.url}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text("Fingerprint  ${pairing.fingerprintSha256 ?: "-"}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text("Bearer credential  ${pairing.credential}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text("Valid until ${pairing.expiresAt}", fontSize = 12.sp)
                }
            }
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    }
}
