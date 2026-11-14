package dev.aidos.tcr.atom.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.aidos.tcr.theme.ColorTokens

@Composable
fun AudioWaveformAtom(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = ColorTokens.PillOrchestratorRed,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(24.dp)) {
        if (amplitudes.isEmpty()) return@Canvas
        val barWidth = size.width / (amplitudes.size * 2)
        val centerY = size.height / 2f

        amplitudes.forEachIndexed { i, amp ->
            val clampedAmp = amp.coerceIn(0.1f, 1.0f)
            val barHeight = size.height * clampedAmp * 0.8f
            val x = i * barWidth * 2 + barWidth / 2
            drawLine(
                color = barColor,
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth,
            )
        }
    }
}
