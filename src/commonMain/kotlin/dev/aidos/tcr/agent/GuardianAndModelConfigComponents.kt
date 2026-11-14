package dev.aidos.tcr.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.TcrColors

/**
 * Maps an [AgentLocality] ceiling to its compact TCR badge label (`LOCAL`, `CLUSTER`, `REMOTE`).
 */
internal fun tcrLocalityShortLabel(locality: AgentLocality): String = when (locality) {
    AgentLocality.LOCAL_ONLY -> "LOCAL"
    AgentLocality.TRUSTED_CLUSTER -> "CLUSTER"
    AgentLocality.ANY -> "REMOTE"
}

/**
 * Maps an [AgentLocality] ceiling to its canonical [TcrColors] semantic token.
 */
internal fun tcrLocalityColor(locality: AgentLocality): Color = when (locality) {
    AgentLocality.LOCAL_ONLY -> TcrColors.EmeraldGreen
    AgentLocality.TRUSTED_CLUSTER -> TcrColors.ElectricBlue
    AgentLocality.ANY -> TcrColors.VioletProtocol
}

/**
 * Maps a [TcrTaintApprovalPolicy] to its semantic highlight color (`AmberWarning` for Guardian,
 * neutral `null` for manual human gate).
 */
internal fun tcrPolicyAccentColor(policy: TcrTaintApprovalPolicy): Color? = when (policy) {
    TcrTaintApprovalPolicy.MANUAL -> null
    TcrTaintApprovalPolicy.GUARDIAN -> TcrColors.AmberWarning
}

/**
 * Compact TCR pill badge matching [AgentRunCard] and [AgentPlacementBadge] visual grammar.
 */
@Composable
internal fun TcrStatusPillBadge(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
        )
    }
}

/**
 * Renders the compact Guardian, Main-Model-chosen, and Enforced Locality pill badges for a run.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TcrRunPolicyBadges(
    run: AgentRunUiModel,
    modifier: Modifier = Modifier,
) {
    val hasPolicyBadge = run.guardianPolicy == TcrTaintApprovalPolicy.GUARDIAN
    val hasMainChosenBadge = run.modelChosenByMainModel
    val locality = run.enforcedLocality
    if (!hasPolicyBadge && !hasMainChosenBadge && locality == null) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (hasPolicyBadge) {
            TcrStatusPillBadge(
                text = "GUARDIAN",
                accentColor = TcrColors.AmberWarning,
            )
        }
        if (hasMainChosenBadge) {
            TcrStatusPillBadge(
                text = "MAIN-CHOSEN",
                accentColor = TcrColors.ElectricBlue,
            )
        }
        if (locality != null) {
            TcrStatusPillBadge(
                text = tcrLocalityShortLabel(locality),
                accentColor = tcrLocalityColor(locality),
            )
        }
    }
}

@Composable
private fun TcrColoredFilterChip(
    label: String,
    selected: Boolean,
    accentColor: Color?,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (accentColor != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(accentColor, CircleShape),
                    )
                }
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (accentColor != null) {
                accentColor.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            selectedLabelColor = if (accentColor != null) {
                accentColor
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            selectedBorderColor = accentColor ?: MaterialTheme.colorScheme.outline,
            selectedBorderWidth = 1.dp,
        ),
    )
}

/**
 * Shared editable model-ID field with optional quick-pick chips and clear action.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TcrModelSelectionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    availableModels: List<String>,
    modifier: Modifier = Modifier,
    defaultChipLabel: String? = null,
    supportingNote: String? = null,
    accentColor: Color = TcrColors.ElectricBlue,
) {
    val distinctModels = availableModels
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            },
            trailingIcon = if (value.isNotBlank()) {
                {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear $label",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            } else {
                null
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            ),
        )

        if (defaultChipLabel != null || distinctModels.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (defaultChipLabel != null) {
                    TcrColoredFilterChip(
                        label = defaultChipLabel,
                        selected = value.isBlank(),
                        accentColor = null,
                        onClick = { onValueChange("") },
                    )
                }
                distinctModels.forEach { modelId ->
                    TcrColoredFilterChip(
                        label = modelId,
                        selected = value == modelId,
                        accentColor = accentColor,
                        onClick = { onValueChange(modelId) },
                    )
                }
            }
        }

        if (!supportingNote.isNullOrBlank()) {
            Text(
                text = supportingNote,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Atomic TCR Component: TcrLocalitySelector
 *
 * Pill/chip selector for enforced execution locality (`LOCAL` / `CLUSTER` / `REMOTE`),
 * with an optional `Inherit` choice for per-run overrides.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TcrLocalitySelector(
    selected: AgentLocality?,
    onSelect: (AgentLocality?) -> Unit,
    allowInherit: Boolean = false,
    label: String = "Enforced Running Place",
    modifier: Modifier = Modifier,
    inheritLabel: String = "Inherit",
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (allowInherit) {
                TcrColoredFilterChip(
                    label = inheritLabel,
                    selected = selected == null,
                    accentColor = null,
                    onClick = { onSelect(null) },
                )
            }
            AgentLocality.entries.forEach { locality ->
                TcrColoredFilterChip(
                    label = tcrLocalityShortLabel(locality),
                    selected = selected == locality,
                    accentColor = tcrLocalityColor(locality),
                    onClick = { onSelect(locality) },
                )
            }
        }
    }
}

/**
 * Atomic TCR Component: TcrGuardianPolicySelector
 *
 * Pill/chip selector for taint approval policy (`MANUAL` human gate vs `GUARDIAN` automated
 * Gate A + Gate B intent alignment), with an optional `Inherit` choice for per-run overrides.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TcrGuardianPolicySelector(
    selected: TcrTaintApprovalPolicy?,
    onSelect: (TcrTaintApprovalPolicy?) -> Unit,
    allowInherit: Boolean = false,
    label: String = "Taint Approval Policy",
    modifier: Modifier = Modifier,
    inheritLabel: String = "Inherit",
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (allowInherit) {
                TcrColoredFilterChip(
                    label = inheritLabel,
                    selected = selected == null,
                    accentColor = null,
                    onClick = { onSelect(null) },
                )
            }
            TcrTaintApprovalPolicy.entries.forEach { policy ->
                TcrColoredFilterChip(
                    label = policy.name,
                    selected = selected == policy,
                    accentColor = tcrPolicyAccentColor(policy),
                    onClick = { onSelect(policy) },
                )
            }
        }

        val description = when (selected) {
            TcrTaintApprovalPolicy.GUARDIAN ->
                "Auto-checks intent alignment via Gate A (Decision) + Gate B (Guardian LLM)."
            TcrTaintApprovalPolicy.MANUAL ->
                "Human approval gate required before executing tainted operations."
            null ->
                "Inherits the active project or global default taint approval policy."
        }
        Text(
            text = description,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Molecular TCR Component: TcrGuardianModelsCard
 *
 * Configures the two-gate Guardian safety verification policy, Gate A decision classifier model,
 * and optional Gate B intent-alignment Guardian LLM.
 */
@Composable
fun TcrGuardianModelsCard(
    config: TcrGuardianConfigUiModel,
    onPolicyChange: (TcrTaintApprovalPolicy) -> Unit,
    onDecisionModelChange: (String) -> Unit,
    onGuardianLlmChange: (String) -> Unit,
    title: String = "Guardian Safety Gate",
    modifier: Modifier = Modifier,
) {
    val isGuardian = config.policy == TcrTaintApprovalPolicy.GUARDIAN
    val borderColor = if (isGuardian) {
        TcrColors.AmberWarning.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TcrStatusPillBadge(
                    text = config.policy.name,
                    accentColor = if (isGuardian) TcrColors.AmberWarning else MaterialTheme.colorScheme.outline,
                )
            }

            TcrGuardianPolicySelector(
                selected = config.policy,
                onSelect = { policy -> if (policy != null) onPolicyChange(policy) },
                allowInherit = false,
            )

            TcrModelSelectionField(
                label = "Gate A Decision Model",
                value = config.decisionModelId,
                onValueChange = onDecisionModelChange,
                placeholder = "laya-multilingual",
                availableModels = config.availableDecisionModels.ifEmpty { listOf("laya-multilingual") },
                accentColor = TcrColors.AmberWarning,
            )

            TcrModelSelectionField(
                label = "Gate B Guardian LLM",
                value = config.guardianLlmModelId,
                onValueChange = onGuardianLlmChange,
                placeholder = "Auto (router default LLM)",
                availableModels = config.availableLlmModels,
                defaultChipLabel = if (config.availableLlmModels.isNotEmpty() || config.guardianLlmModelId.isNotBlank()) {
                    "Auto"
                } else {
                    null
                },
                accentColor = TcrColors.AmberWarning,
            )
        }
    }
}

/**
 * Organism TCR Component: TcrProjectModelRoutingCard
 *
 * Project-level control card for configuring the Main Model, Default Model for Runs,
 * Enforced Model Running Place, and embedded Project [TcrGuardianModelsCard].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TcrProjectModelRoutingCard(
    config: TcrProjectModelConfigUiModel,
    onMainModelChange: (String) -> Unit,
    onDefaultRunModelChange: (String) -> Unit,
    onLocalityChange: (AgentLocality) -> Unit,
    onGuardianPolicyChange: (TcrTaintApprovalPolicy) -> Unit,
    onDecisionModelChange: (String) -> Unit,
    onGuardianLlmChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayTitle = config.projectName.ifBlank {
        config.projectId.ifBlank { "Project Model Routing" }
    }
    val effectiveGuardianConfig = config.guardianConfig.copy(
        availableDecisionModels = config.guardianConfig.availableDecisionModels.ifEmpty { config.availableDecisionModels },
        availableLlmModels = config.guardianConfig.availableLlmModels.ifEmpty { config.availableLlmModels },
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = displayTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (config.projectId.isNotBlank() && config.projectId != displayTitle) {
                        Text(
                            text = config.projectId,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TcrStatusPillBadge(
                        text = "PROJECT ROUTING",
                        accentColor = TcrColors.ElectricBlue,
                    )
                    TcrStatusPillBadge(
                        text = tcrLocalityShortLabel(config.enforcedLocality),
                        accentColor = tcrLocalityColor(config.enforcedLocality),
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            TcrModelSelectionField(
                label = "Main Model",
                value = config.mainModelId,
                onValueChange = onMainModelChange,
                placeholder = "Select or enter main model ID",
                availableModels = config.availableLlmModels,
                accentColor = TcrColors.ElectricBlue,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TcrModelSelectionField(
                    label = "Default Model for Runs",
                    value = config.defaultRunModelId,
                    onValueChange = onDefaultRunModelChange,
                    placeholder = "Same as Main Model",
                    availableModels = config.availableLlmModels,
                    defaultChipLabel = if (config.availableLlmModels.isNotEmpty() || config.defaultRunModelId.isNotBlank()) {
                        "Same as Main"
                    } else {
                        null
                    },
                    accentColor = TcrColors.ElectricBlue,
                )

                if (config.allowMainModelRunOverride) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TcrStatusPillBadge(
                            text = "MAIN-CHOSEN ENABLED",
                            accentColor = TcrColors.ElectricBlue,
                        )
                        Text(
                            text = "Main Model can choose a specific model per sub-run when needed.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            TcrLocalitySelector(
                selected = config.enforcedLocality,
                onSelect = { locality -> if (locality != null) onLocalityChange(locality) },
                allowInherit = false,
                label = "Enforced Model Running Place",
            )

            TcrGuardianModelsCard(
                config = effectiveGuardianConfig,
                onPolicyChange = onGuardianPolicyChange,
                onDecisionModelChange = onDecisionModelChange,
                onGuardianLlmChange = onGuardianLlmChange,
            )
        }
    }
}

/**
 * Molecular TCR Component: TcrRunConfigBar
 *
 * Compact per-run override bar allowing operators to inherit or override the project's
 * Guardian approval policy, enforced locality ceiling, and target run model.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TcrRunConfigBar(
    config: TcrRunLaunchConfigUiModel,
    effectivePolicy: TcrTaintApprovalPolicy,
    effectiveModelId: String,
    effectiveLocality: AgentLocality,
    availableLlmModels: List<String> = emptyList(),
    onGuardianPolicyOverride: (TcrTaintApprovalPolicy?) -> Unit,
    onRunModelOverride: (String) -> Unit,
    onLocalityOverride: (AgentLocality?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolvedPolicy = config.guardianPolicyOverride ?: effectivePolicy
    val resolvedLocality = config.localityOverride ?: effectiveLocality
    val resolvedModel = config.runModelOverride.ifBlank { effectiveModelId.ifBlank { "Auto" } }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Run Launch Overrides",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TcrStatusPillBadge(
                        text = resolvedModel,
                        accentColor = TcrColors.ElectricBlue,
                    )
                    TcrStatusPillBadge(
                        text = tcrLocalityShortLabel(resolvedLocality),
                        accentColor = tcrLocalityColor(resolvedLocality),
                    )
                    TcrStatusPillBadge(
                        text = resolvedPolicy.name,
                        accentColor = tcrPolicyAccentColor(resolvedPolicy) ?: MaterialTheme.colorScheme.outline,
                    )
                }
            }

            TcrGuardianPolicySelector(
                selected = config.guardianPolicyOverride,
                onSelect = onGuardianPolicyOverride,
                allowInherit = true,
                label = "Taint Approval Override",
                inheritLabel = "Inherit (${effectivePolicy.name})",
            )

            TcrLocalitySelector(
                selected = config.localityOverride,
                onSelect = onLocalityOverride,
                allowInherit = true,
                label = "Enforced Locality Override",
                inheritLabel = "Inherit (${tcrLocalityShortLabel(effectiveLocality)})",
            )

            TcrModelSelectionField(
                label = "Run Model Override",
                value = config.runModelOverride,
                onValueChange = onRunModelOverride,
                placeholder = if (effectiveModelId.isNotBlank()) {
                    "Project default ($effectiveModelId)"
                } else {
                    "Project default"
                },
                availableModels = availableLlmModels,
                defaultChipLabel = if (availableLlmModels.isNotEmpty() || config.runModelOverride.isNotBlank()) {
                    "Project default"
                } else {
                    null
                },
                accentColor = TcrColors.ElectricBlue,
            )
        }
    }
}
