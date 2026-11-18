package dev.aidos.tcr.a2ui

/** The `$schema` value every A2UI v1 manifest must carry. */
const val A2UI_SCHEMA = "https://aidos.dev/schemas/a2ui-v1.json"

enum class PropKind { TEXT, NUMBER, BOOL, ENUM }

data class PropSpec(
    val kind: PropKind,
    val required: Boolean = false,
    val enumValues: Set<String> = emptySet(),
    val maxLength: Int = 500,
)

/**
 * One allowlisted A2UI type.
 *
 * [tcrId] is the manifest id of the TCR composable that renders it (`manifest/tcr-manifest.json`),
 * and [contract] is that entry's contract version. Both are null/checked by the catalog test.
 * [tcrId] is null for layout-only types (Row, Column, ActionBar): the renderer lays them out itself
 * and no TCR atom exists for them.
 * [childTypes] null means any type may be a child; a set restricts children to those types.
 */
data class ComponentSpec(
    val type: String,
    val tcrId: String?,
    val contract: String,
    val props: Map<String, PropSpec>,
    val slots: Set<String>,
    val allowsChildren: Boolean,
    val childTypes: Set<String>? = null,
)

/**
 * The explicit v1 allowlist. Nothing outside it is rendered: an unknown type fails closed.
 *
 * The `Icon` type from RFC-0106's example is deliberately absent: there is no vetted icon atom yet.
 */
object A2uiCatalog {
    private const val V1 = "0.1.0"

    val components: Map<String, ComponentSpec> = listOf(
        ComponentSpec(
            type = "InlineCard",
            tcrId = "dev.aidos.tcr.atom.molecule.InlineCardMolecule",
            contract = V1,
            props = mapOf(
                "intent" to PropSpec(PropKind.ENUM, enumValues = setOf("neutral", "accent", "warning", "danger", "success")),
                "density" to PropSpec(PropKind.ENUM, enumValues = setOf("compact", "regular")),
                "elevation" to PropSpec(PropKind.ENUM, enumValues = setOf("flat", "glass")),
            ),
            slots = setOf("header", "body", "footer"),
            allowsChildren = false,
        ),
        ComponentSpec(
            type = "Row",
            tcrId = null,
            contract = V1,
            props = emptyMap(),
            slots = emptySet(),
            allowsChildren = true,
        ),
        ComponentSpec(
            type = "Column",
            tcrId = null,
            contract = V1,
            props = emptyMap(),
            slots = emptySet(),
            allowsChildren = true,
        ),
        ComponentSpec(
            type = "TextSpan",
            tcrId = "dev.aidos.tcr.atom.core.TextSpanAtom",
            contract = V1,
            props = mapOf(
                "text" to PropSpec(PropKind.TEXT, required = true),
                "weight" to PropSpec(PropKind.ENUM, enumValues = setOf("regular", "bold")),
            ),
            slots = emptySet(),
            allowsChildren = false,
        ),
        ComponentSpec(
            type = "MetricCluster",
            tcrId = "dev.aidos.tcr.atom.molecule.MetricClusterMolecule",
            contract = V1,
            props = emptyMap(),
            slots = emptySet(),
            allowsChildren = true,
            childTypes = setOf("MetricValue"),
        ),
        ComponentSpec(
            type = "MetricValue",
            tcrId = "dev.aidos.tcr.atom.core.MetricBadgeAtom",
            contract = V1,
            props = mapOf(
                "label" to PropSpec(PropKind.TEXT, required = true),
                "value" to PropSpec(PropKind.TEXT, required = true),
                "delta" to PropSpec(PropKind.TEXT),
            ),
            slots = emptySet(),
            allowsChildren = false,
        ),
        ComponentSpec(
            type = "ActionBar",
            tcrId = null,
            contract = V1,
            props = emptyMap(),
            slots = emptySet(),
            allowsChildren = true,
            childTypes = setOf("ActionButton"),
        ),
        ComponentSpec(
            type = "ActionButton",
            tcrId = "dev.aidos.tcr.atom.core.ActionButtonAtom",
            contract = V1,
            props = mapOf(
                "label" to PropSpec(PropKind.TEXT, required = true),
                "actionId" to PropSpec(PropKind.TEXT, required = true, maxLength = 64),
                "intent" to PropSpec(PropKind.ENUM, enumValues = setOf("primary", "secondary", "danger")),
            ),
            slots = emptySet(),
            allowsChildren = false,
        ),
        ComponentSpec(
            type = "DiffChunk",
            tcrId = "dev.aidos.tcr.atom.code.DiffChunkAtom",
            contract = V1,
            props = mapOf(
                "path" to PropSpec(PropKind.TEXT, required = true),
                "diff" to PropSpec(PropKind.TEXT, required = true, maxLength = 8000),
            ),
            slots = emptySet(),
            allowsChildren = false,
        ),
        ComponentSpec(
            type = "ScopePathChip",
            tcrId = "dev.aidos.tcr.atom.code.ScopePathChipAtom",
            contract = V1,
            props = mapOf("path" to PropSpec(PropKind.TEXT, required = true)),
            slots = emptySet(),
            allowsChildren = false,
        ),
        ComponentSpec(
            type = "StepCounter",
            tcrId = "dev.aidos.tcr.atom.code.StepCounterAtom",
            contract = V1,
            props = mapOf(
                "current" to PropSpec(PropKind.NUMBER, required = true),
                "total" to PropSpec(PropKind.NUMBER, required = true),
            ),
            slots = emptySet(),
            allowsChildren = false,
        ),
    ).associateBy { it.type }
}
