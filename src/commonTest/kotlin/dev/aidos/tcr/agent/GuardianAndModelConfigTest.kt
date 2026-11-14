package dev.aidos.tcr.agent

import dev.aidos.tcr.theme.TcrColors
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuardianAndModelConfigTest {

    @Test
    fun guardianConfigDefaultsAndCopySemantics() {
        val defaults = TcrGuardianConfigUiModel()
        assertEquals(TcrTaintApprovalPolicy.MANUAL, defaults.policy)
        assertEquals("laya-multilingual", defaults.decisionModelId)
        assertEquals("", defaults.guardianLlmModelId)
        assertEquals(emptyList(), defaults.availableDecisionModels)
        assertEquals(emptyList(), defaults.availableLlmModels)

        val updated = defaults.copy(
            policy = TcrTaintApprovalPolicy.GUARDIAN,
            decisionModelId = "laya-multilingual-v2",
            guardianLlmModelId = "qwen-2.5-3b-instruct",
            availableDecisionModels = listOf("laya-multilingual", "laya-multilingual-v2"),
            availableLlmModels = listOf("qwen-2.5-3b-instruct", "gemma-2-2b-it"),
        )

        assertEquals(TcrTaintApprovalPolicy.MANUAL, defaults.policy)
        assertEquals("laya-multilingual", defaults.decisionModelId)
        assertEquals(TcrTaintApprovalPolicy.GUARDIAN, updated.policy)
        assertEquals("laya-multilingual-v2", updated.decisionModelId)
        assertEquals("qwen-2.5-3b-instruct", updated.guardianLlmModelId)
        assertEquals(2, updated.availableDecisionModels.size)
        assertEquals(2, updated.availableLlmModels.size)
        assertNotEquals(defaults, updated)
    }

    @Test
    fun projectModelConfigDefaultsAndCopySemantics() {
        val defaults = TcrProjectModelConfigUiModel()
        assertEquals("", defaults.projectId)
        assertEquals("", defaults.projectName)
        assertEquals("", defaults.mainModelId)
        assertEquals("", defaults.defaultRunModelId)
        assertTrue(defaults.allowMainModelRunOverride)
        assertEquals(AgentLocality.ANY, defaults.enforcedLocality)
        assertEquals(TcrGuardianConfigUiModel(), defaults.guardianConfig)
        assertEquals(emptyList(), defaults.availableLlmModels)
        assertEquals(emptyList(), defaults.availableDecisionModels)

        val customized = defaults.copy(
            projectId = "proj-alpha",
            projectName = "Alpha Workspace",
            mainModelId = "llama-3.2-8b-instruct",
            defaultRunModelId = "qwen-2.5-3b-instruct",
            allowMainModelRunOverride = false,
            enforcedLocality = AgentLocality.LOCAL_ONLY,
            guardianConfig = TcrGuardianConfigUiModel(policy = TcrTaintApprovalPolicy.GUARDIAN),
            availableLlmModels = listOf("llama-3.2-8b-instruct", "qwen-2.5-3b-instruct"),
            availableDecisionModels = listOf("laya-multilingual"),
        )

        assertEquals(AgentLocality.ANY, defaults.enforcedLocality)
        assertTrue(defaults.allowMainModelRunOverride)
        assertEquals("proj-alpha", customized.projectId)
        assertEquals("Alpha Workspace", customized.projectName)
        assertEquals("llama-3.2-8b-instruct", customized.mainModelId)
        assertEquals("qwen-2.5-3b-instruct", customized.defaultRunModelId)
        assertFalse(customized.allowMainModelRunOverride)
        assertEquals(AgentLocality.LOCAL_ONLY, customized.enforcedLocality)
        assertEquals(TcrTaintApprovalPolicy.GUARDIAN, customized.guardianConfig.policy)
    }

    @Test
    fun runLaunchConfigDefaultsAndCopySemantics() {
        val defaults = TcrRunLaunchConfigUiModel()
        assertNull(defaults.guardianPolicyOverride)
        assertEquals("", defaults.runModelOverride)
        assertNull(defaults.localityOverride)

        val overridden = defaults.copy(
            guardianPolicyOverride = TcrTaintApprovalPolicy.GUARDIAN,
            runModelOverride = "gemma-2-2b-it",
            localityOverride = AgentLocality.TRUSTED_CLUSTER,
        )

        assertNull(defaults.guardianPolicyOverride)
        assertEquals(TcrTaintApprovalPolicy.GUARDIAN, overridden.guardianPolicyOverride)
        assertEquals("gemma-2-2b-it", overridden.runModelOverride)
        assertEquals(AgentLocality.TRUSTED_CLUSTER, overridden.localityOverride)
    }

    @Test
    fun localityAndPolicyEnumMappingsAndTcrColors() {
        assertEquals(
            listOf(TcrTaintApprovalPolicy.MANUAL, TcrTaintApprovalPolicy.GUARDIAN),
            TcrTaintApprovalPolicy.entries,
        )
        assertEquals(TcrTaintApprovalPolicy.MANUAL, TcrTaintApprovalPolicy.valueOf("MANUAL"))
        assertEquals(TcrTaintApprovalPolicy.GUARDIAN, TcrTaintApprovalPolicy.valueOf("GUARDIAN"))

        assertNull(tcrPolicyAccentColor(TcrTaintApprovalPolicy.MANUAL))
        assertEquals(TcrColors.AmberWarning, tcrPolicyAccentColor(TcrTaintApprovalPolicy.GUARDIAN))

        assertEquals(AgentLocality.LOCAL_ONLY, AgentLocality.fromWire("local_only"))
        assertEquals(AgentLocality.TRUSTED_CLUSTER, AgentLocality.fromWire("TRUSTED_CLUSTER"))
        assertEquals(AgentLocality.ANY, AgentLocality.fromWire("any"))
        assertEquals(AgentLocality.ANY, AgentLocality.fromWire("unknown"))

        assertEquals("LOCAL", tcrLocalityShortLabel(AgentLocality.LOCAL_ONLY))
        assertEquals("CLUSTER", tcrLocalityShortLabel(AgentLocality.TRUSTED_CLUSTER))
        assertEquals("REMOTE", tcrLocalityShortLabel(AgentLocality.ANY))

        assertEquals(TcrColors.EmeraldGreen, tcrLocalityColor(AgentLocality.LOCAL_ONLY))
        assertEquals(TcrColors.ElectricBlue, tcrLocalityColor(AgentLocality.TRUSTED_CLUSTER))
        assertEquals(TcrColors.VioletProtocol, tcrLocalityColor(AgentLocality.ANY))
    }

    @Test
    fun agentRunUiModelBackwardCompatibleConstructionAndNewFields() {
        // Minimal positional construction (simulating legacy callers)
        val minimal = AgentRunUiModel("run-001", "Dictator", "gemma-2-2b-it")
        assertEquals("run-001", minimal.runId)
        assertEquals("Dictator", minimal.ownerApp)
        assertEquals("gemma-2-2b-it", minimal.modelId)
        assertEquals(TcrTaintApprovalPolicy.MANUAL, minimal.guardianPolicy)
        assertFalse(minimal.modelChosenByMainModel)
        assertNull(minimal.enforcedLocality)

        // Legacy full positional/named fields without new extensions
        val legacyNamed = AgentRunUiModel(
            runId = "run-002",
            ownerApp = "Agentic Messenger",
            modelId = "llama-3.2-3b-instruct",
            placement = AgentPlacement.CLUSTER,
            locality = AgentLocality.TRUSTED_CLUSTER,
            status = AgentRunStatus.COMPLETED,
            role = AgentRoleUi.WORKER,
            parentSessionId = "sess-1",
            parentRunId = "run-001",
            workerBrief = "Check calendar",
            isParkedOnChild = false,
            awaitingChildRunId = null,
        )
        assertEquals(TcrTaintApprovalPolicy.MANUAL, legacyNamed.guardianPolicy)
        assertFalse(legacyNamed.modelChosenByMainModel)
        assertNull(legacyNamed.enforcedLocality)

        val withNewFields = legacyNamed.copy(
            guardianPolicy = TcrTaintApprovalPolicy.GUARDIAN,
            modelChosenByMainModel = true,
            enforcedLocality = AgentLocality.LOCAL_ONLY,
        )
        assertEquals(TcrTaintApprovalPolicy.GUARDIAN, withNewFields.guardianPolicy)
        assertTrue(withNewFields.modelChosenByMainModel)
        assertEquals(AgentLocality.LOCAL_ONLY, withNewFields.enforcedLocality)
    }

    @Test
    fun agentOverviewUiStateBackwardCompatibleConstructionAndNewFields() {
        val defaultState = AgentOverviewUiState()
        assertNull(defaultState.projectConfig)
        assertEquals(TcrRunLaunchConfigUiModel(), defaultState.runLaunchConfig)

        val legacyNamedState = AgentOverviewUiState(
            stats = AgentRuntimeOverviewStats(activeRuns = 1),
            runs = SampleAgentRuns.create(),
            selectedAppFilter = "Dictator Workspace",
            selectedStatusFilter = AgentFilterTab.ACTIVE,
            selectedPlacementFilter = AgentPlacement.LOCAL,
            selectedViewLayout = AgentViewLayout.FLAT,
            searchQuery = "fft",
            isEngineRunning = true,
            selectedRunId = "run-driver-001",
        )
        assertNull(legacyNamedState.projectConfig)
        assertEquals(TcrRunLaunchConfigUiModel(), legacyNamedState.runLaunchConfig)

        val configuredState = legacyNamedState.copy(
            projectConfig = TcrProjectModelConfigUiModel(
                projectId = "proj-1",
                projectName = "Main Project",
                mainModelId = "llama-3.2-3b",
                enforcedLocality = AgentLocality.TRUSTED_CLUSTER,
                guardianConfig = TcrGuardianConfigUiModel(policy = TcrTaintApprovalPolicy.GUARDIAN),
            ),
            runLaunchConfig = TcrRunLaunchConfigUiModel(
                guardianPolicyOverride = TcrTaintApprovalPolicy.MANUAL,
                runModelOverride = "qwen-2.5-1.5b",
                localityOverride = AgentLocality.LOCAL_ONLY,
            ),
        )
        assertEquals("proj-1", configuredState.projectConfig?.projectId)
        assertEquals(TcrTaintApprovalPolicy.GUARDIAN, configuredState.projectConfig?.guardianConfig?.policy)
        assertEquals(TcrTaintApprovalPolicy.MANUAL, configuredState.runLaunchConfig.guardianPolicyOverride)
        assertEquals("qwen-2.5-1.5b", configuredState.runLaunchConfig.runModelOverride)
        assertEquals(AgentLocality.LOCAL_ONLY, configuredState.runLaunchConfig.localityOverride)
    }

    @Test
    fun serializationRoundtripAndLegacyJsonDefaults() {
        val state = AgentOverviewUiState(
            runs = listOf(
                AgentRunUiModel(
                    runId = "run-guard-1",
                    ownerApp = "Dictator",
                    modelId = "qwen-2.5-3b",
                    guardianPolicy = TcrTaintApprovalPolicy.GUARDIAN,
                    modelChosenByMainModel = true,
                    enforcedLocality = AgentLocality.TRUSTED_CLUSTER,
                )
            ),
            projectConfig = TcrProjectModelConfigUiModel(
                projectId = "p-1",
                projectName = "Dictator Project",
                mainModelId = "llama-3.2-8b",
                defaultRunModelId = "qwen-2.5-3b",
                enforcedLocality = AgentLocality.LOCAL_ONLY,
                guardianConfig = TcrGuardianConfigUiModel(
                    policy = TcrTaintApprovalPolicy.GUARDIAN,
                    decisionModelId = "laya-multilingual",
                    guardianLlmModelId = "qwen-2.5-3b",
                ),
            ),
            runLaunchConfig = TcrRunLaunchConfigUiModel(
                guardianPolicyOverride = TcrTaintApprovalPolicy.MANUAL,
                runModelOverride = "gemma-2-2b-it",
                localityOverride = AgentLocality.ANY,
            ),
        )

        val encoded = Json.encodeToString(AgentOverviewUiState.serializer(), state)
        val decoded = Json.decodeFromString(AgentOverviewUiState.serializer(), encoded)
        assertEquals(state, decoded)

        val legacyRunJson = """{"runId":"legacy-1","ownerApp":"LegacyApp","modelId":"gemma-2-2b-it"}"""
        val decodedLegacyRun = Json.decodeFromString(AgentRunUiModel.serializer(), legacyRunJson)
        assertEquals(TcrTaintApprovalPolicy.MANUAL, decodedLegacyRun.guardianPolicy)
        assertFalse(decodedLegacyRun.modelChosenByMainModel)
        assertNull(decodedLegacyRun.enforcedLocality)
    }
}
