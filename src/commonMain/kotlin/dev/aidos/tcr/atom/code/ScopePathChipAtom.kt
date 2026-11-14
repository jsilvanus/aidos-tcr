package dev.aidos.tcr.atom.code

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.CornerTokens

@Composable
fun ScopePathChipAtom(
    path: String,
    modifier: Modifier = Modifier,
    isExcluded: Boolean = false,
) {
    val bgColor = if (isExcluded) Color(0x33EF4444) else Color(0x3338BDF8)
    val textColor = if (isExcluded) Color(0xFFF87171) else Color(0xFF38BDF8)

    Text(
        text = path,
        color = textColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(CornerTokens.xs))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
