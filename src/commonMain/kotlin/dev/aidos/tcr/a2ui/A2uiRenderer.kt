package dev.aidos.tcr.a2ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import dev.aidos.tcr.atom.code.DiffChunkAtom
import dev.aidos.tcr.atom.code.DiffLine
import dev.aidos.tcr.atom.code.DiffLineKind
import dev.aidos.tcr.atom.code.ScopePathChipAtom
import dev.aidos.tcr.atom.code.StepCounterAtom
import dev.aidos.tcr.atom.core.ActionButtonAtom
import dev.aidos.tcr.atom.core.ActionButtonKind
import dev.aidos.tcr.atom.core.MetricBadgeAtom
import dev.aidos.tcr.atom.core.TextSpanAtom
import dev.aidos.tcr.atom.molecule.InlineCardMolecule
import dev.aidos.tcr.atom.molecule.MetricClusterMolecule
import dev.aidos.tcr.atom.molecule.MetricItem
import dev.aidos.tcr.theme.SpacingTokens
import dev.aidos.tcr.theme.TcrColors
import dev.aidos.tcr.theme.TcrTypography

/** How tightly a manifest is laid out. A card's own `density` prop overrides the renderer argument. */
enum class A2uiDensity { COMPACT, REGULAR }

/**
 * Draws a validated [A2uiManifest] using only TCR components (RFC-0124 generative layer, RFC-0106 §1).
 *
 * The renderer does no parsing: the caller passes the result of [A2uiParser.parse]. Every node type
 * named in [A2uiCatalog] has one branch below, and nothing else is drawn.
 *
 * [onAction] receives the `actionId` of a pressed [ActionButton][A2uiCatalog]. When [actionsEnabled] is
 * false every action button is drawn disabled and cannot fire.
 */
@Composable
fun A2uiRenderer(
    manifest: A2uiManifest,
    density: A2uiDensity,
    onAction: (actionId: String) -> Unit,
    actionsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        A2uiNodeView(manifest.root, density, onAction, actionsEnabled)
    }
}

@Composable
private fun A2uiNodeView(
    node: A2uiNode,
    density: A2uiDensity,
    onAction: (actionId: String) -> Unit,
    actionsEnabled: Boolean,
) {
    val spacing = spacingFor(density)
    when (node.type) {
        "InlineCard" -> {
            val cardDensity = densityOf(node.text("density")) ?: density
            val header = node.slots["header"]
            // A header made only of text becomes the card title; anything else is drawn in the body.
            val title = header?.let { plainText(it) }.orEmpty()
            InlineCardMolecule(title = title, borderColor = intentColor(node.text("intent"))) {
                Column(verticalArrangement = Arrangement.spacedBy(spacingFor(cardDensity))) {
                    if (header != null && plainText(header) == null) {
                        A2uiNodeView(header, cardDensity, onAction, actionsEnabled)
                    }
                    node.slots["body"]?.let { A2uiNodeView(it, cardDensity, onAction, actionsEnabled) }
                    node.slots["footer"]?.let { A2uiNodeView(it, cardDensity, onAction, actionsEnabled) }
                }
            }
        }
        "Row" -> Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            node.children.forEach { A2uiNodeView(it, density, onAction, actionsEnabled) }
        }
        "Column" -> Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            node.children.forEach { A2uiNodeView(it, density, onAction, actionsEnabled) }
        }
        "ActionBar" -> Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            node.children.forEach { A2uiNodeView(it, density, onAction, actionsEnabled) }
        }
        "TextSpan" -> {
            val bold = node.text("weight") == "bold"
            val base = TcrTypography.baseTypography.bodyMedium
            TextSpanAtom(
                text = node.text("text").orEmpty(),
                style = if (bold) base.copy(fontWeight = FontWeight.Bold) else base,
            )
        }
        "MetricCluster" -> MetricClusterMolecule(
            metrics = node.children.map { child ->
                MetricItem(label = child.text("label").orEmpty(), value = metricValueText(child))
            },
        )
        "MetricValue" -> MetricBadgeAtom(
            label = node.text("label").orEmpty(),
            value = metricValueText(node),
        )
        "ActionButton" -> {
            val actionId = node.text("actionId").orEmpty()
            ActionButtonAtom(
                text = node.text("label").orEmpty(),
                onClick = { onAction(actionId) },
                kind = buttonKindOf(node.text("intent")),
                enabled = actionsEnabled,
            )
        }
        "DiffChunk" -> DiffChunkAtom(
            oldPath = null,
            newPath = node.text("path").orEmpty(),
            lines = parseDiff(node.text("diff").orEmpty()),
        )
        "ScopePathChip" -> ScopePathChipAtom(path = node.text("path").orEmpty())
        "StepCounter" -> StepCounterAtom(
            currentStep = node.number("current")?.toInt() ?: 0,
            maxSteps = node.number("total")?.toInt() ?: 0,
        )
        // The parser rejects every type outside A2uiCatalog, so a parsed manifest never reaches here.
        else -> Unit
    }
}

private fun A2uiNode.text(name: String): String? = (props[name] as? A2uiValue.Text)?.value

private fun A2uiNode.number(name: String): Double? = (props[name] as? A2uiValue.Number)?.value

private fun spacingFor(density: A2uiDensity): Dp = when (density) {
    A2uiDensity.COMPACT -> SpacingTokens.xs
    A2uiDensity.REGULAR -> SpacingTokens.sm
}

private fun densityOf(value: String?): A2uiDensity? = when (value) {
    "compact" -> A2uiDensity.COMPACT
    "regular" -> A2uiDensity.REGULAR
    else -> null
}

/** Intent colours from the design tokens. `neutral` and no intent use the surface outline. */
@Composable
private fun intentColor(intent: String?): Color = when (intent) {
    "accent" -> TcrColors.ElectricBlue
    "warning" -> TcrColors.AmberWarning
    "danger" -> TcrColors.EmberRed
    "success" -> TcrColors.EmeraldGreen
    else -> MaterialTheme.colorScheme.surfaceVariant
}

private fun buttonKindOf(intent: String?): ActionButtonKind = when (intent) {
    "secondary" -> ActionButtonKind.SECONDARY
    "danger" -> ActionButtonKind.DANGER
    else -> ActionButtonKind.PRIMARY
}

/** A metric with a delta reads "value (delta)"; the badge atom has no separate delta slot. */
private fun metricValueText(node: A2uiNode): String {
    val value = node.text("value").orEmpty()
    val delta = node.text("delta")
    return if (delta.isNullOrEmpty()) value else "$value ($delta)"
}

/** The text of a node that holds only text, or null when it has any other content. */
private fun plainText(node: A2uiNode): String? = when (node.type) {
    "TextSpan" -> node.text("text")
    "Row", "Column" -> node.children.map { plainText(it) ?: return null }.joinToString(" ")
    else -> null
}

/**
 * Splits a unified-diff body into lines. The first character is the change marker and is dropped,
 * because [DiffChunkAtom] draws its own `+` / `-` prefix.
 */
private fun parseDiff(diff: String): List<DiffLine> = diff.lines().map { line ->
    when {
        line.startsWith("+") -> DiffLine(DiffLineKind.ADDITION, line.drop(1))
        line.startsWith("-") -> DiffLine(DiffLineKind.DELETION, line.drop(1))
        else -> DiffLine(DiffLineKind.CONTEXT, line.removePrefix(" "))
    }
}
