package dev.aidos.tcr.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.aidos.tcr.theme.AidosTypography
import dev.aidos.tcr.theme.ColorTokens
import dev.aidos.tcr.theme.CornerTokens
import dev.aidos.tcr.theme.SpacingTokens

data class CommandItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val shortcut: String? = null,
    val action: () -> Unit,
)

@Composable
fun CommandPaletteModalView(
    isOpen: Boolean,
    searchQuery: String,
    results: List<CommandItem>,
    selectedIndex: Int = 0,
    onQueryChange: (String) -> Unit,
    onSelectIndex: (Int) -> Unit,
    onExecuteItem: (CommandItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable { onClose() },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 80.dp)
                    .width(560.dp)
                    .background(Color(0xFF0F172A), shape = RoundedCornerShape(CornerTokens.md))
                    .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(CornerTokens.md))
                    .clickable(enabled = false) {} // Prevent backdrop dismissal on box click
                    .padding(SpacingTokens.base)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.md)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Type a command, goal, task, or file... (Cmd+K)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(SpacingTokens.xs),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        itemsIndexed(results) { index, item ->
                            val isSelected = index == selectedIndex
                            val bg = if (isSelected) Color(0x3306B6D4) else Color.Transparent

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bg, shape = RoundedCornerShape(CornerTokens.xs))
                                    .clickable {
                                        onExecuteItem(item)
                                        onClose()
                                    }
                                    .padding(SpacingTokens.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = item.title,
                                        style = AidosTypography.baseTypography.bodyMedium,
                                        color = if (isSelected) ColorTokens.ActorAgent else Color.White,
                                    )
                                    if (item.subtitle != null) {
                                        Text(
                                            text = item.subtitle,
                                            style = AidosTypography.baseTypography.labelSmall,
                                            color = Color(0xFF94A3B8),
                                        )
                                    }
                                }

                                if (item.shortcut != null) {
                                    Text(
                                        text = item.shortcut,
                                        style = AidosTypography.baseTypography.labelSmall,
                                        color = Color(0xFF64748B),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
