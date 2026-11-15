package dev.aidos.tcr

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import dev.aidos.tcr.agent.AgentLocality
import dev.aidos.tcr.agent.AgentPlacement
import dev.aidos.tcr.agent.AgentRunUiModel
import dev.aidos.tcr.agent.formatDuration
import dev.aidos.tcr.agent.matchesSearch
import dev.aidos.tcr.engine.AppApprovalRecordUi
import dev.aidos.tcr.engine.AppApprovalStatus
import dev.aidos.tcr.engine.formatActionableErrorMessage
import dev.aidos.tcr.engine.formatElapsed
import dev.aidos.tcr.engine.formatMb
import dev.aidos.tcr.engine.formatSize
import dev.aidos.tcr.engine.parseHostPort
import dev.aidos.tcr.engine.parseShellScope
import dev.aidos.tcr.engine.parseToolNames
import dev.aidos.tcr.engine.shellScopeString
import dev.aidos.tcr.layout.FormFactor
import dev.aidos.tcr.layout.WindowBreakpoints
import dev.aidos.tcr.layout.computeFormFactor
import dev.aidos.tcr.overlay.KeyAction
import dev.aidos.tcr.overlay.KeyboardShortcutRegistry
import dev.aidos.tcr.spatial.canvas.ForceDirectedLayout
import dev.aidos.tcr.spatial.canvas.LodLevel
import dev.aidos.tcr.spatial.canvas.SemanticZoomManager
import dev.aidos.tcr.spatial.edge.EdgeCollisionRouter
import dev.aidos.tcr.theme.TcrColors
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pure helpers consumers rely on for display strings, parsing and layout decisions. */
class FormattersAndRoutingTest {

    // --- formatters -----------------------------------------------------------------

    @Test fun formatSizeRoundsUpKilobytesAndUsesTenthsAboveOneMegabyte() {
        assertEquals("size unknown", formatSize(null))
        assertEquals("size unknown", formatSize(0))
        assertEquals("1 KB", formatSize(1))
        assertEquals("2 KB", formatSize(1025))
        assertEquals("1 MB", formatSize(1024L * 1024))
        assertEquals("1.5 MB", formatSize(1024L * 1024 * 3 / 2))
        assertEquals("4 GB", formatSize(4L * 1024 * 1024 * 1024))
        assertEquals("2.3 GB", formatSize((2.26 * 1024 * 1024 * 1024).toLong()))
    }

    @Test fun formatMbSwitchesToGigabytesAtOneGiB() {
        assertEquals("512 MB", formatMb(512))
        assertEquals("1 GB", formatMb(1024))
        assertEquals("1.5 GB", formatMb(1536))
    }

    @Test fun formatElapsedPicksTheLargestFittingUnit() {
        assertEquals("just now", formatElapsed(999))
        assertEquals("1s ago", formatElapsed(1_000))
        assertEquals("59s ago", formatElapsed(59_999))
        assertEquals("1m ago", formatElapsed(60_000))
        assertEquals("1h ago", formatElapsed(3_600_000))
        assertEquals("2d ago", formatElapsed(2 * 86_400_000L))
    }

    @Test fun formatDurationHandlesMissingAndClockSkew() {
        assertEquals("Unknown", formatDuration(null, 5))
        assertEquals("Unknown", formatDuration(0, 5))
        assertEquals("250ms", formatDuration(1000, 1250))
        assertEquals("5s", formatDuration(1000, 6000))
        assertEquals("2m 5s", formatDuration(0L + 1, 1 + 125_000))
        // Still running (no finish time) and finish-before-start both read as zero, not negative.
        assertEquals("0ms", formatDuration(1000, null))
        assertEquals("0ms", formatDuration(2000, 1000))
    }

    @Test fun actionableErrorsTranslateEngineCodesAndPassUnknownTextThrough() {
        assertEquals("Inference failed", formatActionableErrorMessage(null))
        assertEquals("Inference failed", formatActionableErrorMessage("  "))
        assertTrue(formatActionableErrorMessage("MODEL_UNAVAILABLE: none").startsWith("No candidate models"))
        assertTrue(formatActionableErrorMessage("peer_busy").contains("busy"))
        assertTrue(formatActionableErrorMessage("CUDA out of memory").startsWith("Out of memory"))
        assertTrue(formatActionableErrorMessage("locality_violation").startsWith("Placement restricted"))
        assertEquals("something odd happened", formatActionableErrorMessage("something odd happened"))
    }

    // --- parsers ---------------------------------------------------------------------

    @Test fun parseHostPortAcceptsValidAddressesOnly() {
        assertEquals("127.0.0.1" to 8080, parseHostPort(" 127.0.0.1:8080 "))
        assertEquals("host.local" to 1, parseHostPort("host.local:1"))
        assertEquals("[::1]" to 65535, parseHostPort("[::1]:65535"))
        assertNull(parseHostPort("host"))
        assertNull(parseHostPort(":80"))
        assertNull(parseHostPort("host:"))
        assertNull(parseHostPort("host:0"))
        assertNull(parseHostPort("host:65536"))
        assertNull(parseHostPort("host:abc"))
        assertNull(parseHostPort("ho st:80"))
    }

    @Test fun parseToolNamesSplitsOnAnySeparatorKeepsOrderAndDedupes() {
        assertEquals(listOf("read", "write", "grep"), parseToolNames("read, write\ngrep  read\t,"))
        assertEquals(emptyList(), parseToolNames("  ,\n "))
    }

    @Test fun shellScopeRoundTripsAndRejectsOtherScopes() {
        assertEquals("shell:/work", shellScopeString("/work", null))
        assertEquals("shell:/work", shellScopeString("/work", emptyList()))
        assertEquals("shell:.|git,ls", shellScopeString("  ", listOf("git", "ls")))
        assertEquals("/work" to null, parseShellScope("shell:/work"))
        assertEquals("/work" to listOf("git", "ls"), parseShellScope("shell:/work|git, ls"))
        assertEquals("." to emptyList(), parseShellScope("shell:|"))
        assertNull(parseShellScope("fs:/work"))
        assertNull(parseShellScope(null))
    }

    // --- search ----------------------------------------------------------------------

    @Test fun agentRunSearchMatchesAcrossFieldsCaseInsensitively() {
        val run = AgentRunUiModel(runId = "run-42", ownerApp = "fi.italeino.dictator", modelId = "Qwen-7B", promptPreview = "Summarise the sermon")
        assertTrue(run.matchesSearch(""))
        assertTrue(run.matchesSearch("   "))
        assertTrue(run.matchesSearch("RUN-42"))
        assertTrue(run.matchesSearch("dictator"))
        assertTrue(run.matchesSearch("qwen"))
        assertTrue(run.matchesSearch("sermon"))
        assertTrue(!run.matchesSearch("gemma"))
    }

    // --- layout / navigation -----------------------------------------------------------

    @Test fun formFactorBreakpointsAreExactlyAtMaterialWindowSizeClasses() {
        assertEquals(FormFactor.HANDHELD_COMPACT, computeFormFactor(0f))
        assertEquals(FormFactor.HANDHELD_COMPACT, computeFormFactor(599.9f))
        assertEquals(FormFactor.HANDHELD_EXPANDED, computeFormFactor(600f))
        assertEquals(FormFactor.HANDHELD_EXPANDED, computeFormFactor(839.9f))
        assertEquals(FormFactor.DESKTOP_DOCKED, computeFormFactor(840f))
        assertEquals(840f, WindowBreakpoints.DOCKED_MIN_WIDTH_DP)
    }

    @Test fun semanticZoomLevelsSwitchAtDocumentedThresholds() {
        assertEquals(LodLevel.MACRO_GALAXY, SemanticZoomManager.computeLod(0.05f))
        assertEquals(LodLevel.MACRO_GALAXY, SemanticZoomManager.computeLod(0.349f))
        assertEquals(LodLevel.MESO_CLUSTER, SemanticZoomManager.computeLod(0.35f))
        assertEquals(LodLevel.MESO_CLUSTER, SemanticZoomManager.computeLod(0.749f))
        assertEquals(LodLevel.MICRO_BLUEPRINT, SemanticZoomManager.computeLod(0.75f))
        assertEquals(LodLevel.MICRO_BLUEPRINT, SemanticZoomManager.computeLod(4f))
    }

    @Test fun keyboardShortcutsResolveModifiersBeforePlainKeys() {
        assertEquals(KeyAction.OPEN_COMMAND_PALETTE, KeyboardShortcutRegistry.resolveKeyAction("k", true))
        assertEquals(KeyAction.OPEN_COMMAND_PALETTE, KeyboardShortcutRegistry.resolveKeyAction("K", true))
        assertEquals(KeyAction.NAVIGATE_UP, KeyboardShortcutRegistry.resolveKeyAction("k", false))
        assertEquals(KeyAction.OPEN_ACTION_INBOX, KeyboardShortcutRegistry.resolveKeyAction("i", true))
        assertNull(KeyboardShortcutRegistry.resolveKeyAction("i", false))
        assertEquals(KeyAction.NAVIGATE_DOWN, KeyboardShortcutRegistry.resolveKeyAction("j", false))
        assertEquals(KeyAction.TOGGLE_COMPLETE, KeyboardShortcutRegistry.resolveKeyAction(" ", false))
        assertEquals(KeyAction.TOGGLE_COMPLETE, KeyboardShortcutRegistry.resolveKeyAction("space", false))
        assertEquals(KeyAction.ESCAPE, KeyboardShortcutRegistry.resolveKeyAction("escape", false))
        assertNull(KeyboardShortcutRegistry.resolveKeyAction("x", false))
    }

    @Test fun layoutPlacesEveryNodeOnceAndHandlesEmptyInput() {
        assertTrue(ForceDirectedLayout.layoutDag(emptyList()).isEmpty())
        val ids = (1..12).map { "n$it" }
        val layout = ForceDirectedLayout.layoutDag(ids)
        assertEquals(ids.toSet(), layout.keys)
        // Deterministic: the same input always yields the same canvas positions.
        assertEquals(layout, ForceDirectedLayout.layoutDag(ids))
        // Distinct positions, so nodes never stack exactly on top of each other.
        assertEquals(ids.size, layout.values.toSet().size)
        // The first node starts at angle 0, radius 300, around the canvas centre.
        val (x, y) = layout.getValue("n1")
        assertEquals(1300f, x, 0.01f)
        assertEquals(1000f, y, 0.01f)
    }

    @Test fun edgeRouterCurrentlyReturnsEndpointsUnchanged() {
        // Documents today's behaviour: obstacle avoidance is not implemented yet. When it is,
        // this test is the one to replace (and the manifest contract to bump).
        val a = Offset(0f, 0f)
        val b = Offset(10f, 10f)
        assertEquals(a to b, EdgeCollisionRouter.routeAroundObstacles(a, b, listOf(Rect(2f, 2f, 8f, 8f))))
    }

    // --- tokens & wire formats ---------------------------------------------------------

    @Test fun semanticColorsMatchTheManual() {
        assertEquals(Color(0xFFEF4444), TcrColors.EmberRed)
        assertEquals(Color(0xFF3B82F6), TcrColors.ElectricBlue)
        assertEquals(Color(0xFF10B981), TcrColors.EmeraldGreen)
        assertEquals(Color(0xFFF59E0B), TcrColors.AmberWarning)
        assertEquals(Color(0xFF8B5CF6), TcrColors.VioletProtocol)
    }

    @Test fun placementAndLocalityWireNamesAreStableAndUnknownValuesFailSafe() {
        assertEquals(listOf("local", "cluster", "remote"), AgentPlacement.entries.map { it.wire })
        assertEquals(listOf("local_only", "trusted_cluster", "any"), AgentLocality.entries.map { it.wire })
        assertEquals(AgentPlacement.CLUSTER, AgentPlacement.fromWire("CLUSTER"))
        // Unknown placement is treated as the *least* trusted one, unknown locality as the widest.
        assertEquals(AgentPlacement.REMOTE, AgentPlacement.fromWire("mars"))
        assertEquals(AgentLocality.ANY, AgentLocality.fromWire("mars"))
    }

    @Test fun serializedModelsRoundTripAndTolerateMissingOptionalFields() {
        val json = Json { ignoreUnknownKeys = true }
        val run = AgentRunUiModel(runId = "r", ownerApp = "a", modelId = "m", placement = AgentPlacement.REMOTE, locality = AgentLocality.ANY)
        assertEquals(run, json.decodeFromString(AgentRunUiModel.serializer(), json.encodeToString(AgentRunUiModel.serializer(), run)))
        val app = json.decodeFromString(
            AppApprovalRecordUi.serializer(),
            """{"packageName":"p","displayName":"P","status":"PENDING","futureField":1}""",
        )
        assertEquals(AppApprovalStatus.PENDING, app.status)
        assertEquals(1, app.attemptCount)
    }
}
