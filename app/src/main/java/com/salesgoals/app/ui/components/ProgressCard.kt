package com.salesgoals.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salesgoals.app.data.models.PerformanceStatus
import com.salesgoals.app.data.models.VariableProgress
import com.salesgoals.app.ui.theme.DangerRed
import com.salesgoals.app.ui.theme.DangerRedSoft
import com.salesgoals.app.ui.theme.SuccessGreen
import com.salesgoals.app.ui.theme.SuccessGreenSoft
import com.salesgoals.app.ui.theme.WarningYellow
import com.salesgoals.app.ui.theme.WarningYellowSoft
import com.salesgoals.app.utils.Formatters

private fun statusColor(status: PerformanceStatus): Color = when (status) {
    PerformanceStatus.ON_TRACK -> SuccessGreen
    PerformanceStatus.WARNING -> WarningYellow
    PerformanceStatus.BEHIND -> DangerRed
}

private fun statusBg(status: PerformanceStatus): Color = when (status) {
    PerformanceStatus.ON_TRACK -> SuccessGreenSoft
    PerformanceStatus.WARNING -> WarningYellowSoft
    PerformanceStatus.BEHIND -> DangerRedSoft
}

private fun statusLabel(status: PerformanceStatus): String = when (status) {
    PerformanceStatus.ON_TRACK -> "En línea"
    PerformanceStatus.WARNING -> "Riesgo"
    PerformanceStatus.BEHIND -> "Atrasado"
}

@Composable
fun ProgressCard(progress: VariableProgress, modifier: Modifier = Modifier) {
    val isCurrency = progress.type.isCurrency
    val color = statusColor(progress.status)
    val bg = statusBg(progress.status)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    progress.type.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(progress.status, color, bg)
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${Formatters.formatValue(progress.accumulated, isCurrency)} / ${Formatters.formatValue(progress.monthlyGoal, isCurrency)}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (progress.percent.toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = color,
                trackColor = bg
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricBlock("Restante", Formatters.formatValue(progress.remaining, isCurrency))
                MetricBlock("% Cumplido", Formatters.percent(progress.percent))
                MetricBlock("Diario nec.", Formatters.formatValue(progress.requiredPace, isCurrency))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricBlock("Diario base", Formatters.formatValue(progress.dailyGoal, isCurrency))
                MetricBlock("Ritmo actual", Formatters.formatValue(progress.currentPace, isCurrency))
                MetricBlock("Proyección", Formatters.formatValue(progress.projection, isCurrency))
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusChip(status: PerformanceStatus, color: Color, bg: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(statusLabel(status), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

