package dev.aidos.tcr.manifest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pins the shapes the scanner recognises, so the manifest tooling cannot silently rot. */
class ManifestScannerTest {

    private fun scan(src: String) = ManifestScanner.scan("X.kt", "package dev.aidos.tcr.demo\n\n$src")

    @Test fun composableWithDefaultsLambdasAndGenerics() {
        val s = scan(
            """
            /** A card. Second sentence. */
            @Composable
            fun DemoCard(
                title: String, // trailing comment
                items: List<Pair<String, Int>> = emptyList(),
                onTap: (id: String, count: Int) -> Unit = { _, _ -> },
                modifier: Modifier = Modifier,
            ) { }
            """.trimIndent(),
        ).single()
        assertEquals("dev.aidos.tcr.demo.DemoCard", s.id)
        assertEquals(Kind.COMPOSABLE, s.kind)
        assertEquals("A card.", s.summary)
        assertEquals(
            listOf(
                "title: String",
                "items: List<Pair<String, Int>> =",
                "onTap: (id: String, count: Int) -> Unit =",
                "modifier: Modifier =",
            ),
            s.signature,
        )
    }

    @Test fun privateAndInternalDeclarationsAreIgnored() {
        val s = scan(
            """
            @Composable
            private fun Hidden() {}
            @Composable
            internal fun AlsoHidden() {}
            private fun hiddenHelper() {}
            @Composable fun Visible(a: Int) {}
            """.trimIndent(),
        )
        assertEquals(listOf("dev.aidos.tcr.demo.Visible"), s.map { it.id })
    }

    @Test fun objectMembersAreScopedToTheirObject() {
        val s = scan(
            """
            object Registry {
                private fun secret() {}
                /** Maps things. */
                fun map(x: Int, y: String = ""): Int = x

                @Composable
                fun Render(item: String) {
                    val local = 1
                }

                override fun toString() = "x"
            }
            fun afterObject() {}
            """.trimIndent(),
        ).associateBy { it.id.removePrefix("dev.aidos.tcr.demo.") }
        assertEquals(setOf("Registry.map", "Registry.Render", "afterObject"), s.keys)
        assertEquals(Kind.FUNCTION, s.getValue("Registry.map").kind)
        assertEquals("Maps things.", s.getValue("Registry.map").summary)
        assertEquals(Kind.COMPOSABLE, s.getValue("Registry.Render").kind)
    }

    @Test fun extensionReceiverIsPartOfTheSignatureAndOverloadsGetSuffixes() {
        val s = scan(
            """
            fun Modifier.glow(r: Int) = this
            @Composable fun Atom(a: Int) {}
            @Composable fun Atom(a: Int, b: Int) {}
            """.trimIndent(),
        )
        assertEquals(listOf("this: Modifier", "r: Int"), s[0].signature)
        assertEquals(listOf("dev.aidos.tcr.demo.glow", "dev.aidos.tcr.demo.Atom", "dev.aidos.tcr.demo.Atom#2"), s.map { it.id })
    }

    @Test fun extraAnnotationsBetweenComposableAndFunAreAccepted() {
        val s = scan(
            """
            @Composable
            @OptIn(ExperimentalFoundationApi::class)
            fun Annotated(x: Int) {}
            """.trimIndent(),
        )
        assertEquals(listOf("dev.aidos.tcr.demo.Annotated"), s.map { it.id })
    }

    @Test fun dataClassAndEnumContracts() {
        val s = scan(
            """
            @Serializable
            data class Pt(val x: Int, val label: String = "a,b", var y: List<Int> = listOf(1, 2))
            enum class Mode { A, B(1), C { override fun f() = 1 }; fun g() = 0 }
            enum class Level(val n: Int) { LOW(1), HIGH(2) }
            private data class Nope(val z: Int)
            """.trimIndent(),
        ).associateBy { it.id.substringAfterLast('.') }
        assertEquals(listOf("x: Int", "label: String =", "y: List<Int> ="), s.getValue("Pt").signature)
        assertEquals(listOf("A", "B", "C"), s.getValue("Mode").signature)
        assertEquals(listOf("LOW", "HIGH"), s.getValue("Level").signature)
        assertTrue("Nope" !in s)
    }

    @Test fun classifyFollowsCallerCompatibility() {
        val old = listOf("a: Int", "b: String =")
        assertEquals(Change.NONE, ManifestScanner.classify(Kind.COMPOSABLE, old, old))
        assertEquals(Change.MINOR, ManifestScanner.classify(Kind.COMPOSABLE, old, old + "c: Int ="))
        assertEquals(Change.MAJOR, ManifestScanner.classify(Kind.COMPOSABLE, old, old + "c: Int"))
        assertEquals(Change.MAJOR, ManifestScanner.classify(Kind.COMPOSABLE, old, listOf("a: Int", "c: Int =", "b: String =")))
        assertEquals(Change.MAJOR, ManifestScanner.classify(Kind.COMPOSABLE, old, listOf("a: Int", "b: String")))
        assertEquals(Change.MAJOR, ManifestScanner.classify(Kind.COMPOSABLE, old, listOf("a: Long", "b: String =")))
        assertEquals(Change.MAJOR, ManifestScanner.classify(Kind.COMPOSABLE, old, listOf("a: Int")))
        assertEquals(Change.MINOR, ManifestScanner.classify(Kind.ENUM, listOf("A"), listOf("A", "B")))
        assertEquals(Change.MAJOR, ManifestScanner.classify(Kind.ENUM, listOf("A", "B"), listOf("A")))
    }

    @Test fun semverBumpsTreatZeroMajorAsUnstable() {
        assertEquals("0.2.0", Semver.bump("0.1.4", Change.MAJOR))
        assertEquals("0.1.5", Semver.bump("0.1.4", Change.MINOR))
        assertEquals("2.0.0", Semver.bump("1.3.2", Change.MAJOR))
        assertEquals("1.4.0", Semver.bump("1.3.2", Change.MINOR))
        assertEquals("1.3.2", Semver.bump("1.3.2", Change.NONE))
    }
}
