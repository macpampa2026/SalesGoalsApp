package com.salesgoals.app.ui.screens.advisor

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salesgoals.app.ui.components.SectionHeader
import com.salesgoals.app.ui.theme.GradientBottom
import com.salesgoals.app.ui.theme.GradientTop
import com.salesgoals.app.utils.ExportImportHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: AdvisorViewModel = viewModel(factory = AdvisorViewModel.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isWorking by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val pickBackup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Restaurar respaldo") },
            text = { Text("Se sobrescribirán tu presupuesto actual y todas las cargas diarias con los datos del respaldo. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    val target = uri
                    pendingRestoreUri = null
                    if (isWorking) return@TextButton
                    isWorking = true
                    scope.launch {
                        try {
                            when (val imported = ExportImportHelper.importAny(context, target)) {
                                is ExportImportHelper.ImportedFile.FullBackup -> {
                                    viewModel.restoreFullBackup(imported.payload)
                                    Toast.makeText(context, "Respaldo restaurado", Toast.LENGTH_SHORT).show()
                                }
                                is ExportImportHelper.ImportedFile.Budget -> {
                                    viewModel.applyImportedPayload(imported.payload)
                                    Toast.makeText(context, "Presupuesto restaurado (el archivo no incluía cargas diarias)", Toast.LENGTH_LONG).show()
                                }
                            }
                            onBack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al restaurar: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isWorking = false
                        }
                    }
                }) {
                    Text("Restaurar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Respaldo de datos") },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = "Antes de actualizar la app",
                    subtitle = "Exportá un respaldo completo para no perder tus datos. Después podés restaurarlo en la nueva versión."
                )

                // ----- Exportar -----
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconHeader(icon = Icons.Default.CloudUpload, title = "Exportar respaldo completo")
                        Text(
                            "Genera un archivo JSON con tu presupuesto y todas las cargas diarias. Mandátelo por WhatsApp/email para no perderlo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            enabled = !isWorking,
                            onClick = {
                                if (isWorking) return@Button
                                isWorking = true
                                scope.launch {
                                    try {
                                        val backup = viewModel.buildFullBackup()
                                        val uri = ExportImportHelper.exportFullBackup(context, backup)
                                        context.startActivity(
                                            Intent.createChooser(
                                                ExportImportHelper.shareIntent(
                                                    uri,
                                                    subject = "Respaldo completo - Carrera 152",
                                                    text = "Adjunto el respaldo de mis datos."
                                                ),
                                                "Compartir respaldo"
                                            )
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isWorking = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar y compartir")
                        }
                    }
                }

                // ----- Restaurar -----
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconHeader(icon = Icons.Default.CloudDownload, title = "Restaurar desde un respaldo")
                        Text(
                            "Elegí un archivo de respaldo previamente exportado. Va a sobrescribir lo que tengas cargado ahora.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FilledTonalButton(
                            enabled = !isWorking,
                            onClick = {
                                pickBackup.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Elegir archivo")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "★ A partir de esta versión (1.1.0) las futuras actualizaciones se pueden instalar SIN desinstalar la app, manteniendo tus datos automáticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun IconHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}
