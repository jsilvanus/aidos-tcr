package dev.aidos.tcr.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.aidos.tcr.a2ui.A2uiDensity
import dev.aidos.tcr.a2ui.A2uiManifest
import dev.aidos.tcr.a2ui.A2uiParseResult
import dev.aidos.tcr.a2ui.A2uiParser
import dev.aidos.tcr.a2ui.A2uiRenderer
import dev.aidos.tcr.theme.TcrTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Renders parsed A2UI manifests with the real TCR atoms and drives them as a consumer app would. */
@OptIn(ExperimentalTestApi::class)
class A2uiRendererUiTest {

    /** Renders only manifests that pass the parser, as the app does; a fixture that fails is a test bug. */
    private fun parseOk(json: String): A2uiManifest {
        val result = A2uiParser.parse(json)
        assertIs<A2uiParseResult.Ok>(result, "fixture must parse: $result")
        return result.manifest
    }

    /** The RFC-0124 example: a result card with a metric, and an action bar with "Apply Patch". */
    private val rfcExample = parseOk(
        """
        {
          "${'$'}schema": "https://aidos.dev/schemas/a2ui-v1.json",
          "manifestId": "ui_manifest_42",
          "targetNodeId": "goal_node_audio_buffer",
          "root": {
            "type": "InlineCard",
            "props": { "intent": "accent", "density": "compact", "elevation": "glass" },
            "slots": {
              "header": {
                "type": "Row",
                "children": [
                  { "type": "TextSpan", "props": { "text": "Buffer Optimization Result", "weight": "bold" } }
                ]
              },
              "body": {
                "type": "MetricCluster",
                "children": [
                  { "type": "MetricValue", "props": { "label": "Latency", "value": "18ms", "delta": "-14ms" } }
                ]
              },
              "footer": {
                "type": "ActionBar",
                "children": [
                  { "type": "ActionButton", "props": { "label": "Apply Patch", "actionId": "apply_patch", "intent": "primary" } }
                ]
              }
            }
          }
        }
        """.trimIndent(),
    )

    @Test fun rendersTheRfcExampleWithTcrComponents() = runComposeUiTest {
        setContent { TcrTheme { A2uiRenderer(rfcExample, A2uiDensity.REGULAR, onAction = {}) } }
        onNodeWithText("Buffer Optimization Result").assertIsDisplayed()
        onNodeWithText("Latency: 18ms (-14ms)").assertIsDisplayed()
        onNodeWithText("Apply Patch").assertIsDisplayed()
    }

    @Test fun pressingAnActionButtonReportsItsActionId() = runComposeUiTest {
        val actions = mutableListOf<String>()
        setContent { TcrTheme { A2uiRenderer(rfcExample, A2uiDensity.REGULAR, onAction = { actions += it }) } }
        onNodeWithText("Apply Patch").performClick()
        assertEquals(listOf("apply_patch"), actions)
    }

    @Test fun disabledActionsAreDrawnDisabledAndDoNotFire() = runComposeUiTest {
        val actions = mutableListOf<String>()
        setContent {
            TcrTheme {
                A2uiRenderer(rfcExample, A2uiDensity.REGULAR, onAction = { actions += it }, actionsEnabled = false)
            }
        }
        onNodeWithText("Apply Patch").assertIsNotEnabled()
        onNodeWithText("Apply Patch").performClick()
        assertEquals(emptyList(), actions)
    }

    @Test fun togglingActionsEnabledUpdatesTheButton() = runComposeUiTest {
        var enabled by mutableStateOf(false)
        val actions = mutableListOf<String>()
        setContent {
            TcrTheme {
                A2uiRenderer(rfcExample, A2uiDensity.REGULAR, onAction = { actions += it }, actionsEnabled = enabled)
            }
        }
        onNodeWithText("Apply Patch").assertIsNotEnabled()
        enabled = true
        waitForIdle()
        onNodeWithText("Apply Patch").performClick()
        assertEquals(listOf("apply_patch"), actions)
    }

    @Test fun rendersStepCounterScopePathAndDiffChunk() = runComposeUiTest {
        val manifest = parseOk(
            """
            {
              "${'$'}schema": "https://aidos.dev/schemas/a2ui-v1.json",
              "manifestId": "ui_manifest_43",
              "targetNodeId": "goal_node_patch",
              "root": {
                "type": "Column",
                "children": [
                  { "type": "StepCounter", "props": { "current": 2, "total": 5 } },
                  { "type": "ScopePathChip", "props": { "path": "src/Foo.kt" } },
                  { "type": "DiffChunk", "props": { "path": "src/Foo.kt", "diff": "-val a = 1\n+val a = 2" } }
                ]
              }
            }
            """.trimIndent(),
        )
        setContent { TcrTheme { A2uiRenderer(manifest, A2uiDensity.COMPACT, onAction = {}) } }
        onNodeWithText("Step 2 / 5").assertIsDisplayed()
        onNodeWithText("src/Foo.kt", substring = false).assertIsDisplayed()
        onNodeWithText("- val a = 1").assertIsDisplayed()
        onNodeWithText("+ val a = 2").assertIsDisplayed()
    }
}
