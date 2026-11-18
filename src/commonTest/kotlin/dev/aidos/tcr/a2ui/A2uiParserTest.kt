package dev.aidos.tcr.a2ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class A2uiParserTest {

    private fun manifest(root: String, schema: String? = A2UI_SCHEMA): String {
        val schemaField = if (schema == null) "" else "\"\$schema\":\"$schema\","
        return "{$schemaField\"manifestId\":\"ui_manifest_42\",\"targetNodeId\":\"goal_node\",\"root\":$root}"
    }

    private fun ok(json: String): A2uiManifest {
        val result = A2uiParser.parse(json)
        assertIs<A2uiParseResult.Ok>(result, "expected Ok, got $result")
        return result.manifest
    }

    private fun reasons(json: String): List<String> {
        val result = A2uiParser.parse(json)
        assertIs<A2uiParseResult.Rejected>(result, "expected Rejected, got $result")
        assertTrue(result.reasons.isNotEmpty())
        return result.reasons
    }

    private fun assertRejectedWith(json: String, fragment: String) {
        val all = reasons(json)
        assertTrue(all.any { fragment in it }, "no reason contains '$fragment' in $all")
    }

    @Test
    fun rfcExampleWithoutIconParsesToExpectedTree() {
        val json = """
            {
              "${'$'}schema": "https://aidos.dev/schemas/a2ui-v1.json",
              "manifestId": "ui_manifest_42",
              "targetNodeId": "goal_node_audio_buffer",
              "root": {
                "type": "InlineCard",
                "props": { "intent": "accent", "density": "compact", "elevation": "glass" },
                "slots": {
                  "header": {
                    "type": "Row",
                    "children": [
                      { "type": "TextSpan", "props": { "text": "Buffer Optimization Result", "weight": "bold" } }
                    ]
                  },
                  "body": {
                    "type": "MetricCluster",
                    "children": [
                      { "type": "MetricValue", "props": { "label": "Latency", "value": "18ms", "delta": "-14ms" } }
                    ]
                  },
                  "footer": {
                    "type": "ActionBar",
                    "children": [
                      { "type": "ActionButton", "props": { "label": "Apply Patch", "actionId": "apply_patch", "intent": "primary" } }
                    ]
                  }
                }
              }
            }
        """.trimIndent()

        val m = ok(json)
        assertEquals("ui_manifest_42", m.manifestId)
        assertEquals("goal_node_audio_buffer", m.targetNodeId)

        val root = m.root
        assertEquals("InlineCard", root.type)
        assertEquals(
            mapOf(
                "intent" to A2uiValue.Text("accent"),
                "density" to A2uiValue.Text("compact"),
                "elevation" to A2uiValue.Text("glass"),
            ),
            root.props,
        )
        assertEquals(setOf("header", "body", "footer"), root.slots.keys)
        assertTrue(root.children.isEmpty())

        val header = root.slots.getValue("header")
        assertEquals("Row", header.type)
        val span = header.children.single()
        assertEquals("TextSpan", span.type)
        assertEquals(A2uiValue.Text("Buffer Optimization Result"), span.props["text"])

        val body = root.slots.getValue("body")
        assertEquals("MetricCluster", body.type)
        val metric = body.children.single()
        assertEquals(A2uiValue.Text("18ms"), metric.props["value"])
        assertEquals(A2uiValue.Text("-14ms"), metric.props["delta"])

        val button = root.slots.getValue("footer").children.single()
        assertEquals("ActionButton", button.type)
        assertEquals(A2uiValue.Text("apply_patch"), button.props["actionId"])
    }

    @Test
    fun numberPropsAreParsedAsNumbers() {
        val m = ok(manifest("""{"type":"StepCounter","props":{"current":2,"total":5}}"""))
        assertEquals(A2uiValue.Number(2.0), m.root.props["current"])
        assertEquals(A2uiValue.Number(5.0), m.root.props["total"])
    }

    @Test
    fun wrongOrMissingSchemaIsRejected() {
        assertRejectedWith(manifest("""{"type":"TextSpan","props":{"text":"a"}}""", schema = "https://evil.example/x.json"), "\$schema")
        assertRejectedWith(manifest("""{"type":"TextSpan","props":{"text":"a"}}""", schema = null), "missing \$schema")
    }

    @Test
    fun missingIdsAreRejected() {
        val json = "{\"\$schema\":\"$A2UI_SCHEMA\",\"root\":{\"type\":\"TextSpan\",\"props\":{\"text\":\"a\"}}}"
        val all = reasons(json)
        assertTrue(all.any { "manifestId" in it }, all.toString())
        assertTrue(all.any { "targetNodeId" in it }, all.toString())
    }

    @Test
    fun inputOver64KiBIsRejected() {
        val big = "x".repeat(A2uiParser.MAX_BYTES)
        assertRejectedWith(manifest("""{"type":"TextSpan","props":{"text":"$big"}}"""), "64 KiB")
    }

    @Test
    fun nestingDeeperThanEightIsRejectedAndEightIsAccepted() {
        fun chain(columns: Int): String {
            val leaf = """{"type":"TextSpan","props":{"text":"leaf"}}"""
            return "{\"type\":\"Column\",\"children\":[".repeat(columns) + leaf + "]}".repeat(columns)
        }
        // root is depth 1, so 7 Columns put the leaf at depth 8 (allowed), 8 Columns at depth 9 (rejected).
        ok(manifest(chain(7)))
        assertRejectedWith(manifest(chain(8)), "nesting is deeper than 8")
    }

    @Test
    fun moreThan200NodesIsRejected() {
        fun flat(nodes: Int): String {
            val children = (1 until nodes).joinToString(",") { """{"type":"TextSpan","props":{"text":"t"}}""" }
            return """{"type":"Column","children":[$children]}"""
        }
        ok(manifest(flat(200)))
        assertRejectedWith(manifest(flat(201)), "more than 200 nodes")
    }

    @Test
    fun unknownTypeIsRejected() {
        assertRejectedWith(manifest("""{"type":"Icon","props":{"name":"cpu"}}"""), "unknown type 'Icon'")
    }

    @Test
    fun unknownTypeNestedInSlotIsRejected() {
        val json = manifest(
            """{"type":"InlineCard","slots":{"body":{"type":"Sparkle","props":{}}}}""",
        )
        assertRejectedWith(json, "unknown type 'Sparkle'")
    }

    @Test
    fun unknownPropIsRejected() {
        assertRejectedWith(manifest("""{"type":"TextSpan","props":{"text":"a","color":"red"}}"""), "unknown prop")
    }

    @Test
    fun wrongPropKindIsRejected() {
        assertRejectedWith(manifest("""{"type":"TextSpan","props":{"text":42}}"""), "expected TEXT")
        assertRejectedWith(manifest("""{"type":"StepCounter","props":{"current":"2","total":5}}"""), "expected NUMBER")
    }

    @Test
    fun enumValueNotAllowedIsRejected() {
        assertRejectedWith(manifest("""{"type":"InlineCard","props":{"intent":"rainbow"}}"""), "'rainbow' is not one of")
    }

    @Test
    fun missingRequiredPropIsRejected() {
        assertRejectedWith(manifest("""{"type":"TextSpan","props":{"weight":"bold"}}"""), "missing required prop 'text'")
    }

    @Test
    fun textOverMaxLengthIsRejected() {
        val tooLong = "a".repeat(65)
        assertRejectedWith(
            manifest("""{"type":"ActionButton","props":{"label":"Go","actionId":"$tooLong"}}"""),
            "longer than 64 characters",
        )
    }

    @Test
    fun unknownSlotIsRejected() {
        assertRejectedWith(manifest("""{"type":"InlineCard","slots":{"sidebar":{"type":"TextSpan","props":{"text":"a"}}}}"""), "unknown slot")
    }

    @Test
    fun childrenOnTypeThatDoesNotAllowThemIsRejected() {
        assertRejectedWith(
            manifest("""{"type":"InlineCard","children":[{"type":"TextSpan","props":{"text":"a"}}]}"""),
            "does not allow children",
        )
    }

    @Test
    fun childTypeNotAllowedByParentIsRejected() {
        assertRejectedWith(
            manifest("""{"type":"MetricCluster","children":[{"type":"TextSpan","props":{"text":"a"}}]}"""),
            "TextSpan is not allowed inside MetricCluster",
        )
        assertRejectedWith(
            manifest("""{"type":"ActionBar","children":[{"type":"Column","children":[]}]}"""),
            "Column is not allowed inside ActionBar",
        )
    }

    @Test
    fun nestedObjectOrArrayAsPropValueIsRejected() {
        assertRejectedWith(manifest("""{"type":"TextSpan","props":{"text":{"a":"b"}}}"""), "nested objects and arrays")
        assertRejectedWith(manifest("""{"type":"TextSpan","props":{"text":["a"]}}"""), "nested objects and arrays")
    }

    @Test
    fun allProblemsAreCollectedNotOnlyTheFirst() {
        val json = manifest(
            """{"type":"InlineCard","props":{"intent":"rainbow","nope":1},"slots":{"sidebar":{"type":"TextSpan","props":{"text":"a"}},"body":{"type":"Icon"}}}""",
        )
        val all = reasons(json)
        assertTrue(all.size >= 4, "expected several reasons, got $all")
        assertTrue(all.any { "rainbow" in it })
        assertTrue(all.any { "unknown prop" in it })
        assertTrue(all.any { "unknown slot" in it })
        assertTrue(all.any { "unknown type 'Icon'" in it })
    }

    @Test
    fun invalidJsonIsRejected() {
        assertRejectedWith("{not json", "invalid JSON")
    }

    @Test
    fun nonObjectTopLevelIsRejected() {
        assertRejectedWith("[]", "must be a JSON object")
    }
}
