package dev.aidos.tcr.pill

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import dev.aidos.tcr.actor.GlyphType

/**
 * Atomic TCR component: Renders a clean micro glyph inside an Actor Pill.
 */
@Composable
fun ActionGlyph(
    glyphType: GlyphType,
    tint: Color,
    modifier: Modifier = Modifier
) {
    when (glyphType) {
        GlyphType.MIC -> {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Sanelu",
                tint = tint,
                modifier = modifier
            )
        }
        GlyphType.TEXT_CARET -> {
            Canvas(modifier = modifier) {
                val midX = size.width / 2f
                val topY = size.height * 0.15f
                val bottomY = size.height * 0.85f
                val capWidth = size.width * 0.35f

                // I-beam top bar
                drawLine(
                    color = tint,
                    start = Offset(midX - capWidth, topY),
                    end = Offset(midX + capWidth, topY),
                    strokeWidth = 1.25f,
                    cap = StrokeCap.Round
                )
                // I-beam vertical stem
                drawLine(
                    color = tint,
                    start = Offset(midX, topY),
                    end = Offset(midX, bottomY),
                    strokeWidth = 1.25f,
                    cap = StrokeCap.Round
                )
                // I-beam bottom bar
                drawLine(
                    color = tint,
                    start = Offset(midX - capWidth, bottomY),
                    end = Offset(midX + capWidth, bottomY),
                    strokeWidth = 1.25f,
                    cap = StrokeCap.Round
                )
            }
        }
        GlyphType.SPARK -> {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Tekoäly",
                tint = tint,
                modifier = modifier
            )
        }
        GlyphType.DELETE -> {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Poisto",
                tint = tint,
                modifier = modifier
            )
        }
        GlyphType.SCISSORS -> {
            Icon(
                imageVector = Icons.Default.ContentCut,
                contentDescription = "Leikkaus",
                tint = tint,
                modifier = modifier
            )
        }
        GlyphType.SELECTION -> {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Valinta",
                tint = tint,
                modifier = modifier
            )
        }
        GlyphType.CHECK -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Valmis",
                tint = tint,
                modifier = modifier
            )
        }
        GlyphType.CLOCK -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Odottaa",
                tint = tint,
                modifier = modifier
            )
        }
        GlyphType.QUESTION -> {
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = "Kysymys",
                tint = tint,
                modifier = modifier
            )
        }
    }
}
