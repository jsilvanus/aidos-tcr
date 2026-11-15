package dev.aidos.tcr.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.aidos.tcr.agent.AgentFilterTab
import dev.aidos.tcr.agent.AgentOverviewUiState
import dev.aidos.tcr.agent.AgentsPane
import dev.aidos.tcr.agent.SampleAgentRuns
import dev.aidos.tcr.engine.MemoryBudget
import dev.aidos.tcr.engine.ResidentModel
import dev.aidos.tcr.engine.StatusPane
import dev.aidos.tcr.engine.StatusUiState
import dev.aidos.tcr.generative.GenerativeUiRegistry
import dev.aidos.tcr.mcp.McpAdoptionFilter
import dev.aidos.tcr.mcp.McpServerUiModel
import dev.aidos.tcr.mcp.McpServersPane
import dev.aidos.tcr.mcp.McpToolUiModel
import dev.aidos.tcr.mcp.McpTransportFilter
import dev.aidos.tcr.mcp.McpUiState
import dev.aidos.tcr.mesh.MessageBubble
import dev.aidos.tcr.model.AssertionLevel
import dev.aidos.tcr.model.EndpointIdentifier
import dev.aidos.tcr.model.EndpointType
import dev.aidos.tcr.model.EnvelopePayload
import dev.aidos.tcr.model.InformationBoundaryPolicy
import dev.aidos.tcr.model.PrivacyLevel
import dev.aidos.tcr.model.ProvenanceRecord
import dev.aidos.tcr.model.RoutingHeader
import dev.aidos.tcr.model.TimelineItemUiModel
import dev.aidos.tcr.model.UniversalEnvelope
import dev.aidos.tcr.theme.TcrTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Headless UI tests for the Engine/Agents/MCP panes and the message bubble. */
@OptIn(ExperimentalTestApi::class)
class PanesUiTest {

    // --- engine.StatusPane -------------------------------------------------------------

    @Test fun statusPaneExplainsAStoppedEngine() = runComposeUiTest {
        setContent { TcrTheme { StatusPane(state = StatusUiState(), engineControl = { Text("CONTROL") }) } }
        onNodeWithText("CONTROL").assertIsDisplayed()
        onNodeWithText("Start the Engine to load and list models.").assertIsDisplayed()
        onNodeWithText("Start the Engine to see linked devices").assertIsDisplayed()
        onNodeWithText("Start the Engine to see connected apps").assertIsDisplayed()
        onNodeWithText("Storage space unknown").assertIsDisplayed()
    }

    @Test fun statusPaneSummarisesARunningEngine() = runComposeUiTest {
        setContent {
            TcrTheme {
                StatusPane(
                    state = StatusUiState(
                        isEngineRunning = true, engineStateName = "READY",
                        residentModels = listOf(ResidentModel("m1", "Gemma 2B", "Q4_K_M", 5_000)),
                        memory = MemoryBudget(2048, 8192), freeStorageGb = 42,
                        approvedApps = listOf("a", "b"), pendingAppCount = 1, linkedPeerCount = 2,
                    ),
                    engineControl = {},
                )
            }
        }
        onNodeWithText("Gemma 2B", substring = true).assertIsDisplayed()
        onNodeWithText("2 peer devices linked for cluster inference").assertIsDisplayed()
        onNodeWithText("42 GB free").assertIsDisplayed()
        onNodeWithText("2 approved · 1 pending approval").assertIsDisplayed()
    }

    // --- agent.AgentsPane --------------------------------------------------------------

    private fun agentsPane(
        state: AgentOverviewUiState,
        cancelled: MutableList<String> = mutableListOf(),
        inspected: MutableList<String?> = mutableListOf(),
        reset: () -> Unit = {},
        status: (AgentFilterTab) -> Unit = {},
    ): @androidx.compose.runtime.Composable () -> Unit = {
        TcrTheme {
            AgentsPane(
                state = state,
                onAppFilterSelected = {}, onStatusFilterSelected = status,
                onCancelRun = { cancelled += it }, onInspectRun = { inspected += it },
                onResetFilters = reset,
            )
        }
    }

    @Test fun agentsPaneShowsAnEmptyStateWithoutRuns() = runComposeUiTest {
        setContent(agentsPane(AgentOverviewUiState()))
        onNodeWithText("No agent runs recorded yet").assertIsDisplayed()
    }

    @Test fun agentsPaneListsSampleRunsAndFilterTabsReportSelection() = runComposeUiTest {
        val runs = SampleAgentRuns.create()
        val tabs = mutableListOf<AgentFilterTab>()
        setContent(agentsPane(AgentOverviewUiState(runs = runs), status = { tabs += it }))
        onNodeWithText("Execution Status").assertIsDisplayed()
        onNodeWithText("All Apps (${runs.size})").assertIsDisplayed()
        onNodeWithText(AgentFilterTab.FAILED.label).performClick()
        assertEquals(listOf(AgentFilterTab.FAILED), tabs)
    }

    @Test fun agentsPaneOffersFilterResetWhenSearchMatchesNothing() = runComposeUiTest {
        var resets = 0
        setContent(
            agentsPane(
                AgentOverviewUiState(runs = SampleAgentRuns.create(), searchQuery = "zzz-no-such-run"),
                reset = { resets++ },
            ),
        )
        onNodeWithText("No agent runs match the selected filters or search").assertIsDisplayed()
        onNodeWithText("Reset All Filters").performClick()
        assertEquals(1, resets)
    }

    // --- mcp.McpServersPane ------------------------------------------------------------

    private val server = McpServerUiModel(
        id = "srv-1", name = "Files", transport = "stdio", command = "mcp-files",
        toolCount = 3, adoptedCount = 1,
    )
    private val tools = listOf(
        McpToolUiModel(name = "read_file", serverId = "srv-1", description = "Read a file", isAdopted = true),
        McpToolUiModel(name = "write_file", serverId = "srv-1", description = "Write a file"),
    )

    private class McpCalls {
        val selected = mutableListOf<String?>()
        val adopt = mutableListOf<Triple<String, String, Boolean>>()
        val adoptAll = mutableListOf<String>()
        val refresh = mutableListOf<String>()
    }

    private fun mcpPane(state: McpUiState, c: McpCalls): @androidx.compose.runtime.Composable () -> Unit = {
        TcrTheme {
            McpServersPane(
                state = state,
                onAddServer = {}, onDeleteServer = {},
                onSelectServer = { c.selected += it },
                onToggleAdoption = { s, t, a -> c.adopt += Triple(s, t, a) },
                onAdoptAll = { c.adoptAll += it }, onUnadoptAll = {},
                onRefreshCatalog = { c.refresh += it },
                onSearchQueryChange = {}, onTransportFilterChange = {}, onAdoptionFilterChange = {},
            )
        }
    }

    @Test fun mcpPaneShowsEmptyStateAndServerCards() = runComposeUiTest {
        val calls = McpCalls()
        var state by mutableStateOf(McpUiState())
        setContent { mcpPane(state, calls)() }
        onNodeWithText("MCP Servers & Tools").assertIsDisplayed()
        onNodeWithText("No MCP servers configured yet.").assertIsDisplayed()
        state = McpUiState(servers = listOf(server))
        waitForIdle()
        onNodeWithText("Files").assertIsDisplayed()
        onNodeWithText("1 of 3 tools adopted").assertIsDisplayed()
        onNodeWithText("Inspect & Adopt Tools").performClick()
        assertEquals(listOf<String?>("srv-1"), calls.selected)
    }

    @Test fun mcpCatalogAdoptsAndUnadoptsPerToolAndNavigatesBack() = runComposeUiTest {
        val calls = McpCalls()
        setContent(mcpPane(McpUiState(servers = listOf(server), selectedServerId = "srv-1", selectedCatalog = tools), calls))
        onNodeWithText("Files Tools Catalog").assertIsDisplayed()
        onNodeWithText("Adopt Tool").performClick()
        onNodeWithText("Unadopt").performClick()
        assertEquals(
            listOf(Triple("srv-1", "write_file", true), Triple("srv-1", "read_file", false)),
            calls.adopt,
        )
        onNodeWithText("Adopt All").performClick()
        assertEquals(listOf("srv-1"), calls.adoptAll)
        onNodeWithText("← Back to Servers").performClick()
        assertEquals(listOf<String?>(null), calls.selected)
    }

    @Test fun mcpCatalogAdoptionFilterNarrowsTheList() = runComposeUiTest {
        val calls = McpCalls()
        setContent(
            mcpPane(
                McpUiState(
                    servers = listOf(server), selectedServerId = "srv-1", selectedCatalog = tools,
                    adoptionFilter = McpAdoptionFilter.ADOPTED, transportFilter = McpTransportFilter.ALL,
                ),
                calls,
            ),
        )
        onNodeWithText("read_file").assertIsDisplayed()
        onNodeWithText("write_file").assertDoesNotExist()
    }

    // --- mesh.MessageBubble ------------------------------------------------------------

    private fun message(text: String, privacy: PrivacyLevel = PrivacyLevel.AGENT_ACCESSIBLE) =
        GenerativeUiRegistry.mapEnvelopeToTimelineItem(
            UniversalEnvelope(
                envelopeId = "env-1", conversationId = "c", epoch = 1L, timestamp = 1_000L,
                routing = RoutingHeader(
                    sender = EndpointIdentifier("did:aidos:maria", EndpointType.HUMAN_DIRECT, "k1"),
                    recipients = emptyList(),
                ),
                provenance = ProvenanceRecord(AssertionLevel.ORIGINAL_HUMAN, "key-abc-123456", "00"),
                policy = InformationBoundaryPolicy(privacyLevel = privacy),
                payload = EnvelopePayload(naturalLanguage = text),
            ),
        ) as TimelineItemUiModel.Message

    @Test fun messageBubbleShowsTextForBothSidesOfAConversation() = runComposeUiTest {
        var self by mutableStateOf(false)
        setContent { TcrTheme { MessageBubble(message = message("Hei kaikille"), isSelf = self) } }
        onNodeWithText("Hei kaikille", substring = true).assertIsDisplayed()
        self = true
        waitForIdle()
        onNodeWithText("Hei kaikille", substring = true).assertIsDisplayed()
    }

    @Test fun humanOnlyMessagesCarryLockIndicatorsThatOpenMessagesLack() = runComposeUiTest {
        var privacy by mutableStateOf(PrivacyLevel.AGENT_ACCESSIBLE)
        setContent { TcrTheme { MessageBubble(message = message("secret", privacy)) } }
        onAllNodesWithText("🔒", substring = true).assertCountEquals(0)
        privacy = PrivacyLevel.HUMAN_ONLY
        waitForIdle()
        assertTrue(onAllNodesWithText("🔒", substring = true).fetchSemanticsNodes().isNotEmpty())
    }
}
