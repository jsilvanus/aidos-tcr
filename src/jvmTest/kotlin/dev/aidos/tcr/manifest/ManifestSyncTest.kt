package dev.aidos.tcr.manifest

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Validates `manifest/tcr-manifest.json` against the sources.
 *
 * To accept an intentional change (new/changed/removed component), run
 * `./gradlew jvmTest --tests '*ManifestSyncTest*' -Dtcr.updateManifest=true`
 * and review the resulting diff: it bumps each changed component's `contract` version by the
 * kind of change (see [ManifestScanner.classify]) and keeps each component's `status`.
 */
class ManifestSyncTest {

    private val scanned = ManifestIo.scanSources()

    @Test fun manifestMatchesSources() {
        val old = ManifestIo.read()
        val derived = ManifestIo.derive(scanned, old)
        if (System.getProperty("tcr.updateManifest") == "true") {
            ManifestIo.write(derived)
            return
        }
        assertNotNull(old, "manifest/tcr-manifest.json is missing; run with -Dtcr.updateManifest=true")
        if (old != derived) {
            val oldById = old.components.associateBy { it.id }
            val newById = derived.components.associateBy { it.id }
            val msg = buildList {
                (newById.keys - oldById.keys).sorted().forEach { add("added:   $it") }
                (oldById.keys - newById.keys).sorted().forEach { add("removed: $it") }
                (newById.keys intersect oldById.keys).sorted().forEach { id ->
                    val o = oldById.getValue(id); val n = newById.getValue(id)
                    if (o != n) add("changed: $id ${o.contract} -> ${n.contract} (${if (o.signature != n.signature) "signature" else "metadata"})")
                }
                if (old.libraryVersion != derived.libraryVersion) add("libraryVersion ${old.libraryVersion} -> ${derived.libraryVersion}")
            }
            fail("TCR manifest is out of date. Run `./gradlew jvmTest --tests '*ManifestSyncTest*' -Dtcr.updateManifest=true` and commit the diff:\n" + msg.joinToString("\n"))
        }
    }

    @Test fun manifestIsWellFormed() {
        val m = assertNotNull(ManifestIo.read(), "manifest missing")
        assertEquals(1, m.schemaVersion)
        assertEquals(ManifestIo.libraryVersion(), m.libraryVersion, "libraryVersion must equal build.gradle.kts version")
        assertEquals(m.components.map { it.id }.toSet().size, m.components.size, "duplicate component ids (overloads need distinct names)")
        val statuses = Status.entries.map { it.name }.toSet()
        val kinds = Kind.entries.map { it.name }.toSet()
        for (c in m.components) {
            assertTrue(Semver.isValid(c.contract), "${c.id}: contract '${c.contract}' is not semver")
            assertTrue(c.status in statuses, "${c.id}: unknown status ${c.status}")
            assertTrue(c.kind in kinds, "${c.id}: unknown kind ${c.kind}")
            assertTrue(File(c.file).isFile, "${c.id}: file ${c.file} does not exist")
            assertTrue(c.id.startsWith(ManifestScanner.ROOT + "."), "${c.id}: outside ${ManifestScanner.ROOT}")
            val pkg = ManifestScanner.ROOT + (if (c.domain == "root") "" else "." + c.domain)
            assertTrue(c.id.startsWith("$pkg."), "${c.id}: domain ${c.domain} does not match the id's package")
            // The package must match the directory, so ids are findable from the file tree.
            assertTrue(c.file.contains(pkg.replace('.', '/') + "/"), "${c.id}: package does not match path ${c.file}")
        }
    }

    @Test fun stableComponentsAreDocumentedAndAtLeastOnePointZero() {
        val m = assertNotNull(ManifestIo.read())
        for (c in m.components.filter { it.status == Status.STABLE.name }) {
            assertTrue(c.contract.substringBefore('.').toInt() >= 1, "${c.id}: STABLE needs contract >= 1.0.0")
            assertTrue(c.summary.isNotBlank(), "${c.id}: STABLE components need a KDoc summary")
        }
    }

    @Test fun everyDomainHasAtLeastOneComponentAndDeclaredDomainsAreDocumented() {
        val m = assertNotNull(ManifestIo.read())
        val readme = File("README.md").readText()
        // Domains listed in the README package map must still exist in the manifest.
        val documented = Regex("""`dev\.aidos\.tcr\.(\w+)`""").findAll(readme).map { it.groupValues[1] }.toSet()
        val domains = m.components.map { it.domain.substringBefore('.') }.toSet()
        val missing = documented - domains
        assertTrue(missing.isEmpty(), "README documents domains with no components in the manifest: $missing")
    }
}
