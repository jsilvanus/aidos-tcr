package dev.aidos.tcr.agent

/**
 * Shared demonstration and sample data suite for [AgentsPane] and Agent TCR components.
 * Showcases RFC-0011 Driver/Worker Topology, DAG dependencies, budget split meters,
 * RFC-0104 placement (Local, Cluster, Remote), and RFC-0042 network egress records.
 */
object SampleAgentRuns {

    fun create(): List<AgentRunUiModel> {
        val now = 1731174000000L // Deterministic timestamp base

        val driverRun = AgentRunUiModel(
            runId = "run-driver-001",
            ownerApp = "Dictator Workspace",
            modelId = "gemma-2-2b-it",
            placement = AgentPlacement.LOCAL,
            locality = AgentLocality.LOCAL_ONLY,
            status = AgentRunStatus.TOOL_CALLS,
            role = AgentRoleUi.DRIVER,
            seq = 4,
            steps = 4,
            maxSteps = 12,
            promptPreview = "Refactor audio pipeline and spawn worker to process audio frequency buffers",
            replyText = "",
            isParkedOnChild = true,
            awaitingChildRunId = "run-worker-002",
            tasks = listOf(
                TaskGraphNodeUi("t1", 1, "TOOL_CALL", "Inspect audio input device descriptors", "COMPLETED"),
                TaskGraphNodeUi("t2", 2, "COMPOSITE", "Spawn worker: analyze FFT frequency buffers", "AWAITING_INPUT", listOf("t1"), awaitingRunId = "run-worker-002"),
                TaskGraphNodeUi("t3", 3, "TOOL_CALL", "Merge audio transform buffers", "PENDING", listOf("t2")),
                TaskGraphNodeUi("t4", 4, "MODEL_CALL", "Draft voice recognition response", "PENDING", listOf("t3")),
            ),
            workerSpawns = listOf(
                WorkerSpawnUi(
                    taskId = "t2",
                    workerSessionId = "sess-worker-002",
                    childRunId = "run-worker-002",
                    brief = "Analyze FFT frequency buffers",
                    budgetSplitWays = 2,
                    delegatedCapabilities = listOf("STT_QUERY", "FS_READ")
                )
            ),
            budget = AgentBudgetUi(
                modelCalls = 8,
                inputTokens = 8000L,
                outputTokens = 4000L,
                steps = 12,
                maxSteps = 12,
                splitWays = 2
            ),
            startedAt = now - 120_000,
        )

        val workerRun = AgentRunUiModel(
            runId = "run-worker-002",
            ownerApp = "Dictator Workspace",
            modelId = "gemma-2-2b-it",
            placement = AgentPlacement.LOCAL,
            locality = AgentLocality.LOCAL_ONLY,
            status = AgentRunStatus.RUNNING,
            role = AgentRoleUi.WORKER,
            parentSessionId = "sess-worker-002",
            parentRunId = "run-driver-001",
            workerBrief = "Analyze FFT frequency buffers and compute spectral centroid",
            seq = 2,
            steps = 2,
            maxSteps = 6,
            promptPreview = "Subagent worker session for audio spectral analysis",
            replyText = "",
            tasks = listOf(
                TaskGraphNodeUi("w1", 1, "FS_READ", "Load sample PCM audio chunk", "COMPLETED"),
                TaskGraphNodeUi("w2", 2, "CALCULATION", "Compute FFT window transform", "RUNNING", listOf("w1")),
            ),
            budget = AgentBudgetUi(
                modelCalls = 4,
                inputTokens = 4000L,
                outputTokens = 2000L,
                steps = 6,
                maxSteps = 6,
                splitWays = 2
            ),
            workerOutcome = WorkerOutcomeUi(
                workerSessionId = "sess-worker-002",
                commitRef = "refs/aidos/workers/sess-worker-002",
                outcomeSummary = "FFT calculation in-flight; window size: 1024 bins",
                committedAt = "2026-11-09T17:15:00Z"
            ),
            startedAt = now - 60_000,
        )

        val clusterRun = AgentRunUiModel(
            runId = "run-peer-cluster-003",
            ownerApp = "Agentic Messenger",
            modelId = "llama-3.2-3b-instruct",
            placement = AgentPlacement.CLUSTER,
            locality = AgentLocality.TRUSTED_CLUSTER,
            status = AgentRunStatus.COMPLETED,
            role = AgentRoleUi.DRIVER,
            seq = 6,
            steps = 6,
            maxSteps = 6,
            promptPreview = "Negotiate calendar appointment with John's agent over local peer mesh",
            replyText = "Appointment successfully scheduled for Tuesday 14:00. UAE disclosure approved.",
            tasks = listOf(
                TaskGraphNodeUi("c1", 1, "PEER_DISCOVERY", "Discover trusted peer node via AIDIN", "COMPLETED"),
                TaskGraphNodeUi("c2", 2, "ENVELOPE_EXCHANGE", "Exchange UAE calendar query envelope", "COMPLETED", listOf("c1")),
                TaskGraphNodeUi("c3", 3, "DISCLOSURE_EVAL", "Evaluate progressive disclosure policy", "COMPLETED", listOf("c2")),
            ),
            egressSummary = AgentEgressSummaryUi(
                totalCalls = 1,
                totalBytesOut = 2450L,
                totalBytesIn = 1820L,
                records = listOf(
                    AgentEgressRecordUi(
                        destinationHost = "aidin-peer://desktop-studio.local:18080",
                        method = "POST",
                        requestBytes = 2450L,
                        responseBytes = 1820L,
                        payloadHash = "sha256-d7a8f9...",
                        durationMs = 45L,
                        outcome = "ALLOWED",
                        timestamp = "2026-11-09T17:10:00Z"
                    )
                )
            ),
            startedAt = now - 300_000,
            finishedAt = now - 280_000,
        )

        val remoteFailedRun = AgentRunUiModel(
            runId = "run-remote-004",
            ownerApp = "Aidos Research",
            modelId = "gemini-1.5-flash",
            placement = AgentPlacement.REMOTE,
            locality = AgentLocality.ANY,
            status = AgentRunStatus.FAILED,
            role = AgentRoleUi.DRIVER,
            seq = 3,
            steps = 3,
            maxSteps = 8,
            promptPreview = "Fetch external benchmark datasets from huggingface.co",
            replyText = "",
            errorMessage = "HTTP 403 Forbidden: External egress blocked by strict firewall policy",
            egressSummary = AgentEgressSummaryUi(
                totalCalls = 1,
                totalBytesOut = 512L,
                totalBytesIn = 0L,
                records = listOf(
                    AgentEgressRecordUi(
                        destinationHost = "huggingface.co",
                        method = "GET",
                        requestBytes = 512L,
                        responseBytes = 0L,
                        payloadHash = "sha256-e3b0c4...",
                        durationMs = 120L,
                        outcome = "DENIED_POLICY",
                        timestamp = "2026-11-09T16:50:00Z"
                    )
                )
            ),
            startedAt = now - 600_000,
            finishedAt = now - 599_000,
        )

        return listOf(driverRun, workerRun, clusterRun, remoteFailedRun)
    }

    val sampleStats: AgentRuntimeOverviewStats = AgentRuntimeOverviewStats(
        activeRuns = 2,
        maxConcurrentRuns = 8,
        waitingToolCalls = 1,
        completedRuns = 14,
        failedRuns = 1,
        totalRetainedRuns = 17,
        appsCount = 3,
        runsByApp = mapOf(
            "Dictator Workspace" to 9,
            "Agentic Messenger" to 5,
            "Aidos Research" to 3
        ),
        totalEgressCalls = 4
    )
}
