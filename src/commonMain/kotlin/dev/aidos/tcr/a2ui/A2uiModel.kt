package dev.aidos.tcr.a2ui

/**
 * A validated A2UI v1 manifest (RFC-0106 §1, RFC-0124 generative layer).
 *
 * Only ever produced by [A2uiParser], which rejects anything outside [A2uiCatalog].
 */
data class A2uiManifest(
    val manifestId: String,
    val targetNodeId: String,
    val root: A2uiNode,
)

/** One component in a manifest tree. [slots] are named single children, [children] an ordered list. */
data class A2uiNode(
    val type: String,
    val props: Map<String, A2uiValue>,
    val slots: Map<String, A2uiNode>,
    val children: List<A2uiNode>,
)

/** A prop value. v1 has no nested objects or arrays. */
sealed interface A2uiValue {
    data class Text(val value: String) : A2uiValue
    data class Number(val value: Double) : A2uiValue
    data class Bool(val value: Boolean) : A2uiValue
}
