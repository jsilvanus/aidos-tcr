package dev.aidos.tcr.intent

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IntentComponentsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    @Test
    fun testIntentNodeUiModelSerialization() {
        val model = TcrIntentNodeUiModel(
            id = "node-101",
            projectId = "project-core",
            title = "Implement Blinded Egress Bridge",
            description = "RFC-0113 declassification gate UI",
            type = TcrIntentNodeType.ATOMIC_TASK,
            actorKind = TcrIntentActorKind.HYBRID,
            status = TcrDerivedStatus.NEEDS_REVIEW,
            dispatchPolicy = TcrEffectiveDispatchPolicy.MANUAL_START,
            granularity = TcrIntentGranularity.ATOMIC,
            targetProfile = TcrIntentTargetProfile.DESKTOP,
            minLocality = TcrIntentMinLocality.LOCAL_ONLY,
            estimatedSteps = 4,
            scopePaths = listOf("bridge/EgressGate.kt", "tcr/EgressGateCard.kt"),
            tags = listOf("rfc-0113", "security"),
            unmetDependencyIds = emptyList(),
            activeRunIds = listOf("run-xyz-99"),
            pendingPlatformChecks = true,
            checkKind = TcrIntentCheckKind.SHELL_COMMAND,
            checkSpec = "./gradlew check"
        )

        val serialized = json.encodeToString(model)
        val deserialized = json.decodeFromString<TcrIntentNodeUiModel>(serialized)

        assertEquals(model.id, deserialized.id)
        assertEquals(model.title, deserialized.title)
        assertEquals(TcrIntentActorKind.HYBRID, deserialized.actorKind)
        assertEquals(TcrDerivedStatus.NEEDS_REVIEW, deserialized.status)
        assertEquals(TcrIntentTargetProfile.DESKTOP, deserialized.targetProfile)
        assertEquals(2, deserialized.scopePaths.size)
        assertTrue(deserialized.pendingPlatformChecks)
    }

    @Test
    fun testEgressGateCardModelsAndStatuses() {
        val request = TcrOutboundMessageRequest(
            id = "msg-001",
            projectId = "personal-life",
            projectType = "personal",
            channel = "Matrix",
            recipient = "@alice:example.org",
            payload = "Meeting summary and action items from today's discussion.",
            reason = "User requested external matrix dispatch",
            taintLevel = "HIGH",
            runId = "run-life-42"
        )

        val parkedMessage = TcrParkedOutboundMessage(
            id = "parked-msg-001",
            request = request,
            status = TcrParkedMessageStatus.PARKED,
            createdAt = "2026-11-14T17:00:00Z"
        )

        val serialized = json.encodeToString(parkedMessage)
        val deserialized = json.decodeFromString<TcrParkedOutboundMessage>(serialized)

        assertEquals("parked-msg-001", deserialized.id)
        assertEquals(TcrParkedMessageStatus.PARKED, deserialized.status)
        assertEquals("personal", deserialized.request.projectType)
        assertEquals("Matrix", deserialized.request.channel)

        // Test status transition to APPROVED with HMAC token
        val approvedMessage = deserialized.copy(
            status = TcrParkedMessageStatus.APPROVED,
            hmacTokenId = "grant-token-hmac-sha256-abcdef123456",
            approvalDecisionAt = "2026-11-14T17:01:00Z"
        )
        assertEquals(TcrParkedMessageStatus.APPROVED, approvedMessage.status)
        assertNotNull(approvedMessage.hmacTokenId)
    }

    @Test
    fun testSplitVerificationUiModel() {
        val splitModel = TcrSplitVerificationUiModel(
            nodeId = "node-coding-202",
            projectId = "aidos-coding",
            title = "Add SQLite indexes for fast intent status resolution",
            runId = "mobile-run-11",
            originatingProfile = TcrIntentTargetProfile.MOBILE,
            targetProfile = TcrIntentTargetProfile.DESKTOP,
            checkKind = TcrIntentCheckKind.SHELL_COMMAND,
            checkSpec = "./gradlew.bat :storage:jvmTest",
            scopePaths = listOf("storage/src/commonMain/sqldelight/Intents.sq"),
            status = TcrDerivedStatus.NEEDS_REVIEW,
            outcome = TcrVerificationOutcome.PENDING,
            outputLog = null,
            isVerifying = false
        )

        val serialized = json.encodeToString(splitModel)
        val deserialized = json.decodeFromString<TcrSplitVerificationUiModel>(serialized)

        assertEquals("node-coding-202", deserialized.nodeId)
        assertEquals(TcrIntentTargetProfile.MOBILE, deserialized.originatingProfile)
        assertEquals(TcrIntentTargetProfile.DESKTOP, deserialized.targetProfile)
        assertEquals(TcrVerificationOutcome.PENDING, deserialized.outcome)
        assertFalse(deserialized.isVerifying)

        // Simulate desktop check running and passing
        val runningModel = deserialized.copy(isVerifying = true, outcome = TcrVerificationOutcome.RUNNING)
        assertTrue(runningModel.isVerifying)

        val passedModel = runningModel.copy(
            isVerifying = false,
            outcome = TcrVerificationOutcome.PASSED,
            outputLog = "BUILD SUCCESSFUL in 4s\n3 tests passed, 0 failures",
            status = TcrDerivedStatus.DONE
        )
        assertEquals(TcrVerificationOutcome.PASSED, passedModel.outcome)
        assertEquals(TcrDerivedStatus.DONE, passedModel.status)
        assertNotNull(passedModel.outputLog)
    }
}
