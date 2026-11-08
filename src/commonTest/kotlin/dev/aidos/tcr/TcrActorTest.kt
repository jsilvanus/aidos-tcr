package dev.aidos.tcr

import dev.aidos.tcr.actor.ActorPillStatus
import dev.aidos.tcr.actor.ActorSelectionHolder
import dev.aidos.tcr.actor.CadenceEngine
import dev.aidos.tcr.actor.GlyphType
import dev.aidos.tcr.actor.OperationSeverity
import dev.aidos.tcr.actor.TextRangeBounds
import dev.aidos.tcr.generative.GenerativeUiRegistry
import dev.aidos.tcr.model.AssertionLevel
import dev.aidos.tcr.model.EndpointIdentifier
import dev.aidos.tcr.model.EndpointType
import dev.aidos.tcr.model.EnvelopePayload
import dev.aidos.tcr.model.InformationBoundaryPolicy
import dev.aidos.tcr.model.MeshProvenanceType
import dev.aidos.tcr.model.PrivacyLevel
import dev.aidos.tcr.model.ProvenanceRecord
import dev.aidos.tcr.model.RoutingHeader
import dev.aidos.tcr.model.TimelineItemUiModel
import dev.aidos.tcr.model.TypedAnnotation
import dev.aidos.tcr.model.UniversalEnvelope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TcrActorTest {

    @Test
    fun testActorSelectionHolderCaptureAndRelease() {
        val holder = ActorSelectionHolder()
        assertFalse(holder.isHolding)

        val docText = "Aidos TCR unified component architecture."
        val state = holder.holdSelection(docText, 0, 5)

        assertTrue(holder.isHolding)
        assertNotNull(holder.holdingState.value)
        assertEquals(TextRangeBounds(0, 5), state.selectedRange)
        assertEquals(1, state.wordCount)

        holder.clearSelection()
        assertFalse(holder.isHolding)
    }

    @Test
    fun testCadenceEnginePauseAndResume() = runTest {
        val engine = CadenceEngine(this, defaultTickIntervalMs = 10L)
        assertTrue(engine.currentStatus.value is ActorPillStatus.WaitingAmbient)

        var committed = false
        val job = engine.stageAndExecute(
            targetRange = TextRangeBounds(0, 5),
            severity = OperationSeverity.DESTRUCTIVE, // 900ms
            description = "Deleting phrase",
            glyph = GlyphType.DELETE,
            onCommit = { committed = true }
        )

        runCurrent()
        advanceTimeBy(50L)
        val staging = engine.currentStatus.value
        assertTrue(staging is ActorPillStatus.CadenceStaging)
        assertFalse(staging.isPaused)

        engine.pauseStaging()
        val paused = engine.currentStatus.value
        assertTrue(paused is ActorPillStatus.CadenceStaging)
        assertTrue(paused.isPaused)

        engine.resumeStaging()
        val resumed = engine.currentStatus.value
        assertTrue(resumed is ActorPillStatus.CadenceStaging)
        assertFalse(resumed.isPaused)

        engine.abortCurrentAction()
        assertTrue(engine.currentStatus.value is ActorPillStatus.WaitingAmbient)
        assertFalse(committed)
        job.cancel()
    }

    @Test
    fun testGenerativeUiRegistryMapping() {
        val envelope = UniversalEnvelope(
            envelopeId = "env-123",
            conversationId = "conv-456",
            epoch = 1L,
            timestamp = 1000L,
            routing = RoutingHeader(
                sender = EndpointIdentifier(
                    principalId = "did:aidos:test",
                    endpointType = EndpointType.HUMAN_DIRECT,
                    deviceKeyId = "dev-1"
                ),
                recipients = emptyList()
            ),
            provenance = ProvenanceRecord(
                assertionLevel = AssertionLevel.ORIGINAL_HUMAN,
                signerKeyId = "key-abc-123456",
                signatureHex = "00"
            ),
            policy = InformationBoundaryPolicy(
                privacyLevel = PrivacyLevel.AGENT_ACCESSIBLE
            ),
            payload = EnvelopePayload(
                naturalLanguage = "Hello world",
                structuredAnnotations = listOf(
                    TypedAnnotation("status", 1.0, mapOf("cue" to "Approved"))
                )
            )
        )

        val item = GenerativeUiRegistry.mapEnvelopeToTimelineItem(envelope)
        assertTrue(item is TimelineItemUiModel.Message)
        assertEquals("env-123", item.id)
        assertEquals("Hello world", item.naturalText)
        assertEquals(MeshProvenanceType.HUMAN_DIRECT, item.provenanceType)
        assertEquals(listOf("Approved"), item.cueWords)
    }
}
