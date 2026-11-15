package dev.aidos.tcr.engine

/**
 * UI models for the Engine's sharing surfaces: LAN sharing (RFC-0051), remote agent hosting
 * (RFC-0107) and remote routes (RFC-0108). Plain data, no Engine types, so any app can bind them.
 */

/** A device that may use an Engine over the LAN. */
data class LanDeviceUi(
    val id: String,
    val label: String,
    val expiresAt: String,
    val revoked: Boolean,
    val expired: Boolean,
)

/** What a freshly started pairing shows: a QR/URI for SDK clients, or connection details for a generic client. */
sealed interface LanPairingUi {
    /** Mode A: [uri] is what the QR code holds; the single-use [code] is also shown for typing. */
    data class Code(val label: String, val uri: String, val code: String) : LanPairingUi

    /** Mode B: manual connection details. The credential is shown once and not kept. */
    data class Manual(
        val label: String,
        val url: String,
        val fingerprintSha256: String?,
        val credential: String,
        val expiresAt: String,
    ) : LanPairingUi
}

data class LanSharingUiState(
    val enabled: Boolean = false,
    val port: Int = 18443,
    val fingerprintSha256: String? = null,
    val devices: List<LanDeviceUi> = emptyList(),
    val pairing: LanPairingUi? = null,
    val error: String? = null,
)

/** How a peer link arrived (RFC-0108). */
enum class PeerRouteUi(val label: String) {
    LAN("LAN"),
    DIRECT("Direct"),
    SSH_TUNNEL("SSH tunnel"),
}

/** The tools one app on this Engine lets one peer's agent loop use. Empty means none: the default. */
data class ToolAllowlistUi(val app: String, val tools: List<String>)

/** A paired AIDIN peer, with everything the user can switch about it (RFC-0107, RFC-0108). */
data class RemotePeerUi(
    val id: String,
    val label: String,
    val connected: Boolean,
    /** Null while there is no link. */
    val route: PeerRouteUi?,
    /** This Engine runs agent loops for the peer (the `agent` scope). Off until the user switches it on. */
    val hostsAgentLoops: Boolean,
    /** 1-based position in this Engine's preferred hosts for its own agent turns; null when not preferred. */
    val preferredRank: Int?,
    /** The peer may reach this Engine over a tunnel or overlay address. Off by default. */
    val remoteAccessAllowed: Boolean,
    /** `host:port` addresses to dial the peer at when it is not on the LAN. */
    val hints: List<String>,
    val toolAllowlists: List<ToolAllowlistUi>,
)

/** An effectful tool call on a tainted remote run, waiting for the user. Carries no arguments or content. */
data class ToolApprovalUi(
    val id: String,
    val peerLabel: String,
    val app: String,
    val tool: String,
)

data class RemoteSharingUiState(
    /** False while group sharing (AIDIN) is off: there are no peers to show. */
    val aidinRunning: Boolean = false,
    val peers: List<RemotePeerUi> = emptyList(),
    val pendingApprovals: List<ToolApprovalUi> = emptyList(),
    /** Newest last: the remote-access switch changes, remote-link decisions and redials. */
    val auditLines: List<String> = emptyList(),
    /** Apps on this Engine that can have a tool allowlist, offered when adding one. */
    val knownApps: List<String> = emptyList(),
    val error: String? = null,
)

/** `host:port` as typed, or null when it is not a valid address. Shared so every client validates the same way. */
fun parseHostPort(text: String): Pair<String, Int>? {
    val t = text.trim()
    val i = t.lastIndexOf(':')
    if (i <= 0 || i == t.lastIndex) return null
    val host = t.substring(0, i)
    val port = t.substring(i + 1).toIntOrNull() ?: return null
    if (host.any { it.isWhitespace() } || port !in 1..65535) return null
    return host to port
}

/** Tool names from comma, space or newline separated text; blanks dropped, order kept, duplicates removed. */
fun parseToolNames(text: String): List<String> =
    text.split(',', ' ', '\n', '\t').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
