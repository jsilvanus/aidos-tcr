package dev.aidos.tcr.engine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Organism TCR Component: SettingsContent
 *
 * Engine Settings content (RFC-0103, Phase D):
 * - Hugging Face API token management (input, validate, clear, encrypted storage disclaimer)
 * - RAM baseline configuration (calibrating fit verdicts against steady-state device RAM)
 * - Model suggestions restoration
 */
@Composable
fun SettingsContent(
    state: SettingsState,
    dismissedSuggestionCount: Int,
    onStateChange: (SettingsState) -> Unit,
    onSaveToken: (String) -> Unit,
    onClearToken: () -> Unit,
    onRestoreSuggestions: () -> Unit,
    tokenStorageNote: String,
    modifier: Modifier = Modifier,
    ramBaseline: RamBaselineState? = null,
    onSetRamBaselineNow: (() -> Unit)? = null,
    onClearRamBaseline: (() -> Unit)? = null,
    engineCacheDir: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.successMessage?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp)
            }
        }

        state.errorMessage?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
            }
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hugging Face Token", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    "Optional. Used when downloading gated models (e.g. Llama 3) from the Hub.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (state.hfTokenStatus.isConfigured) {
                    Text(
                        "Configured",
                        color = Color(0xFF22C55E),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                    OutlinedButton(onClick = onClearToken, enabled = !state.isLoading) {
                        Text("Clear Token", color = Color(0xFFEF4444))
                    }
                } else if (!state.showTokenInput) {
                    Button(
                        onClick = { onStateChange(state.copy(showTokenInput = true, tokenInput = "")) },
                        enabled = !state.isLoading,
                    ) {
                        Text("Add Token")
                    }
                }

                if (state.showTokenInput) {
                    OutlinedTextField(
                        value = state.tokenInput,
                        onValueChange = { onStateChange(state.copy(tokenInput = it)) },
                        label = { Text("Token (hf_...)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSaveToken(state.tokenInput) },
                            enabled = !state.isLoading && state.tokenInput.isNotBlank(),
                        ) {
                            Text("Save")
                        }
                        TextButton(
                            onClick = { onStateChange(state.copy(showTokenInput = false, tokenInput = "")) },
                            enabled = !state.isLoading,
                        ) {
                            Text("Cancel")
                        }
                    }
                }

                Text(tokenStorageNote, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (ramBaseline != null && onSetRamBaselineNow != null && onClearRamBaseline != null) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("RAM Baseline for Fit Scoring", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        "Model fit verdicts are scored against free RAM. Setting a baseline lets the Engine judge models against your usual system load instead of whatever happens to be free right now.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val baseline = ramBaseline.baselineUsedMB
                    if (baseline != null) {
                        Text(
                            "Baseline in use: $baseline MB (${formatMb(baseline)}) of ${ramBaseline.totalMB} MB total · ${ramBaseline.baselineFreeMB} MB free at baseline",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text("No baseline set (scoring uses current free RAM).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Currently in use: ${ramBaseline.currentUsedMB} MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSetRamBaselineNow) {
                            Text(if (baseline == null) "Set baseline to current load" else "Update baseline to current load")
                        }
                        if (baseline != null) {
                            OutlinedButton(onClick = onClearRamBaseline) { Text("Clear baseline") }
                        }
                    }
                }
            }
        }

        if (dismissedSuggestionCount > 0) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Model Suggestions", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        "$dismissedSuggestionCount suggested model${if (dismissedSuggestionCount == 1) " has" else "s have"} been removed from the Cookbook tab.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onRestoreSuggestions) {
                        Text("Restore suggestions")
                    }
                }
            }
        }

        engineCacheDir?.let { dir ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Cache Directory", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(dir, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
