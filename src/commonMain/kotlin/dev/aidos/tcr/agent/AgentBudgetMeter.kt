package dev.aidos.tcr.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
 * Atomic TCR Component: AgentBudgetMeter (RFC-0028 & RFC-0011)
 *
 * Visualizes capability budget allocation and quota consumption:
 * - Reasoning steps spent vs ceiling
 * - Model calls and token bounds
 * - Budget delegation splits (fan-out protection)
 */
@Composable
fun AgentBudgetMeter(
    budget: AgentBudgetUi,
    currentSteps: Int = 0,
    modifier: Modifier = Modifier
) {
    val maxSteps = budget.maxSteps ?: budget.steps ?: 8
    val stepsFraction = if (maxSteps > 0) (currentSteps.toFloat() / maxSteps).coerceIn(0f, 1f) else 0f

    val progressColor = when {
        stepsFraction >= 0.9f -> TcrColors.EmberRed
        stepsFraction >= 0.7f -> TcrColors.AmberWarning
        else -> TcrColors.EmeraldGreen
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "📊", fontSize = 12.sp)
                Text(
                    text = "Delegated Budget & Spend Ceiling",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
            }

            if (budget.splitWays != null && budget.splitWays > 1) {
                Box(
                    modifier = Modifier
                        .background(TcrColors.ElectricBlue.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .border(1.dp, TcrColors.ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Split ${budget.splitWays} ways",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TcrColors.ElectricBlue
                    )
                }
            }
        }

        // Steps Progress Bar
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Reasoning Steps",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Text(
                    text = "$currentSteps / $maxSteps steps (${(stepsFraction * 100).toInt()}%)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
            }

            LinearProgressIndicator(
                progress = { stepsFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = progressColor,
                trackColor = Color.LightGray.copy(alpha = 0.3f),
            )
        }

        // Quota chips (Model calls, Tokens, Cost)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            budget.modelCalls?.let { calls ->
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Max Calls: $calls",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray
                    )
                }
            }

            budget.inputTokens?.let { tokens ->
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Tokens: ${tokens / 1000}k cap",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray
                    )
                }
            }

            budget.costUnits?.let { cost ->
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Cost Cap: $cost units",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}
