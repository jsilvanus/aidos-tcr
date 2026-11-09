package dev.aidos.tcr.agent

import kotlinx.serialization.Serializable

/**
 * Model execution placement according to RFC-0104.
 */
@Serializable
enum class AgentPlacement(val wire: String, val label: String) {
    LOCAL("local", "Local Device"),
    CLUSTER("cluster", "Trusted Cluster"),
    REMOTE("remote", "Remote Provider");

    companion object {
        fun fromWire(wire: String): AgentPlacement = entries.firstOrNull { it.wire.equals(wire, ignoreCase = true) } ?: REMOTE
    }
}

/**
 * Enforced Locality ceiling (RFC-0104).
 */
@Serializable
enum class AgentLocality(val wire: String, val label: String) {
    LOCAL_ONLY("local_only", "Local Only"),
    TRUSTED_CLUSTER("trusted_cluster", "Cluster Only"),
    ANY("any", "Any");

    companion object {
        fun fromWire(wire: String): AgentLocality = entries.firstOrNull { it.wire.equals(wire, ignoreCase = true) } ?: ANY
    }
}

/**
 * Execution status of an agent run task.
 */
@Serializable
enum class AgentRunStatus {
    RUNNING,
    TOOL_CALLS,
    COMPLETED,
    FAILED,
    CANCELLED;

    val isTerminal: Boolean get() = this == COMPLETED || this == FAILED || this == CANCELLED
}

/**
 * Outbound connection audit record (RFC-0042 Egress).
 */
@Serializable
data class AgentEgressRecordUi(
    val destinationHost: String,
    val method: String = "GET",
    val requestBytes: Long = 0L,
    val responseBytes: Long = 0L,
    val payloadHash: String = "",
    val durationMs: Long = 0L,
    val outcome: String = "ALLOWED",
    val timestamp: String = ""
)

/**
 * Aggregated network egress telemetry for a single agent turn or run.
 */
@Serializable
data class AgentEgressSummaryUi(
    val totalCalls: Int = 0,
    val totalBytesOut: Long = 0L,
    val totalBytesIn: Long = 0L,
    val records: List<AgentEgressRecordUi> = emptyList()
) {
    val isAirGapped: Boolean get() = totalCalls == 0
}

/**
 * Executed tool call details.
 */
@Serializable
data class ExecutedToolCallUi(
    val toolName: String,
    val argumentsJson: String,
    val resultText: String
)

/**
 * Pending tool call awaiting host/client response.
 */
@Serializable
data class PendingToolCallUi(
    val callId: String,
    val toolName: String,
    val argumentsJson: String
)

/**
 * Agent role according to RFC-0011 (Driver vs Worker).
 */
@Serializable
enum class AgentRoleUi(val wire: String, val label: String, val badgeEmoji: String) {
    DRIVER("DRIVER", "Driver", "👑"),
    WORKER("WORKER", "Worker", "🛠️");

    companion object {
        fun fromWire(wire: String?): AgentRoleUi =
            entries.firstOrNull { it.wire.equals(wire, ignoreCase = true) } ?: DRIVER
    }
}

/**
 * Single node in the execution graph DAG (RFC-0019, RFC-0011).
 */
@Serializable
data class TaskGraphNodeUi(
    val taskId: String,
    val ordinal: Int,
    val kind: String,
    val description: String,
    val state: String,
    val dependsOnTaskIds: List<String> = emptyList(),
    val toolName: String? = null,
    val awaitingRunId: String? = null,
) {
    val isTerminal: Boolean
        get() = state == "COMPLETED" || state == "FAILED" || state == "CANCELLED" || state == "SKIPPED"
    val isSkipped: Boolean get() = state == "SKIPPED"
    val isRunning: Boolean get() = state == "RUNNING"
    val isParked: Boolean get() = state == "AWAITING_INPUT" || state == "AWAITING_APPROVAL"
}

/**
 * Worker session spawn metadata for COMPOSITE tasks (RFC-0011).
 */
@Serializable
data class WorkerSpawnUi(
    val taskId: String,
    val workerSessionId: String? = null,
    val childRunId: String? = null,
    val brief: String,
    val budgetSplitWays: Int = 1,
    val delegatedCapabilities: List<String> = emptyList(),
)

/**
 * Budget and quota limits (RFC-0028).
 */
@Serializable
data class AgentBudgetUi(
    val modelCalls: Int? = null,
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val costUnits: Long? = null,
    val steps: Int? = null,
    val maxSteps: Int? = null,
    val splitWays: Int? = null,
)

/**
 * Worker Git commit outcome record (refs/aidos/workers/<workerSessionId>).
 */
@Serializable
data class WorkerOutcomeUi(
    val workerSessionId: String,
    val commitRef: String,
    val outcomeSummary: String,
    val committedAt: String? = null,
)

/**
 * UI representation of an Agent Run.
 */
@Serializable
data class AgentRunUiModel(
    val runId: String,
    val ownerApp: String,
    val modelId: String,
    val placement: AgentPlacement = AgentPlacement.LOCAL,
    val locality: AgentLocality = AgentLocality.LOCAL_ONLY,
    val status: AgentRunStatus = AgentRunStatus.RUNNING,
    val seq: Int = 1,
    val steps: Int = 0,
    val maxSteps: Int = 8,
    val promptPreview: String = "",
    val replyText: String = "",
    val errorMessage: String? = null,
    val pendingToolCall: PendingToolCallUi? = null,
    val executedTools: List<ExecutedToolCallUi> = emptyList(),
    val egressSummary: AgentEgressSummaryUi = AgentEgressSummaryUi(),
    val startedAt: Long = 0L,
    val finishedAt: Long = 0L,
    // RFC-0011 Driver/Worker Orchestration and RFC-0019 Execution DAG extensions:
    val role: AgentRoleUi = AgentRoleUi.DRIVER,
    val parentSessionId: String? = null,
    val parentRunId: String? = null,
    val workerBrief: String? = null,
    val tasks: List<TaskGraphNodeUi> = emptyList(),
    val workerSpawns: List<WorkerSpawnUi> = emptyList(),
    val budget: AgentBudgetUi? = null,
    val workerOutcome: WorkerOutcomeUi? = null,
    val isParkedOnChild: Boolean = false,
    val awaitingChildRunId: String? = null,
)

/**
 * Runtime overview telemetry & concurrency statistics.
 */
@Serializable
data class AgentRuntimeOverviewStats(
    val activeRuns: Int = 0,
    val maxConcurrentRuns: Int = 8,
    val waitingToolCalls: Int = 0,
    val completedRuns: Int = 0,
    val failedRuns: Int = 0,
    val totalRetainedRuns: Int = 0,
    val appsCount: Int = 0,
    val runsByApp: Map<String, Int> = emptyMap(),
    val totalEgressCalls: Int = 0
)

/**
 * Tab selection for filtering agent runs by execution status.
 */
enum class AgentFilterTab(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    AWAITING_TOOLS("Waiting on Tools"),
    COMPLETED("Completed"),
    FAILED("Failed / Cancelled")
}

/**
 * Display layout mode for the Agent Overview pane.
 */
enum class AgentViewLayout(val label: String) {
    HIERARCHY("🌳 Subagent Hierarchy"),
    FLAT("📋 Flat Feed")
}

/**
 * Filter option for execution placement (RFC-0104).
 */
enum class PlacementFilterOption(val label: String, val placement: AgentPlacement?) {
    ALL("All Placements", null),
    LOCAL("🟢 Local (Device)", AgentPlacement.LOCAL),
    CLUSTER("🔵 Cluster (Peer)", AgentPlacement.CLUSTER),
    REMOTE("🟣 Remote (Cloud)", AgentPlacement.REMOTE)
}

/**
 * UI State for the agent runtime overview pane.
 */
@Serializable
data class AgentOverviewUiState(
    val stats: AgentRuntimeOverviewStats = AgentRuntimeOverviewStats(),
    val runs: List<AgentRunUiModel> = emptyList(),
    val selectedAppFilter: String? = null,
    val selectedStatusFilter: AgentFilterTab = AgentFilterTab.ALL,
    val selectedPlacementFilter: AgentPlacement? = null,
    val selectedViewLayout: AgentViewLayout = AgentViewLayout.HIERARCHY,
    val searchQuery: String = "",
    val isEngineRunning: Boolean = true,
    val selectedRunId: String? = null,
)

/**
 * Predicate checking whether an agent run matches a search query across ID, app, prompt, brief, tools, or tasks.
 */
fun AgentRunUiModel.matchesSearch(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return runId.contains(q, ignoreCase = true) ||
        ownerApp.contains(q, ignoreCase = true) ||
        modelId.contains(q, ignoreCase = true) ||
        promptPreview.contains(q, ignoreCase = true) ||
        replyText.contains(q, ignoreCase = true) ||
        (workerBrief?.contains(q, ignoreCase = true) == true) ||
        executedTools.any { it.toolName.contains(q, ignoreCase = true) || it.argumentsJson.contains(q, ignoreCase = true) } ||
        tasks.any { it.description.contains(q, ignoreCase = true) || (it.toolName?.contains(q, ignoreCase = true) == true) }
}

/**
 * Human-readable execution duration formatter.
 */
fun formatDuration(startedAt: Long?, finishedAt: Long?): String {
    if (startedAt == null || startedAt == 0L) return "Unknown"
    val end = if (finishedAt != null && finishedAt > 0L) finishedAt else startedAt
    val millis = (end - startedAt).coerceAtLeast(0)
    val seconds = millis / 1000
    return when {
        seconds < 1 -> "${millis}ms"
        seconds < 60 -> "${seconds}s"
        else -> "${seconds / 60}m ${seconds % 60}s"
    }
}

