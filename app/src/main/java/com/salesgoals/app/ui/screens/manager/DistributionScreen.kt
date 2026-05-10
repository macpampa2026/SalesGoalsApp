package com.salesgoals.app.ui.screens.manager

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salesgoals.app.data.models.VariableType
import com.salesgoals.app.utils.ExportFormat
import com.salesgoals.app.utils.ExportImportHelper
import com.salesgoals.app.utils.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributionScreen(
    onBack: () -> Unit,
    viewModel: ManagerViewModel = viewModel(factory = ManagerViewModel.Factory)
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isExporting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Distribución") },
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SummaryCard(
                    title = "Presupuesto Total",
                    workingDays = state.workingDays,
                    advisorCount = state.advisorCount
                ) {
                    VariableType.values().forEach { v ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(v.displayName, modifier = Modifier.weight(1f))
                            Text(
                                Formatters.formatValue(v.valueOf(state.totalGoals), v.isCurrency),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            item {
                SummaryCard(
                    title = "Por Asesor (mes)",
                    workingDays = state.workingDays,
                    advisorCount = state.advisorCount
                ) {
                    VariableType.values().forEach { v ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(v.displayName, modifier = Modifier.weight(1f))
                            Text(
                                Formatters.formatValue(v.valueOf(state.perAdvisor), v.isCurrency),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            item {
                SummaryCard(
                    title = "Por Asesor (día)",
                    workingDays = state.workingDays,
                    advisorCount = state.advisorCount
                ) {
                    VariableType.values().forEach { v ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(v.displayName, modifier = Modifier.weight(1f))
                            Text(
                                Formatters.formatValue(v.valueOf(state.perDayPerAdvisor), v.isCurrency),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item { Text("Asesores", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }

            itemsIndexed(state.advisors, key = { idx, _ -> idx }) { index, name ->
                AdvisorRow(
                    index = index,
                    name = name,
                    enabled = !isExporting,
                    onNameChange = { viewModel.updateAdvisorName(index, it) },
                    onShare = {
                        if (isExporting) return@AdvisorRow
                        isExporting = true
                        try {
                            val payload = viewModel.buildPayloadFor(name.ifBlank { "Asesor ${index + 1}" })
                            val uri = ExportImportHelper.exportAdvisorPayload(context, payload, ExportFormat.JSON)
                            context.startActivity(Intent.createChooser(ExportImportHelper.shareIntent(uri), "Compartir presupuesto"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isExporting = false
                        }
                    },
                    onWhatsapp = {
                        if (isExporting) return@AdvisorRow
                        isExporting = true
                        try {
                            val payload = viewModel.buildPayloadFor(name.ifBlank { "Asesor ${index + 1}" })
                            val uri = ExportImportHelper.exportAdvisorPayload(context, payload, ExportFormat.JSON)
                            context.startActivity(ExportImportHelper.whatsappIntent(uri))
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp no disponible", Toast.LENGTH_LONG).show()
                        } finally {
                            isExporting = false
                        }
                    },
                    onEmail = {
                        if (isExporting) return@AdvisorRow
                        isExporting = true
                        try {
                            val payload = viewModel.buildPayloadFor(name.ifBlank { "Asesor ${index + 1}" })
                            val uri = ExportImportHelper.exportAdvisorPayload(context, payload, ExportFormat.JSON)
                            context.startActivity(Intent.createChooser(ExportImportHelper.emailIntent(uri), "Enviar email"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Email no disponible", Toast.LENGTH_LONG).show()
                        } finally {
                            isExporting = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    workingDays: Int,
    advisorCount: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                "$advisorCount asesores · $workingDays días",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun AdvisorRow(
    index: Int,
    name: String,
    enabled: Boolean = true,
    onNameChange: (String) -> Unit,
    onShare: () -> Unit,
    onWhatsapp: () -> Unit,
    onEmail: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { onNameChange(it.replace("\n", "")) },
                label = { Text("Asesor #${index + 1}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(enabled = enabled, onClick = onShare, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compartir")
                }
                FilledTonalButton(enabled = enabled, onClick = onWhatsapp, modifier = Modifier.weight(1f)) {
                    Text("WhatsApp")
                }
                FilledTonalButton(enabled = enabled, onClick = onEmail, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Email, contentDescription = null)
                }
            }
        }
    }
}
