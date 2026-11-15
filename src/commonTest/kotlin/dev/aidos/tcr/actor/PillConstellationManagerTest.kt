package dev.aidos.tcr.actor

import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PillConstellationManagerTest {
    private fun manager() = PillConstellationManager(TestScope())

    @Test
    fun defaultsAreRedBlueGreenWithGivenNames() {
        val m = PillConstellationManager(TestScope(), "A", "B", "C")
        assertEquals("A", m.orchestratorConfig.value.displayName)
        assertEquals(ActorPillConfig.DEFAULT_RED_ORCHESTRATOR, m.orchestratorConfig.value.colorArgb)
        assertEquals("B", m.editorConfig.value.displayName)
        assertEquals(ActorPillConfig.DEFAULT_BLUE_EDITOR, m.editorConfig.value.colorArgb)
        assertEquals("C", m.aiConfig.value.displayName)
        assertEquals(ActorPillConfig.DEFAULT_GREEN_AI, m.aiConfig.value.colorArgb)
    }

    @Test
    fun clicksToggleTheHudAndTranscriptDrawer() {
        val m = manager()
        m.onPillSingleClick("x")
        assertTrue(m.isHappeningsHudVisible.value)
        m.onPillSingleClick("x")
        assertFalse(m.isHappeningsHudVisible.value)
        m.onPillDoubleClick("x")
        assertTrue(m.isTranscriptDrawerOpen.value)
        m.closeTranscriptDrawer()
        assertFalse(m.isTranscriptDrawerOpen.value)
        m.onPillSingleClick("x"); m.dismissHappeningsHud()
        assertFalse(m.isHappeningsHudVisible.value)
    }

    @Test
    fun renameAndRecolourTargetOnlyTheNamedPill() {
        val m = manager()
        m.renamePill(m.aiConfig.value.id, "Muse")
        m.setPillColor(m.editorConfig.value.id, 0xFF000001L)
        assertEquals("Muse", m.aiConfig.value.displayName)
        assertEquals(0xFF000001L, m.editorConfig.value.colorArgb)
        assertEquals("Sanelu", m.orchestratorConfig.value.displayName)
        // Unknown ids change nothing and do not create pills.
        m.renamePill("ghost", "Boo"); m.setPillColor("ghost", 1L)
        assertTrue(m.customPills.value.isEmpty())
    }

    @Test
    fun happeningsKeepNewestFirstAndCapAtThirty() {
        val m = manager()
        repeat(35) { m.recordHappening("u$it", ActorRole.GENERATIVE_AI, "A", "d$it") }
        assertEquals(30, m.happenings.value.size)
        assertEquals("u34", m.happenings.value.first().recognizedUtterance)
        assertEquals(m.aiConfig.value.colorArgb, m.happenings.value.first().actorColorArgb)
        m.recordHappening("x", ActorRole.DETERMINISTIC_EDITOR, "A", "d", actorColorArgb = 7L)
        assertEquals(7L, m.happenings.value.first().actorColorArgb)
    }

    @Test
    fun transcriptAppendsInOrder() {
        val m = manager()
        m.appendTranscript("user", "hei", 0.9f, false)
        m.appendTranscript("user", "hei maailma", 0.95f, true)
        assertEquals(listOf("hei", "hei maailma"), m.transcript.value.map { it.text })
        assertTrue(m.transcript.value.last().isFinal)
    }

    @Test
    fun spawningAWorkerRegistersItAndLogsTheProjection() {
        val m = manager()
        val custom = ActorPillConfig("w1", ActorRole.CUSTOM, "Worker", 0xFF123456L, GlyphType.SPARK)
        val ev = m.spawnWorkerPill(custom, SpatialPoint(300f, 40f), PillProjectionStyle.MITOSIS_EJECT)
        assertEquals(SpatialPoint(120f, 120f), ev.sourcePoint) // from the orchestrator
        assertEquals(custom, m.customPills.value["w1"])
        assertEquals(ev, m.activeProjection.value)
        assertEquals("PROJECT_PILL", m.happenings.value.first().actionName)
        m.completeProjection("other")
        assertNotNull(m.activeProjection.value)
        m.completeProjection(ev.id)
        assertNull(m.activeProjection.value)
    }

    @Test
    fun editorRoleReplacesTheEditorConfig() {
        val m = manager()
        val ed = ActorPillConfig.createDefaultEditor(id = "ed2", name = "Second")
        m.spawnWorkerPill(ed, SpatialPoint(1f, 1f))
        assertEquals("ed2", m.editorConfig.value.id)
        assertTrue(m.customPills.value.isEmpty())
    }

    @Test
    fun targetConeAndAutonomousMovement() {
        val m = manager()
        m.projectTargetCone(SpatialPoint(0f, 0f), TextRangeBounds(2, 9), SpatialRect(0f, 0f, 10f, 4f), "word")
        val cone = m.activeTargetCone.value
        assertNotNull(cone)
        assertTrue(cone.isPulsing)
        m.clearTargetCone()
        assertNull(m.activeTargetCone.value)
        m.movePillAutonomously(m.orchestratorConfig.value.id, SpatialPoint(0f, 0f), SpatialPoint(5f, 5f))
        assertTrue(m.orchestratorStatus.value is ActorPillStatus.AutonomousTransit)
        m.movePillAutonomously("unknown", SpatialPoint(0f, 0f), SpatialPoint(1f, 1f))
        m.updateOrchestratorPosition(9f, 8f)
        assertEquals(SpatialPoint(9f, 8f), m.orchestratorPosition.value)
    }

    @Test
    fun modelHelpersComputeGeometry() {
        assertEquals(0, TextRangeBounds(9, 2).length)
        val r = SpatialRect(10f, 20f, 30f, 60f)
        assertEquals(20f, r.width); assertEquals(40f, r.height)
        assertEquals(20f, r.centerX); assertEquals(40f, r.centerY)
    }
}
