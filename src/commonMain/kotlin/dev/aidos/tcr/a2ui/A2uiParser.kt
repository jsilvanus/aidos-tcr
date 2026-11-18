package dev.aidos.tcr.a2ui

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed interface A2uiParseResult {
    data class Ok(val manifest: A2uiManifest) : A2uiParseResult
    data class Rejected(val reasons: List<String>) : A2uiParseResult
}

/**
 * Validates an A2UI v1 manifest against [A2uiCatalog]. Fails closed: any problem rejects the whole
 * manifest. Every problem is collected, not only the first, so a generating agent can fix them all.
 *
 * RFC-0106 §1 (manifest schema); RFC-0124 generative layer. Uses the kotlinx JSON tree API, no reflection.
 */
object A2uiParser {
    const val MAX_BYTES = 64 * 1024
    const val MAX_DEPTH = 8
    const val MAX_NODES = 200

    fun parse(json: String): A2uiParseResult {
        if (json.encodeToByteArray().size > MAX_BYTES) {
            return A2uiParseResult.Rejected(listOf("input is over 64 KiB"))
        }
        val element = try {
            Json.parseToJsonElement(json)
        } catch (e: SerializationException) {
            return A2uiParseResult.Rejected(listOf("invalid JSON: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            return A2uiParseResult.Rejected(listOf("invalid JSON: ${e.message}"))
        }
        val obj = element as? JsonObject
            ?: return A2uiParseResult.Rejected(listOf("manifest must be a JSON object"))

        val reasons = mutableListOf<String>()
        val schema = obj["\$schema"]?.stringOrNull()
        if (schema == null) reasons += "missing \$schema"
        else if (schema != A2UI_SCHEMA) reasons += "\$schema must be $A2UI_SCHEMA"

        val manifestId = requiredString(obj, "manifestId", reasons)
        val targetNodeId = requiredString(obj, "targetNodeId", reasons)

        val walker = Walker(reasons)
        val rootElement = obj["root"]
        val root = if (rootElement == null) {
            reasons += "missing root"
            null
        } else {
            walker.node(rootElement, "root", depth = 1)
        }

        if (reasons.isNotEmpty() || manifestId == null || targetNodeId == null || root == null) {
            return A2uiParseResult.Rejected(reasons)
        }
        return A2uiParseResult.Ok(A2uiManifest(manifestId, targetNodeId, root))
    }

    private fun requiredString(obj: JsonObject, key: String, reasons: MutableList<String>): String? {
        val value = obj[key]?.stringOrNull()
        if (value.isNullOrBlank()) reasons += "missing or empty $key"
        return value?.takeIf { it.isNotBlank() }
    }

    private class Walker(private val reasons: MutableList<String>) {
        private var nodeCount = 0
        private val nodeFields = setOf("type", "props", "slots", "children")

        /** Returns the node, or null when it (or its type) is rejected. Every null has a reason. */
        fun node(element: JsonElement, path: String, depth: Int): A2uiNode? {
            if (depth > MAX_DEPTH) {
                reasons += "$path: nesting is deeper than $MAX_DEPTH"
                return null
            }
            val obj = element as? JsonObject ?: run {
                reasons += "$path: a node must be a JSON object"
                return null
            }
            if (++nodeCount == MAX_NODES + 1) reasons += "manifest has more than $MAX_NODES nodes"

            obj.keys.filter { it !in nodeFields }.forEach { reasons += "$path: unknown field '$it'" }

            val type = obj["type"]?.stringOrNull()
            if (type.isNullOrBlank()) {
                reasons += "$path: missing type"
                return null
            }
            val spec = A2uiCatalog.components[type] ?: run {
                reasons += "$path: unknown type '$type'"
                return null
            }

            val props = props(obj["props"], spec, path)
            val slots = slots(obj["slots"], spec, path, depth)
            val children = children(obj["children"], spec, path, depth)

            return A2uiNode(type, props, slots, children)
        }

        private fun props(element: JsonElement?, spec: ComponentSpec, path: String): Map<String, A2uiValue> {
            val values = mutableMapOf<String, A2uiValue>()
            if (element == null) {
                checkRequired(emptyMap(), spec, path)
                return values
            }
            val obj = element as? JsonObject ?: run {
                reasons += "$path.props: must be a JSON object"
                return values
            }
            for ((name, value) in obj) {
                val propSpec = spec.props[name]
                if (propSpec == null) {
                    reasons += "$path.props.$name: unknown prop for ${spec.type}"
                    continue
                }
                prop(value, propSpec, "$path.props.$name")?.let { values[name] = it }
            }
            checkRequired(values, spec, path)
            return values
        }

        private fun checkRequired(present: Map<String, A2uiValue>, spec: ComponentSpec, path: String) {
            for ((name, propSpec) in spec.props) {
                if (propSpec.required && name !in present) {
                    reasons += "$path.props: missing required prop '$name' for ${spec.type}"
                }
            }
        }

        private fun prop(value: JsonElement, spec: PropSpec, path: String): A2uiValue? {
            if (value !is JsonPrimitive) {
                reasons += "$path: nested objects and arrays are not allowed as prop values"
                return null
            }
            if (value is JsonNull) {
                reasons += "$path: null is not a valid prop value"
                return null
            }
            return when (spec.kind) {
                PropKind.TEXT -> when {
                    !value.isString -> wrongKind(path, spec)
                    value.content.length > spec.maxLength -> {
                        reasons += "$path: text is longer than ${spec.maxLength} characters"
                        null
                    }
                    else -> A2uiValue.Text(value.content)
                }
                PropKind.ENUM -> when {
                    !value.isString -> wrongKind(path, spec)
                    value.content !in spec.enumValues -> {
                        reasons += "$path: '${value.content}' is not one of ${spec.enumValues.sorted()}"
                        null
                    }
                    else -> A2uiValue.Text(value.content)
                }
                PropKind.NUMBER -> {
                    val number = if (value.isString) null else value.content.toDoubleOrNull()
                    if (number == null) wrongKind(path, spec) else A2uiValue.Number(number)
                }
                PropKind.BOOL -> when {
                    value.isString || (value.content != "true" && value.content != "false") -> wrongKind(path, spec)
                    else -> A2uiValue.Bool(value.content == "true")
                }
            }
        }

        private fun wrongKind(path: String, spec: PropSpec): A2uiValue? {
            reasons += "$path: expected ${spec.kind}"
            return null
        }

        private fun slots(element: JsonElement?, spec: ComponentSpec, path: String, depth: Int): Map<String, A2uiNode> {
            if (element == null) return emptyMap()
            val obj = element as? JsonObject ?: run {
                reasons += "$path.slots: must be a JSON object"
                return emptyMap()
            }
            val result = mutableMapOf<String, A2uiNode>()
            for ((name, child) in obj) {
                if (name !in spec.slots) {
                    reasons += "$path.slots.$name: unknown slot for ${spec.type}"
                    continue
                }
                node(child, "$path.slots.$name", depth + 1)?.let { result[name] = it }
            }
            return result
        }

        private fun children(element: JsonElement?, spec: ComponentSpec, path: String, depth: Int): List<A2uiNode> {
            if (element == null) return emptyList()
            val array = element as? JsonArray ?: run {
                reasons += "$path.children: must be a JSON array"
                return emptyList()
            }
            if (!spec.allowsChildren && array.isNotEmpty()) {
                reasons += "$path: ${spec.type} does not allow children"
            }
            val result = mutableListOf<A2uiNode>()
            array.forEachIndexed { i, child ->
                val childPath = "$path.children[$i]"
                val node = node(child, childPath, depth + 1) ?: return@forEachIndexed
                val allowed = spec.childTypes
                if (allowed != null && node.type !in allowed) {
                    reasons += "$childPath: ${node.type} is not allowed inside ${spec.type}"
                }
                result += node
            }
            return result
        }
    }
}

private fun JsonElement.stringOrNull(): String? =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.content
