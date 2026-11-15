package dev.aidos.tcr.engine

/**
 * UI models for inference groups (RFC-0105): pairing, groups, linked devices and layer splitting.
 * Plain data, no AIDIN types, so any host can bind them. Ids are opaque strings the host maps back.
 */

/** What a pairing in progress is for, which decides the heading the user sees. */
enum class GroupPairingKind { PAIR, ENROL_OTHER, ENROLLED_BY_OTHER }

/** A pairing waiting for the user to compare the six-digit [code] with the other screen. */
data class GroupPairingUi(
    val id: String,
    val kind: GroupPairingKind,
    val code: String,
    /** The user pressed "Codes match" here. */
    val confirmedHere: Boolean = false,
    /** The other device confirmed too. */
    val confirmedThere: Boolean = false,
)

data class GroupUi(val id: String, val label: String, val isAdmin: Boolean)

/** A paired or group-linked device and what it last reported. */
data class GroupPeerUi(
    val id: String,
    /** The name the user gave it; null for a device that is only a group member. */
    val label: String?,
    /** Shortened node id shown when there is no [label]. */
    val shortId: String,
    val connected: Boolean,
    /** Connection lifecycle name, e.g. "READY". */
    val stateName: String,
    /** True when it is a paired device (offers Unpair). */
    val isPaired: Boolean = label != null,
    /** `paired` / `group` per trust domain it is reachable through. */
    val viaGroup: Boolean = false,
    val viaPaired: Boolean = false,
    val draining: Boolean = false,
    val batteryPercent: Int? = null,
    val charging: Boolean? = null,
    val engineVersion: String? = null,
    /** RAM the peer offers for split inference, in bytes; null or 0 when none. */
    val ramOfferedBytes: Long? = null,
    val modelIds: List<String> = emptyList(),
    /** Groups this device may be removed from by the user (groups where the user is admin). */
    val removableFromGroupIds: List<String> = emptyList(),
)

/** Layer split hosting on this device (RFC-0105 phase 3); null in [GroupsUiState] where the host offers none. */
data class GroupSplitUi(
    /** False until the pinned llama-server and ggml-rpc-server are installed. */
    val available: Boolean,
    /** The model currently served across a group, if any. */
    val activeModelId: String? = null,
    /** Latest status line from the split runtime. */
    val status: String? = null,
    /** Installed models this device could serve across a group. */
    val models: List<String> = emptyList(),
)

data class GroupsUiState(
    val sharingEnabled: Boolean = false,
    val isEngineRunning: Boolean = false,
    /** False while the node is still coming up. */
    val nodeStarted: Boolean = false,
    /** Why sharing could not start. */
    val error: String? = null,
    /** Failure of the last user action. */
    val actionError: String? = null,
    /** This device's node id and port, e.g. "ab12… · port 18080". */
    val thisDevice: String = "",
    val pairings: List<GroupPairingUi> = emptyList(),
    /** Message of the most recent failed pairing. */
    val lastPairingFailure: String? = null,
    val groups: List<GroupUi> = emptyList(),
    val peers: List<GroupPeerUi> = emptyList(),
    /** Invite text currently shown for another device to scan or paste. */
    val shownInvite: String? = null,
    val split: GroupSplitUi? = null,
)

class GroupsActions(
    val onSharingChange: (Boolean) -> Unit = {},
    val onConfirmPairing: (id: String, label: String) -> Unit = { _, _ -> },
    val onRejectPairing: (id: String) -> Unit = {},
    val onShowInvite: () -> Unit = {},
    /** True when the pasted text is an invite this version understands. */
    val isValidInvite: (String) -> Boolean = { it.isNotBlank() },
    val onPair: (invite: String) -> Unit = {},
    val onEnrol: (invite: String, groupId: String) -> Unit = { _, _ -> },
    val onLeaveGroup: (groupId: String) -> Unit = {},
    val onCreateGroup: (name: String) -> Unit = {},
    val onUnpair: (peerId: String) -> Unit = {},
    val onRemoveFromGroup: (groupId: String, peerId: String) -> Unit = { _, _ -> },
    /** Serve [model] across [groupId]; returns the outcome in words. Null hides the Run button. */
    val onStartSplit: (suspend (groupId: String, model: String) -> String)? = null,
    val onStopSplit: suspend () -> Unit = {},
)

/** One-line peer status and capacity summaries, shared so every host words them the same. */
fun GroupPeerUi.statusLine(): String = listOfNotNull(
    stateName.lowercase(),
    if (!connected) "not linked" else null,
    listOfNotNull(if (viaGroup) "group" else null, if (viaPaired) "paired" else null).joinToString().ifBlank { null },
    if (draining) "draining" else null,
    batteryPercent?.let { "$it%" + if (charging == true) " ⚡" else "" },
    engineVersion?.let { "v$it" },
).joinToString(" · ")

fun GroupPeerUi.capacityLine(): String? {
    val ram = ramOfferedBytes?.takeIf { it > 0 }?.let { "${it / (1024 * 1024)} MB RAM offered" }
    val models = modelIds.takeIf { it.isNotEmpty() }?.let { ids ->
        "${ids.size} model${if (ids.size == 1) "" else "s"}: " + ids.joinToString(", ") { it.substringAfterLast('/') }
    }
    return listOfNotNull(ram, models).joinToString(" · ").ifBlank { null }
}
