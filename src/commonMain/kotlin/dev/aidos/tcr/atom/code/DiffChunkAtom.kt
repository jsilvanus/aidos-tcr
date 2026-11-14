package dev.aidos.tcr.atom.code

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.aidos.tcr.theme.SpacingTokens

enum class DiffLineKind {
    CONTEXT,
    ADDITION,
    DELETION,
}

data class DiffLine(
    val kind: DiffLineKind,
    val text: String,
)

@Composable
fun DiffChunkAtom(
    oldPath: String?,
    newPath: String?,
    lines: List<DiffLine>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), shape = RoundedCornerShape(CornerTokens.sm))
            .padding(SpacingTokens.sm)
    ) {
        val header = newPath ?: oldPath ?: "diff"
        Text(
            text = "--- $header",
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        lines.forEach { line ->
            val (bgColor, textColor, prefix) = when (line.kind) {
                DiffLineKind.ADDITION -> Triple(Color(0x2E10B981), Color(0xFF34D399), "+ ")
                DiffLineKind.DELETION -> Triple(Color(0x2EEF4444), Color(0xFFF87171), "- ")
                DiffLineKind.CONTEXT -> Triple(Color.Transparent, Color(0xFFE2E8F0), "  ")
            }

            Text(
                text = "$prefix${line.text}",
                color = textColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
fun DiffChunkAtom(
    header: String,
    lines: List<DiffLine>,
    modifier: Modifier = Modifier,
) {
    DiffChunkAtom(
        oldPath = null,
        newPath = header,
        lines = lines,
        modifier = modifier,
    )
}
