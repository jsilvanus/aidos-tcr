package dev.aidos.tcr.atom.code

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.CornerTokens

@Composable
fun StepCounterAtom(
    currentStep: Int,
    maxSteps: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Step $currentStep / $maxSteps",
        color = Color(0xFF94A3B8),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        modifier = modifier
            .background(Color(0xFF1E293B), shape = RoundedCornerShape(CornerTokens.xs))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun StepCounterAtom(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    useTotalAlias: Boolean = true,
) {
    StepCounterAtom(currentStep = currentStep, maxSteps = totalSteps, modifier = modifier)
}
