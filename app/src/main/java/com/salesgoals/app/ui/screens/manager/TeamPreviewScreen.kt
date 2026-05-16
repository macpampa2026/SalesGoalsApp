package com.salesgoals.app.ui.screens.manager

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salesgoals.app.data.models.PerformanceStatus
import com.salesgoals.app.data.models.VariableSet
import com.salesgoals.app.ui.theme.DangerRed
import com.salesgoals.app.ui.theme.GradientBottom
import com.salesgoals.app.ui.theme.GradientTop
import com.salesgoals.app.ui.theme.SuccessGreen
import com.salesgoals.app.ui.theme.WarningYellow
import com.salesgoals.app.utils.Formatters

/**
 * Vista de equipo: simula cómo se vería el multi-usuario con varios asesores
 * sincronizando datos en tiempo real a la Gerencia. La data es DEMO/MOCK pero
 * usa el presupuesto real cargado para que sea creíble.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamPreviewScreen(
    onBack: () -> Unit,
    viewModel: ManagerViewModel = viewModel(factory = ManagerViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val perAdvisor = state.perAdvisor
    val count = state.advisors.size.coerceAtLeast(1)
    val names = state.advisors

    // Simulamos % de cumplimiento por asesor con valores realistas
    // (algunos en línea, otros en riesgo, alguno atrasado)
    val mockProgress = remember(count) {
        listOf(1.08, 0.96, 0.84, 0.72, 0.91, 1.02, 0.65, 0.78, 0.99, 1.15)
            .take(count.coerceAtMost(10))
            .let { base ->
                if (base.size < count) base + List(count - base.size) { 0.85 } else base
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vista de equipo") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(GradientTop, GradientBottom)))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Banner explicativo
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Vista previa",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Así se verá la sincronización en tiempo real cuando se libere la versión Business. Los datos son demo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                item {
                    // Resumen de la sucursal
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                state.branchName.ifBlank { "Sucursal" },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${state.advisorCount} asesores · ${state.period}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Promedio de cumplimiento
                            val avg = mockProgress.average()
                            Text(
                                "Promedio del equipo: ${Formatters.percent(avg * 100)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                item {
                    Text(
                        "Asesores",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                    )
                }

                items(names.zip(mockProgress)) { (name, ratio) ->
                    AdvisorTile(name = name, ratio = ratio, perAdvisor = perAdvisor)
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "★ Esta vista es de demostración. La versión Business permitirá ver datos reales actualizados al instante desde los celulares del equipo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvisorTile(name: String, ratio: Double, perAdvisor: VariableSet) {
    val status = when {
        ratio >= 0.95 -> PerformanceStatus.ON_TRACK
        ratio >= 0.80 -> PerformanceStatus.WARNING
        else -> PerformanceStatus.BEHIND
    }
    val color = when (status) {
        PerformanceStatus.ON_TRACK -> SuccessGreen
        PerformanceStatus.WARNING -> WarningYellow
        PerformanceStatus.BEHIND -> DangerRed
    }
    val statusLabel = when (status) {
        PerformanceStatus.ON_TRACK -> "En línea"
        PerformanceStatus.WARNING -> "Riesgo"
        PerformanceStatus.BEHIND -> "Atrasado"
    }
    val accumulatedVolume = perAdvisor.volume * ratio

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = color)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Volumen: ${Formatters.money(accumulatedVolume)} / ${Formatters.money(perAdvisor.volume)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        Formatters.percent(ratio * 100),
                        style = MaterialTheme.typography.titleLarge,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = {
                    val r = ratio.toFloat()
                    if (r.isFinite()) r.coerceIn(0f, 1f) else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )
        }
    }
}

