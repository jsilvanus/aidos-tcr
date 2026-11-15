package dev.aidos.tcr.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.aidos.tcr.agent.AgentLocality
import dev.aidos.tcr.agent.AgentPlacement
import dev.aidos.tcr.agent.AgentPlacementBadge
import dev.aidos.tcr.engine.AppApprovalRecordUi
import dev.aidos.tcr.engine.AppApprovalStatus
import dev.aidos.tcr.engine.ConnectedAppsPane
import dev.aidos.tcr.generative.GenerativeUiRegistry
import dev.aidos.tcr.mesh.ApprovalCard
import dev.aidos.tcr.mesh.ContactRow
import dev.aidos.tcr.model.ContactUiModel
import dev.aidos.tcr.model.DisclosureStatus
import dev.aidos.tcr.model.TimelineItemUiModel
import dev.aidos.tcr.theme.TcrTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Headless Compose UI tests: render the shared components inside [TcrTheme] and drive them the way
 * a consumer app does. They pin behaviour consumers depend on (callbacks fire with the right id,
 * state decides which controls exist), not pixels.
 */
@OptIn(ExperimentalTestApi::class)
class ComponentUiTest {

    @Test fun themeHostsContent() = runComposeUiTest {
        setContent { TcrTheme { Text("hello") } }
        onNodeWithText("hello").assertIsDisplayed()
    }

    // --- mesh.ApprovalCard -------------------------------------------------------------

    @Test fun pendingApprovalCardOffersBothActionsAndReportsTheRequestId() = runComposeUiTest {
        val approved = mutableListOf<String>()
        val declined = mutableListOf<String>()
        setContent {
            TcrTheme {
                ApprovalCard(
                    requestId = "req-7", topicKey = "calendar", promptText = "Share your availability?",
                    proposedValue = "Mon-Fri 9-17", status = DisclosureStatus.PENDING,
                    onApprove = { approved += it }, onDecline = { declined += it },
                )
            }
        }
        onNodeWithText("⚡ Action Required").assertIsDisplayed()
        onNodeWithText("Share your availability?").assertIsDisplayed()
        onNodeWithText("Mon-Fri 9-17").assertIsDisplayed()
        onNodeWithText("Share Data").performClick()
        onNodeWithText("Decline").performClick()
        assertEquals(listOf("req-7"), approved)
        assertEquals(listOf("req-7"), declined)
    }

    @Test fun decidedApprovalCardsHaveNoActions() = runComposeUiTest {
        var status by mutableStateOf(DisclosureStatus.APPROVED)
        setContent {
            TcrTheme { ApprovalCard(requestId = "r", topicKey = "t", promptText = "p", status = status) }
        }
        onNodeWithText("✅ Approved & Disclosed").assertIsDisplayed()
        onNodeWithText("Share Data").assertDoesNotExist()
        status = DisclosureStatus.DECLINED
        waitForIdle()
        onNodeWithText("✕ Declined").assertIsDisplayed()
        onNodeWithText("Decline").assertDoesNotExist()
    }

    // --- engine.ConnectedAppsPane ------------------------------------------------------

    private fun app(pkg: String, status: AppApprovalStatus) =
        AppApprovalRecordUi(packageName = pkg, displayName = "App $pkg", status = status)

    @Test fun connectedAppsPaneGroupsBySectionAndRoutesActionsByPackage() = runComposeUiTest {
        val log = mutableListOf<String>()
        setContent {
            TcrTheme {
                ConnectedAppsPane(
                    approvals = listOf(
                        app("p.new", AppApprovalStatus.PENDING),
                        app("p.ok", AppApprovalStatus.APPROVED),
                        app("p.no", AppApprovalStatus.DENIED),
                    ),
                    isEngineRunning = true,
                    onApprove = { log += "approve:$it" }, onDeny = { log += "deny:$it" },
                    onRevoke = { log += "revoke:$it" }, onUndoDeny = { log += "undo:$it" },
                )
            }
        }
        onNodeWithText("Approval Pending").assertIsDisplayed()
        onNodeWithText("Approved").assertIsDisplayed()
        onNodeWithText("Denied").assertIsDisplayed()
        onNodeWithText("Approve").performClick()
        onNodeWithText("Deny").performClick()
        onNodeWithText("Revoke").performClick()
        onNodeWithText("Undo Deny").performClick()
        assertEquals(listOf("approve:p.new", "deny:p.new", "revoke:p.ok", "undo:p.no"), log)
    }

    @Test fun connectedAppsPaneExplainsWhenEngineIsStoppedOrEmpty() = runComposeUiTest {
        var running by mutableStateOf(false)
        setContent {
            TcrTheme {
                ConnectedAppsPane(
                    approvals = emptyList(), isEngineRunning = running,
                    onApprove = {}, onDeny = {}, onRevoke = {}, onUndoDeny = {},
                    notRunningText = "engine is off",
                )
            }
        }
        onNodeWithText("engine is off").assertIsDisplayed()
        running = true
        waitForIdle()
        onNodeWithText("engine is off").assertDoesNotExist()
        onNodeWithText("No apps have requested access yet.", substring = true).assertIsDisplayed()
    }

    // --- agent.AgentPlacementBadge -----------------------------------------------------

    @Test fun placementBadgeShowsModelPlacementAndLocalityLabels() = runComposeUiTest {
        setContent {
            TcrTheme {
                AgentPlacementBadge(modelId = "qwen-7b", placement = AgentPlacement.CLUSTER, locality = AgentLocality.TRUSTED_CLUSTER)
            }
        }
        onNodeWithText("🤖 qwen-7b").assertIsDisplayed()
        onNodeWithText(AgentPlacement.CLUSTER.label, substring = true).assertIsDisplayed()
        onNodeWithText(AgentLocality.TRUSTED_CLUSTER.label, substring = true).assertIsDisplayed()
    }

    // --- mesh.ContactRow ---------------------------------------------------------------

    @Test fun contactRowShowsIdentityCountersAndHandlesClicks() = runComposeUiTest {
        var clicks = 0
        setContent {
            TcrTheme {
                ContactRow(
                    contact = ContactUiModel(
                        contactId = "c1", displayName = "Maria", handle = "maria@amp", mailboxId = "m1",
                        isAidosActive = true, unreadCount = 3, pendingDisclosureCount = 2,
                        latestSnippet = "See you tomorrow",
                    ),
                    onClick = { clicks++ },
                )
            }
        }
        onNodeWithText("Maria").assertIsDisplayed()
        onNodeWithText("🤖 Aidos").assertIsDisplayed()
        onNodeWithText("See you tomorrow").assertIsDisplayed()
        onNodeWithText("3").assertIsDisplayed()
        onNodeWithText("⚡ 2").assertIsDisplayed()
        onNodeWithText("Maria").performClick()
        assertEquals(1, clicks)
    }

    @Test fun contactRowOmitsAgentBadgeAndCountersWhenQuiet() = runComposeUiTest {
        setContent {
            TcrTheme { ContactRow(contact = ContactUiModel(contactId = "c2", displayName = "Olli", handle = "olli@amp", mailboxId = "m2")) }
        }
        onNodeWithText("Olli").assertIsDisplayed()
        onNodeWithText("🤖 Aidos").assertDoesNotExist()
        onNodeWithText("⚡ 0").assertDoesNotExist()
    }

    // --- generative.GenerativeUiRegistry.RenderItem ------------------------------------

    @Test fun registryDispatchesDisclosureRequestsToApprovalCardWithCallbacks() = runComposeUiTest {
        val approved = mutableListOf<String>()
        setContent {
            TcrTheme {
                GenerativeUiRegistry.RenderItem(
                    item = TimelineItemUiModel.ProgressiveDisclosureRequest(
                        id = "e1", timestamp = 1L, requestId = "rq", requester = principal("agent"),
                        topicKey = "location", promptText = "Where are you?", proposedPayloadSnippet = null,
                        status = DisclosureStatus.PENDING,
                    ),
                    currentUserId = "me",
                    onApproveDisclosure = { approved += it },
                )
            }
        }
        onNodeWithText("Where are you?").assertIsDisplayed()
        onNodeWithText("Share Data").performClick()
        assertEquals(listOf("rq"), approved)
    }

    @Test fun registryDispatchesCalendarProposals() = runComposeUiTest {
        var confirmed = 0
        setContent {
            TcrTheme {
                GenerativeUiRegistry.RenderItem(
                    item = TimelineItemUiModel.CalendarProposal(
                        id = "e2", timestamp = 1L, eventTitle = "Staff meeting", proposedTime = "Tue 10:00",
                        conflictSummary = null, isActionTaken = false,
                    ),
                    currentUserId = "me",
                    onConfirmCalendar = { confirmed++ },
                )
            }
        }
        onNodeWithText("Staff meeting", substring = true).assertIsDisplayed()
        assertEquals(0, confirmed)
    }

    private fun principal(id: String) =
        dev.aidos.tcr.model.EndpointIdentifier(principalId = id, endpointType = dev.aidos.tcr.model.EndpointType.AGENT_DELEGATE, deviceKeyId = "k")
}
