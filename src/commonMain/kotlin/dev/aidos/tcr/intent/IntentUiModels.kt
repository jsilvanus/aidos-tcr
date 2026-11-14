package dev.aidos.tcr.intent

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Structural type of an Intent node within the Intent Graph hierarchy.
 */
@Serializable
enum class TcrIntentNodeType {
    GROUP,
    GOAL,
    SUB_GOAL,
    ATOMIC_TASK,
    CONSTRAINT,
    ACCEPTANCE_CRITERION,
}

/**
 * Actor ownership kind for an Intent node (RFC-0110).
 * - [HUMAN]: User executed, never auto-dispatched to an agent.
 * - [AGENT]: AI session Run executed.
 * - [HYBRID]: Agent prepares/drafts, human reviews and completes.
 */
@Serializable
enum class TcrIntentActorKind {
    HUMAN,
    AGENT,
    HYBRID,
}

/**
 * Derived execution status of an Intent node.
 * Invariant: Status is purely computed, never set directly by agents.
 */
@Serializable
enum class TcrDerivedStatus {
    NOT_STARTED,
    IN_PROGRESS,
    BLOCKED,
    NEEDS_REVIEW,
    DONE,
    STALE,
    CONFLICTED,
}

/**
 * Effective scheduling policy for an Intent node.
 */
@Serializable
enum class TcrEffectiveDispatchPolicy {
    MANUAL_START,
    AUTO_WHEN_READY,
}

/**
 * Sizing granularity of an Intent node for planner decomposition.
 */
@Serializable
enum class TcrIntentGranularity {
    GROUP,
    GOAL,
    SUB_GOAL,
    ATOMIC,
}

/**
 * Target device/host platform profile for dispatch (RFC-0110).
 */
@Serializable
enum class TcrIntentTargetProfile {
    MOBILE,
    DESKTOP,
    HEADLESS_SERVER,
    ANY,
}

/**
 * Minimum network/compute locality constraint for task execution (RFC-0042).
 */
@Serializable
enum class TcrIntentMinLocality {
    LOCAL_ONLY,
    TRUSTED_CLUSTER,
    ANY,
}

/**
 * Kind of verification check required for task acceptance.
 */
@Serializable
enum class TcrIntentCheckKind {
    NONE,
    FILE_EXISTS,
    CONTAINS_PATTERN,
    DIFF_NON_EMPTY,
    SHELL_COMMAND,
}

/**
 * Presentation model for an Intent node across Outliner, Kanban, and Pipeline views.
 */
@Immutable
@Serializable
data class TcrIntentNodeUiModel(
    val id: String,
    val projectId: String,
    val title: String,
    val description: String? = null,
    val type: TcrIntentNodeType = TcrIntentNodeType.ATOMIC_TASK,
    val actorKind: TcrIntentActorKind = TcrIntentActorKind.AGENT,
    val status: TcrDerivedStatus = TcrDerivedStatus.NOT_STARTED,
    val dispatchPolicy: TcrEffectiveDispatchPolicy = TcrEffectiveDispatchPolicy.MANUAL_START,
    val granularity: TcrIntentGranularity = TcrIntentGranularity.ATOMIC,
    val targetProfile: TcrIntentTargetProfile? = null,
    val minLocality: TcrIntentMinLocality? = null,
    val estimatedSteps: Int? = null,
    val scopePaths: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val unmetDependencyIds: List<String> = emptyList(),
    val activeRunIds: List<String> = emptyList(),
    val pendingPlatformChecks: Boolean = false,
    val checkKind: TcrIntentCheckKind? = null,
    val checkSpec: String? = null,
    val dueAt: String? = null,
)

/**
 * Status of a parked outbound message in the Egress Gate queue (RFC-0113 §4.3).
 */
@Serializable
enum class TcrParkedMessageStatus {
    PARKED,
    APPROVED,
    REJECTED,
    TRANSMITTED,
}

/**
 * Proposed outbound network message originating from an agent session (RFC-0113).
 */
@Immutable
@Serializable
data class TcrOutboundMessageRequest(
    val id: String,
    val projectId: String,
    val projectType: String = "personal", // e.g. "personal" / "LIFE" / "coding"
    val channel: String, // e.g. "SMS", "Matrix", "Email", "HTTP"
    val recipient: String, // e.g. "Alice (+358 40 123 4567)"
    val payload: String, // Proposed message body
    val reason: String? = null,
    val taintLevel: String = "HIGH",
    val runId: String? = null,
)

/**
 * An outbound message parked for explicit human cryptographic declassification approval (RFC-0113 §4.3).
 */
@Immutable
@Serializable
data class TcrParkedOutboundMessage(
    val id: String,
    val request: TcrOutboundMessageRequest,
    val status: TcrParkedMessageStatus = TcrParkedMessageStatus.PARKED,
    val createdAt: String,
    val approvalDecisionAt: String? = null,
    val approverUserId: String? = null,
    val rejectionReason: String? = null,
    val hmacTokenId: String? = null,
)

/**
 * Execution outcome of a verification test check.
 */
@Serializable
enum class TcrVerificationOutcome {
    PENDING,
    RUNNING,
    PASSED,
    FAILED,
}

/**
 * UI model for a Split Verification gate review card (RFC-0110 §Coding Management).
 * Enables Desktop hosts to review and execute platform-dependent checks on work produced by Mobile agents.
 */
@Immutable
@Serializable
data class TcrSplitVerificationUiModel(
    val nodeId: String,
    val projectId: String,
    val title: String,
    val runId: String? = null,
    val originatingProfile: TcrIntentTargetProfile = TcrIntentTargetProfile.MOBILE,
    val targetProfile: TcrIntentTargetProfile = TcrIntentTargetProfile.DESKTOP,
    val checkKind: TcrIntentCheckKind = TcrIntentCheckKind.SHELL_COMMAND,
    val checkSpec: String = "",
    val scopePaths: List<String> = emptyList(),
    val status: TcrDerivedStatus = TcrDerivedStatus.NEEDS_REVIEW,
    val outcome: TcrVerificationOutcome = TcrVerificationOutcome.PENDING,
    val outputLog: String? = null,
    val isVerifying: Boolean = false,
)
