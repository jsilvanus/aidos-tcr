package dev.aidos.tcr.atom.core

import androidx.compose.foundation.background
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
fun MetricBadgeAtom(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF06B6D4),
) {
    Text(
        text = "$label: $value",
        style = TcrTypography.baseTypography.labelSmall,
        color = accentColor,
        modifier = modifier
            .background(accentColor.copy(alpha = 0.15f), shape = RoundedCornerShape(CornerTokens.xs))
            .padding(horizontal = SpacingTokens.sm, vertical = 2.dp),
    )
}
