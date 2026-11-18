package dev.aidos.tcr.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.aidos.tcr.engine.CookbookModel
import dev.aidos.tcr.engine.EngineState
import dev.aidos.tcr.engine.ModelDetail
import dev.aidos.tcr.engine.ModelDetailActions
import dev.aidos.tcr.engine.ModelDetailContent
import dev.aidos.tcr.engine.ModelDetailState
import dev.aidos.tcr.engine.ModelFitVerdict
import dev.aidos.tcr.engine.ModelLoadingState
import dev.aidos.tcr.engine.ModelsPaneActions
import dev.aidos.tcr.engine.ModelsPaneState
import dev.aidos.tcr.engine.ModelsTabs
import dev.aidos.tcr.theme.TcrTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Headless UI tests for the Models tabs and the model detail view (D46: the TCR owns this UI). */
@OptIn(ExperimentalTestApi::class)
class ModelsUiTest {

    private val installed = CookbookModel(
        id = "acme/echo", name = "Echo", kind = "LLM", quantization = "Q4_K_M",
        sizeBytes = 2_411_724L, contextLength = 2048, fitVerdict = ModelFitVerdict.RUNS_WELL,
    )

    @Test
    fun tabsListTheThreeSections() = runComposeUiTest {
        setContent { TcrTheme { ModelsTabs(ModelsPaneState(engineState = EngineState.READY), ModelsPaneActions()) } }
        onNodeWithText("Local").assertIsDisplayed()
        onNodeWithText("Cookbook").assertIsDisplayed()
        onNodeWithText("Providers").assertIsDisplayed()
    }

    @Test
    fun bannerExplainsAStoppedOrFailedEngine() = runComposeUiTest {
        var state by mutableStateOf(ModelsPaneState(engineState = EngineState.STARTING))
        setContent { TcrTheme { ModelsTabs(state, ModelsPaneActions()) } }
        onNodeWithText("The Engine is starting or turned off.", substring = true).assertIsDisplayed()
        state = ModelsPaneState(engineState = EngineState.FAILED)
        waitForIdle()
        onNodeWithText("The Engine failed to start", substring = true).assertIsDisplayed()
    }

    @Test
    fun readyEngineShowsNoBanner() = runComposeUiTest {
        setContent { TcrTheme { ModelsTabs(ModelsPaneState(engineState = EngineState.READY), ModelsPaneActions()) } }
        onAllNodesWithText("The Engine is starting", substring = true).assertCountEquals(0)
    }

    @Test
    fun localTabShowsInstalledModelAndReportsSelection() = runComposeUiTest {
        var selected: String? = null
        setContent {
            TcrTheme {
                ModelsTabs(
                    ModelsPaneState(engineState = EngineState.READY, localModels = listOf(installed)),
                    ModelsPaneActions(onModelSelected = { selected = it }),
                )
            }
        }
        onNodeWithText("Local").performClick()
        onNodeWithText("Echo").assertIsDisplayed()
        onNodeWithText("Installed · tap to load or test").assertIsDisplayed()
        onNodeWithText("Echo").performClick()
        assertEquals("acme/echo", selected)
    }

    @Test
    fun localTabExplainsWhenNothingIsInstalled() = runComposeUiTest {
        setContent { TcrTheme { ModelsTabs(ModelsPaneState(engineState = EngineState.READY), ModelsPaneActions()) } }
        onNodeWithText("Local").performClick()
        onNodeWithText("No models installed.", substring = true).assertIsDisplayed()
    }

    @Test
    fun localTabConfirmsBeforeDeleting() = runComposeUiTest {
        var deleted: String? = null
        setContent {
            TcrTheme {
                ModelsTabs(
                    ModelsPaneState(engineState = EngineState.READY, localModels = listOf(installed)),
                    ModelsPaneActions(onDeleteModel = { deleted = it }),
                )
            }
        }
        onNodeWithText("Local").performClick()
        onNodeWithContentDescription("Delete").performClick()
        assertEquals(null, deleted, "nothing is deleted before the dialog is confirmed")
        onNodeWithText("Delete Model").assertIsDisplayed()
        onNodeWithText("Delete").performClick()
        waitForIdle()
        onNodeWithText("Delete Model").assertDoesNotExist()
        assertEquals("acme/echo", deleted)
    }

    @Test
    fun providersTabSaysRemoteRoutingIsGated() = runComposeUiTest {
        setContent { TcrTheme { ModelsTabs(ModelsPaneState(engineState = EngineState.READY), ModelsPaneActions()) } }
        onNodeWithText("Providers").performClick()
        onNodeWithText("`remote` placement", substring = true).assertIsDisplayed()
    }

    private val detail = ModelDetail(
        id = "acme/echo", name = "Echo", providerName = "Hugging Face", licenseName = "apache-2.0",
        modelUrl = "https://huggingface.co/acme/echo", sizeMB = 2,
    )

    private fun detailState(installed: Boolean = false) = ModelDetailState(model = detail, isInstalled = installed)

    @Test
    fun detailShowsSizeAndTheDeclaredLicense() = runComposeUiTest {
        var opened: String? = null
        setContent {
            TcrTheme {
                ModelDetailContent(
                    state = detailState(), loadingState = ModelLoadingState(), engineState = EngineState.READY,
                    preferredContext = 2048, actions = ModelDetailActions(onOpenUrl = { opened = it }),
                )
            }
        }
        onNodeWithText("Size on disk: 2 MB").assertIsDisplayed()
        assertTrue(onAllNodesWithText("apache-2.0", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertEquals(null, opened)
    }

    @Test
    fun detailHidesLicenseAcceptanceByDefault() = runComposeUiTest {
        setContent {
            TcrTheme {
                ModelDetailContent(
                    state = detailState(), loadingState = ModelLoadingState(), engineState = EngineState.READY,
                    preferredContext = 2048, actions = ModelDetailActions(),
                )
            }
        }
        onAllNodesWithText("License accepted").assertCountEquals(0)
        onAllNodesWithText("Accept License").assertCountEquals(0)
    }

    @Test
    fun detailOffersLicenseAcceptanceUntilAccepted() = runComposeUiTest {
        var accepted = 0
        setContent {
            TcrTheme {
                ModelDetailContent(
                    state = detailState(), loadingState = ModelLoadingState(), engineState = EngineState.READY,
                    preferredContext = 2048, actions = ModelDetailActions(),
                    licenseAccepted = false, onAcceptLicense = { accepted++ },
                )
            }
        }
        onNodeWithText("License acceptance required before running this model.").assertIsDisplayed()
        onNodeWithText("Accept License").performClick()
        assertEquals(1, accepted)
    }

    @Test
    fun detailShowsAcceptedLicenseWithoutAButton() = runComposeUiTest {
        setContent {
            TcrTheme {
                ModelDetailContent(
                    state = detailState(), loadingState = ModelLoadingState(), engineState = EngineState.READY,
                    preferredContext = 2048, actions = ModelDetailActions(), licenseAccepted = true,
                )
            }
        }
        onNodeWithText("License accepted").assertIsDisplayed()
        onAllNodesWithText("Accept License").assertCountEquals(0)
    }

    @Test
    fun detailShowsErrorAndSpinnerStates() = runComposeUiTest {
        setContent {
            TcrTheme {
                ModelDetailContent(
                    state = ModelDetailState(error = "Model not found"), loadingState = ModelLoadingState(),
                    engineState = EngineState.READY, preferredContext = 2048, actions = ModelDetailActions(),
                )
            }
        }
        onNodeWithText("Model not found").assertIsDisplayed()
    }
}
