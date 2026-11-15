package dev.aidos.tcr.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import dev.aidos.tcr.engine.GroupPairingKind
import dev.aidos.tcr.engine.GroupPairingUi
import dev.aidos.tcr.engine.GroupPeerUi
import dev.aidos.tcr.engine.GroupSplitUi
import dev.aidos.tcr.engine.GroupUi
import dev.aidos.tcr.engine.GroupsActions
import dev.aidos.tcr.engine.GroupsPane
import dev.aidos.tcr.engine.GroupsUiState
import dev.aidos.tcr.engine.capacityLine
import dev.aidos.tcr.engine.statusLine
import dev.aidos.tcr.theme.TcrTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class GroupsUiTest {

    private val running = GroupsUiState(
        sharingEnabled = true, isEngineRunning = true, nodeStarted = true, thisDevice = "node-1 · port 18080",
    )

    @Test
    fun sharingOffShowsOnlyTheSwitch() = runComposeUiTest {
        var enabled: Boolean? = null
        setContent {
            TcrTheme { GroupsPane(GroupsUiState(sharingEnabled = false), GroupsActions(onSharingChange = { enabled = it })) }
        }
        onNodeWithText("Share with devices on this network").assertIsDisplayed()
        onAllNodesWithText("This device").assertCountEquals(0)
        assertNull(enabled)
    }

    @Test
    fun explainsWhyNothingShowsWhenTheEngineIsStoppedOrStarting() = runComposeUiTest {
        setContent {
            TcrTheme {
                GroupsPane(GroupsUiState(sharingEnabled = true, isEngineRunning = false), GroupsActions())
            }
        }
        onNodeWithText("Start the Engine to use device groups.").assertIsDisplayed()
    }

    @Test
    fun showsStartingWhileTheNodeComesUp() = runComposeUiTest {
        setContent {
            TcrTheme { GroupsPane(GroupsUiState(sharingEnabled = true, isEngineRunning = true), GroupsActions()) }
        }
        onNodeWithText("Starting…").assertIsDisplayed()
    }

    @Test
    fun showsStartupError() = runComposeUiTest {
        setContent {
            TcrTheme { GroupsPane(GroupsUiState(error = "port in use"), GroupsActions()) }
        }
        onNodeWithText("Could not start sharing: port in use").assertIsDisplayed()
    }

    @Test
    fun pairingCardShowsGroupedCodeAndConfirmsWithTheTypedName() = runComposeUiTest {
        var confirmed: Pair<String, String>? = null
        var rejected: String? = null
        setContent {
            TcrTheme {
                GroupsPane(
                    running.copy(pairings = listOf(GroupPairingUi("p1", GroupPairingKind.PAIR, "123456"))),
                    GroupsActions(onConfirmPairing = { id, l -> confirmed = id to l }, onRejectPairing = { rejected = it }),
                )
            }
        }
        onNodeWithText("Pair with another device").assertIsDisplayed()
        onNodeWithText("123 456").assertIsDisplayed()
        onNodeWithText("Name for this device or group").performTextInput(" Phone ")
        onNodeWithText("Codes match").performClick()
        assertEquals("p1" to "Phone", confirmed)
        onNodeWithText("They differ").performClick()
        assertEquals("p1", rejected)
    }

    @Test
    fun pairingCardWaitsOnceConfirmedHere() = runComposeUiTest {
        setContent {
            TcrTheme {
                GroupsPane(
                    running.copy(pairings = listOf(GroupPairingUi("p1", GroupPairingKind.ENROLLED_BY_OTHER, "654321", confirmedHere = true))),
                    GroupsActions(),
                )
            }
        }
        onNodeWithText("Join a group run by another device").assertIsDisplayed()
        onNodeWithText("Waiting for the other device to confirm.").assertIsDisplayed()
        onAllNodesWithText("Codes match").assertCountEquals(0)
    }

    @Test
    fun garbageInviteIsRejectedAndPairStaysDisabled() = runComposeUiTest {
        setContent {
            TcrTheme {
                GroupsPane(
                    running.copy(groups = listOf(GroupUi("g1", "Family", true))),
                    GroupsActions(isValidInvite = { it.startsWith("aidin:") }),
                )
            }
        }
        onNodeWithText("Pair").assertIsNotEnabled()
        onNodeWithText("Paste another device's invite").performTextInput("garbage")
        onNodeWithText("That is not an invite this version understands.").assertIsDisplayed()
        onNodeWithText("Pair").assertIsNotEnabled()
        onNodeWithText("Add to Family").assertIsNotEnabled()
    }

    @Test
    fun validInviteCanPairOrEnrolIntoAnAdminGroup() = runComposeUiTest {
        var paired: String? = null
        var enrolled: Pair<String, String>? = null
        setContent {
            TcrTheme {
                GroupsPane(
                    running.copy(groups = listOf(GroupUi("g1", "Family", true), GroupUi("g2", "Work", false))),
                    GroupsActions(
                        isValidInvite = { it.startsWith("aidin:") },
                        onPair = { paired = it },
                        onEnrol = { i, g -> enrolled = i to g },
                    ),
                    initialInvite = "aidin:abc",
                )
            }
        }
        onNodeWithText("Pair").assertIsEnabled()
        onNodeWithText("Add to Family").assertIsEnabled()
        onAllNodesWithText("Add to Work").assertCountEquals(0)
        onNodeWithText("Add to Family").performClick()
        assertEquals("aidin:abc" to "g1", enrolled)
        assertNull(paired)
    }

    @Test
    fun showInviteRendersTheTextAndTheHostQrSlot() = runComposeUiTest {
        var asked = 0
        setContent {
            TcrTheme {
                GroupsPane(
                    running.copy(shownInvite = "aidin:xyz"),
                    GroupsActions(onShowInvite = { asked++ }),
                    qrCode = { Text("QR[$it]") },
                )
            }
        }
        onNodeWithText("aidin:xyz").assertIsDisplayed()
        onNodeWithText("QR[aidin:xyz]").assertIsDisplayed()
        onNodeWithText("Show invite").performClick()
        assertEquals(1, asked)
    }

    @Test
    fun groupsCanBeLeftAndCreated() = runComposeUiTest {
        var left: String? = null
        var created: String? = null
        setContent {
            TcrTheme {
                GroupsPane(
                    running.copy(groups = listOf(GroupUi("g1", "Family", true))),
                    GroupsActions(onLeaveGroup = { left = it }, onCreateGroup = { created = it }),
                )
            }
        }
        onNodeWithText("Family (admin)").assertIsDisplayed()
        onNodeWithText("Leave").performClick()
        assertEquals("g1", left)
        onNodeWithText("Create group").assertIsNotEnabled()
        onNodeWithText("New group name").performTextInput(" Church ")
        onNodeWithText("Create group").performClick()
        assertEquals("Church", created)
    }

    @Test
    fun devicesListShowsLinkedPeersAndActions() = runComposeUiTest {
        var unpaired: String? = null
        var removed: Pair<String, String>? = null
        val peer = GroupPeerUi(
            id = "n1", label = "Phone", shortId = "n1", connected = true, stateName = "READY",
            viaPaired = true, batteryPercent = 80, charging = true, engineVersion = "0.9",
            ramOfferedBytes = 2L * 1024 * 1024 * 1024, modelIds = listOf("acme/echo"),
            removableFromGroupIds = listOf("g1"),
        )
        setContent {
            TcrTheme {
                GroupsPane(
                    running.copy(peers = listOf(peer)),
                    GroupsActions(onUnpair = { unpaired = it }, onRemoveFromGroup = { g, p -> removed = g to p }),
                )
            }
        }
        onNodeWithText("Phone").assertIsDisplayed()
        onNodeWithText("LINKED").assertIsDisplayed()
        onNodeWithText("ready · paired · 80% ⚡ · v0.9").assertIsDisplayed()
        onNodeWithText("2048 MB RAM offered · 1 model: echo").assertIsDisplayed()
        onNodeWithText("Unpair").performClick()
        assertEquals("n1", unpaired)
        onNodeWithText("Remove from group").performClick()
        assertEquals("g1" to "n1", removed)
    }

    @Test
    fun emptyDevicesListSaysSo() = runComposeUiTest {
        setContent { TcrTheme { GroupsPane(running, GroupsActions()) } }
        onNodeWithText("No paired or linked devices yet.").assertIsDisplayed()
    }

    @Test
    fun splitSectionExplainsMissingRuntime() = runComposeUiTest {
        setContent {
            TcrTheme { GroupsPane(running.copy(split = GroupSplitUi(available = false)), GroupsActions()) }
        }
        onNodeWithText("This device needs the pinned llama-server", substring = true).assertIsDisplayed()
    }

    @Test
    fun splitSectionRunsTheChosenModelAcrossTheGroup() = runComposeUiTest {
        var started: Pair<String, String>? = null
        setContent {
            TcrTheme {
                GroupsPane(
                    running.copy(
                        groups = listOf(GroupUi("g1", "Family", true)),
                        split = GroupSplitUi(available = true, models = listOf("acme/echo")),
                    ),
                    GroupsActions(onStartSplit = { g, m -> started = g to m; "Serving $m" }),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
        }
        onNodeWithText("Run across group").performScrollTo().assertIsEnabled()
        onNodeWithText("Run across group").performClick()
        waitForIdle()
        assertEquals("g1" to "acme/echo", started)
        onNodeWithText("Serving acme/echo").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun splitSectionShowsActiveModelAndStops() = runComposeUiTest {
        var stopped = 0
        setContent {
            TcrTheme {
                GroupsPane(
                    running.copy(
                        groups = listOf(GroupUi("g1", "Family", true)),
                        split = GroupSplitUi(available = true, activeModelId = "acme/echo", models = listOf("acme/echo")),
                    ),
                    GroupsActions(onStartSplit = { _, _ -> "" }, onStopSplit = { stopped++ }),
                )
            }
        }
        onNodeWithText("Serving acme/echo across the group").assertIsDisplayed()
        onNodeWithText("Run across group").assertIsNotEnabled()
        onNodeWithText("Stop").performClick()
        waitForIdle()
        assertEquals(1, stopped)
    }

    @Test
    fun peerSummaryLinesAreWordedConsistently() {
        val offline = GroupPeerUi(id = "n2", label = null, shortId = "n2", connected = false, stateName = "IDLE", viaGroup = true, draining = true)
        assertEquals("idle · not linked · group · draining", offline.statusLine())
        assertNull(offline.capacityLine())
        val many = offline.copy(modelIds = listOf("a/x", "b/y"), ramOfferedBytes = 0)
        assertEquals("2 models: x, y", many.capacityLine())
    }
}
