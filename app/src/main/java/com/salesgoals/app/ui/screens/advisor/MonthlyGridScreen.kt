package com.salesgoals.app.ui.screens.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salesgoals.app.data.entities.DailyEntryEntity
import com.salesgoals.app.data.models.VariableSet
import com.salesgoals.app.data.models.VariableType
import com.salesgoals.app.data.repository.toGoals
import com.salesgoals.app.data.repository.toSet
import com.salesgoals.app.data.repository.toTargetsOrNull
import com.salesgoals.app.ui.theme.DangerRed
import com.salesgoals.app.ui.theme.SuccessGreen
import com.salesgoals.app.ui.theme.WarningYellow
import com.salesgoals.app.utils.Formatters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class DayCell(
    val date: String,        // YYYY-MM-DD
    val day: Int,            // 1..31
    val weekday: String,     // "Lun", "Mar", ...
    val target: VariableSet, // efectivo (ya sea custom del día o default mensual/días)
    val sales: VariableSet,
    val isCustomTarget: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyGridScreen(
    onBack: () -> Unit,
    onEditDay: (String) -> Unit,
    viewModel: AdvisorViewModel = viewModel(factory = AdvisorViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val period = state.budget?.period?.ifBlank { Formatters.currentPeriod() }
        ?: Formatters.currentPeriod()
    val workingDays = state.workingDays
    val monthlyGoals = state.budget?.toGoals() ?: VariableSet.ZERO
    val defaultDaily = if (workingDays > 0) monthlyGoals / workingDays else VariableSet.ZERO

    val cells = remember(state.entries, period, workingDays, monthlyGoals) {
        buildDayCells(period, state.entries, defaultDaily)
    }

    val totalTarget = cells.fold(VariableSet.ZERO) { acc, c -> acc + c.target }
    val totalSales = cells.fold(VariableSet.ZERO) { acc, c -> acc + c.sales }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grilla mensual · $period") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (state.budget == null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "ℹ️ No hay presupuesto cargado",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Podés cargar ventas igualmente, pero los objetivos diarios automáticos serán 0. Andá al Dashboard y cargá tu presupuesto para tener metas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            item {
                SummaryCard(totalTarget = totalTarget, totalSales = totalSales)
            }
            item {
                Text(
                    "Tocá un día para editar objetivo y ventas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                )
            }
            items(cells, key = { it.date }) { cell ->
                DayRow(cell = cell, onClick = { onEditDay(cell.date) })
            }
        }
    }
}

@Composable
private fun SummaryCard(totalTarget: VariableSet, totalSales: VariableSet) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Resumen acumulado del mes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            VariableType.values().forEach { v ->
                val target = v.valueOf(totalTarget)
                val sales = v.valueOf(totalSales)
                val pct = if (target > 0) (sales / target * 100.0) else 0.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        v.displayName,
                        modifier = Modifier.weight(0.35f),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        "${Formatters.formatValue(sales, v.isCurrency)} / ${Formatters.formatValue(target, v.isCurrency)}",
                        modifier = Modifier.weight(0.5f),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        Formatters.percent(pct),
                        modifier = Modifier.weight(0.15f),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DayRow(cell: DayCell, onClick: () -> Unit) {
    val volTarget = cell.target.volume
    val volSales = cell.sales.volume
    val pct = if (volTarget > 0) (volSales / volTarget).coerceIn(0.0, 2.0) else 0.0
    val color = when {
        pct >= 0.95 -> SuccessGreen
        pct >= 0.80 -> WarningYellow
        pct > 0.0 -> DangerRed
        else -> MaterialTheme.colorScheme.outline
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Día y semana a la izquierda
            Column(
                modifier = Modifier.width(60.dp)
            ) {
                Text(
                    cell.day.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    cell.weekday,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Datos centro
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Vol: ${Formatters.money(volSales)} / ${Formatters.money(volTarget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Cel: ${safeLong(cell.sales.phones)} / ${safeLong(cell.target.phones)}  ·  Créd: ${Formatters.money(cell.sales.credit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = {
                        val raw = pct.toFloat()
                        if (raw.isFinite()) raw.coerceIn(0f, 1f) else 0f
                    },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f)
                )
                if (cell.isCustomTarget) {
                    Text(
                        "★ Objetivo personalizado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Indicador semáforo
            Box(
                modifier = Modifier.size(12.dp).clip(RoundedCornerShape(50)).background(color)
            )
        }
    }
}

private fun buildDayCells(
    period: String,
    entries: List<DailyEntryEntity>,
    defaultDaily: VariableSet
): List<DayCell> {
    if (!Formatters.isValidPeriod(period)) return emptyList()
    val parts = period.split("-")
    val year = parts[0].toIntOrNull() ?: return emptyList()
    val month = parts[1].toIntOrNull() ?: return emptyList()

    val cal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val byDate = entries.associateBy { it.date }
    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val sdfWeekday = SimpleDateFormat("EEE", Locale("es", "AR"))

    return (1..totalDays).map { day ->
        cal.set(Calendar.DAY_OF_MONTH, day)
        val date = sdfDate.format(cal.time)
        val weekday = sdfWeekday.format(cal.time).replaceFirstChar { it.uppercase() }
            .removeSuffix(".")
        val entry = byDate[date]
        val customTarget = entry?.toTargetsOrNull()
        val target = customTarget ?: defaultDaily
        val sales = entry?.toSet() ?: VariableSet.ZERO
        DayCell(
            date = date,
            day = day,
            weekday = weekday,
            target = target,
            sales = sales,
            isCustomTarget = customTarget != null
        )
    }
}

/** Convierte Double a Long con safety contra NaN/Infinity/overflow. */
private fun safeLong(value: Double): Long {
    if (!value.isFinite()) return 0L
    return value.coerceAtMost(Long.MAX_VALUE.toDouble()).coerceAtLeast(0.0).toLong()
}
