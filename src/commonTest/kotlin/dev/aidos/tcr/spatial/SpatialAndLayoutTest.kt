package dev.aidos.tcr.spatial

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.aidos.tcr.layout.AdaptiveLayoutState
import dev.aidos.tcr.layout.FormFactor
import dev.aidos.tcr.layout.ScaffoldNavigationType
import dev.aidos.tcr.layout.computeFormFactor
import dev.aidos.tcr.layout.navigationTypeForFormFactor
import dev.aidos.tcr.overlay.KeyAction
import dev.aidos.tcr.overlay.KeyboardShortcutRegistry
import dev.aidos.tcr.spatial.canvas.ForceDirectedLayout
import dev.aidos.tcr.spatial.canvas.LodLevel
import dev.aidos.tcr.spatial.canvas.SemanticZoomManager
import dev.aidos.tcr.spatial.canvas.ViewportTransform
import dev.aidos.tcr.spatial.edge.EdgeCollisionRouter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpatialAndLayoutTest {
    private val box = Rect(0f, 0f, 100f, 100f)

    @Test
    fun edgeIsClippedToTheNodeBoundary() {
        val (s, e) = EdgeCollisionRouter.routeAroundObstacles(Offset(50f, 50f), Offset(400f, 50f), listOf(box))
        assertEquals(Offset(100f, 50f), s)
        assertEquals(Offset(400f, 50f), e)
        // Both ends inside one box: never collapsed.
        val inner = EdgeCollisionRouter.routeAroundObstacles(Offset(10f, 10f), Offset(90f, 90f), listOf(box))
        assertEquals(Offset(10f, 10f) to Offset(90f, 90f), inner)
        // No obstacles or a zero-length edge: unchanged.
        val p = Offset(3f, 3f)
        assertEquals(p to p, EdgeCollisionRouter.routeAroundObstacles(p, p, listOf(box)))
    }

    @Test
    fun zoomBreakpointsPickTheLevelOfDetail() {
        assertEquals(LodLevel.MACRO_GALAXY, SemanticZoomManager.computeLod(0.34f))
        assertEquals(LodLevel.MESO_CLUSTER, SemanticZoomManager.computeLod(0.35f))
        assertEquals(LodLevel.MESO_CLUSTER, SemanticZoomManager.computeLod(0.74f))
        assertEquals(LodLevel.MICRO_BLUEPRINT, SemanticZoomManager.computeLod(0.75f))
    }

    @Test
    fun viewportRoundTripsAndCulls() {
        val vp = ViewportTransform(panX = 30f, panY = -10f, zoom = 2f)
        val s = vp.toScreenCoordinates(100f, 50f)
        val w = vp.toWorldCoordinates(s.x, s.y)
        assertEquals(100f, w.x, 0.001f)
        assertEquals(50f, w.y, 0.001f)
        assertTrue(ViewportTransform().isPointInViewport(-90f, 0f, 800f, 600f))
        assertFalse(ViewportTransform().isPointInViewport(-900f, 0f, 800f, 600f))
    }

    @Test
    fun layoutPlacesEveryNodeOnceAndHandlesEmpty() {
        assertTrue(ForceDirectedLayout.layoutDag(emptyList()).isEmpty())
        val ids = (0 until 40).map { "n$it" }
        val pos = ForceDirectedLayout.layoutDag(ids)
        assertEquals(ids.toSet(), pos.keys)
        assertEquals(40, pos.values.toSet().size, "no two nodes share a position")
    }

    @Test
    fun formFactorBreakpointsChooseNavigation() {
        assertEquals(FormFactor.HANDHELD_COMPACT, computeFormFactor(599f))
        assertEquals(FormFactor.HANDHELD_EXPANDED, computeFormFactor(600f))
        assertEquals(FormFactor.HANDHELD_EXPANDED, computeFormFactor(839f))
        assertEquals(FormFactor.DESKTOP_DOCKED, computeFormFactor(840f))
        assertEquals(ScaffoldNavigationType.BOTTOM_NAVIGATION, navigationTypeForFormFactor(FormFactor.HANDHELD_COMPACT))
        assertEquals(ScaffoldNavigationType.NAVIGATION_RAIL, navigationTypeForFormFactor(FormFactor.HANDHELD_EXPANDED))
        assertEquals(ScaffoldNavigationType.PERMANENT_DRAWER, navigationTypeForFormFactor(FormFactor.DESKTOP_DOCKED))
        val st = AdaptiveLayoutState()
        assertTrue(st.isHandheldCompact)
        st.updateDimensions(900f, 700f)
        assertTrue(st.isDesktopDocked)
        st.updateDimensions(700f, 700f)
        assertTrue(st.isHandheldExpanded)
    }

    @Test
    fun keyboardShortcutsResolve() {
        assertEquals(KeyAction.OPEN_COMMAND_PALETTE, KeyboardShortcutRegistry.resolveKeyAction("K", true))
        assertEquals(KeyAction.NAVIGATE_UP, KeyboardShortcutRegistry.resolveKeyAction("k", false))
        assertEquals(KeyAction.OPEN_ACTION_INBOX, KeyboardShortcutRegistry.resolveKeyAction("i", true))
        assertEquals(KeyAction.NAVIGATE_DOWN, KeyboardShortcutRegistry.resolveKeyAction("j", false))
        assertEquals(KeyAction.TOGGLE_COMPLETE, KeyboardShortcutRegistry.resolveKeyAction("Space", false))
        assertEquals(KeyAction.ESCAPE, KeyboardShortcutRegistry.resolveKeyAction("escape", false))
        assertNull(KeyboardShortcutRegistry.resolveKeyAction("x", false))
    }
}
