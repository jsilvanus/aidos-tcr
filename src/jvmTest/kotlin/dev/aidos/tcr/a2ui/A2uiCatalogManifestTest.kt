package dev.aidos.tcr.a2ui

import dev.aidos.tcr.manifest.ManifestIo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** The A2UI catalog may only name TCR composables that the registry manifest lists, at the same contract. */
class A2uiCatalogManifestTest {

    @Test
    fun everyCatalogTcrIdIsInTheManifestAtTheSameContract() {
        val byId = requireNotNull(ManifestIo.read()).components.associateBy { it.id }
        for (spec in A2uiCatalog.components.values) {
            val id = spec.tcrId ?: continue
            val entry = assertNotNull(byId[id], "catalog ${spec.type} names $id, which is not in tcr-manifest.json")
            assertEquals(entry.contract, spec.contract, "contract mismatch for ${spec.type}")
        }
    }

    @Test
    fun layoutOnlyTypesHaveNoTcrAtom() {
        for (type in listOf("Row", "Column", "ActionBar")) {
            assertNull(A2uiCatalog.components.getValue(type).tcrId, type)
        }
    }
}
