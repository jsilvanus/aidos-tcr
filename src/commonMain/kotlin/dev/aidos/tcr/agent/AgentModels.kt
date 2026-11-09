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
    val finishedAt: Long = 0L
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
