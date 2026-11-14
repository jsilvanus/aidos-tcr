package dev.aidos.tcr.atom.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.aidos.tcr.theme.CornerTokens
import dev.aidos.tcr.theme.SpacingTokens
import dev.aidos.tcr.theme.TcrTypography

@Composable
fun InlineCardMolecule(
    title: String,
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0x3394A3B8),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), shape = RoundedCornerShape(CornerTokens.md))
            .border(1.dp, borderColor, shape = RoundedCornerShape(CornerTokens.md))
            .padding(SpacingTokens.base)
    ) {
        Column {
            Text(
                text = title,
                style = TcrTypography.baseTypography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = SpacingTokens.sm),
            )
            content()
        }
    }
}
