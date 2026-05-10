package com.salesgoals.app.ui.screens.advisor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salesgoals.app.data.models.VariableSet
import com.salesgoals.app.data.models.VariableType
import com.salesgoals.app.ui.components.SectionHeader
import com.salesgoals.app.ui.components.VariableInput
import com.salesgoals.app.utils.ExportImportHelper
import com.salesgoals.app.utils.Formatters
import com.salesgoals.app.utils.PendingImport
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBudgetScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AdvisorViewModel = viewModel(factory = AdvisorViewModel.Factory)
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(Formatters.currentPeriod()) }
    var workingDays by rememberSaveable { mutableStateOf("22") }
    var volume by rememberSaveable { mutableStateOf("") }
    var credit by rememberSaveable { mutableStateOf("") }
    var warranty by rememberSaveable { mutableStateOf("") }
    var cashCredit by rememberSaveable { mutableStateOf("") }
    var phones by rememberSaveable { mutableStateOf("") }

    var lastHydratedSig by rememberSaveable { mutableStateOf("") }
    // NO rememberSaveable: rotación durante save dejaría el flag atascado.
    var isSaving by remember { mutableStateOf(false) }
    val budgetSignature = state.budget?.let { "${it.id}-${it.updatedAt}" } ?: "EMPTY"
    LaunchedEffect(state.isLoaded, budgetSignature) {
        if (!state.isLoaded) return@LaunchedEffect
        val wasEmpty = lastHydratedSig == "EMPTY"
        val isEmpty = budgetSignature == "EMPTY"
        if (lastHydratedSig.isNotEmpty() && wasEmpty == isEmpty) return@LaunchedEffect

        val b = state.budget
        if (b == null) {
            name = ""
            period = Formatters.currentPeriod()
            workingDays = "22"
            volume = ""; credit = ""; warranty = ""; cashCredit = ""; phones = ""
        } else {
            name = b.ownerName
            period = Formatters.safePeriod(b.period)
            workingDays = b.workingDays.toString()
            volume = safeLongString(b.goalVolume)
            credit = safeLongString(b.goalCredit)
            warranty = safeLongString(b.goalWarranty)
            cashCredit = safeLongString(b.goalCashCredit)
            phones = safeLongString(b.goalPhones)
        }
        lastHydratedSig = budgetSignature
    }

    val pickFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val payload = ExportImportHelper.importPayload(context, uri)
                    viewModel.applyImportedPayload(payload)
                    Toast.makeText(context, "Presupuesto importado correctamente", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al leer el archivo: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Observamos el URI pendiente continuamente. Si llega una nueva URI mientras
    // ya estamos en esta pantalla, también se importa.
    val pendingUriObserved by PendingImport.uri.collectAsStateWithLifecycle()
    LaunchedEffect(pendingUriObserved) {
        val pending = pendingUriObserved ?: return@LaunchedEffect
        // Consumimos atómicamente para que la nav no re-dispare.
        PendingImport.consume()
        try {
            val payload = ExportImportHelper.importPayload(context, pending)
            viewModel.applyImportedPayload(payload)
            Toast.makeText(context, "Presupuesto importado desde el archivo recibido", Toast.LENGTH_SHORT).show()
            onSuccess()
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo importar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar / Cargar Presupuesto") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(
                title = "Importar archivo",
                subtitle = "Seleccioná el JSON o CSV enviado por tu Gerencia"
            )
            Button(
                onClick = { pickFile.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "*/*")) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Elegir archivo")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            SectionHeader(title = "O cargar manualmente", subtitle = "Ingresá tus objetivos y días")

            OutlinedTextField(
                value = name, onValueChange = { name = it.replace("\n", "") },
                label = { Text("Tu nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = period, onValueChange = { period = it.replace("\n", "") },
                label = { Text("Periodo (YYYY-MM)") },
                singleLine = true,
                isError = period.isNotBlank() && !Formatters.isValidPeriod(period),
                supportingText = {
                    if (period.isNotBlank() && !Formatters.isValidPeriod(period)) {
                        Text("Formato inválido — usá YYYY-MM (ej. 2026-05)")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = workingDays,
                onValueChange = { v -> workingDays = v.filter { c -> c.isDigit() }.take(2) },
                label = { Text("Días laborales") },
                modifier = Modifier.fillMaxWidth()
            )
            VariableInput(VariableType.VOLUME, volume) { volume = it }
            VariableInput(VariableType.CREDIT, credit) { credit = it }
            VariableInput(VariableType.WARRANTY, warranty) { warranty = it }
            VariableInput(VariableType.CASH_CREDIT, cashCredit) { cashCredit = it }
            VariableInput(VariableType.PHONES, phones) { phones = it }

            FilledTonalButton(
                enabled = !isSaving,
                onClick = {
                    if (isSaving) return@FilledTonalButton
                    isSaving = true
                    scope.launch {
                        try {
                            val days = workingDays.toIntOrNull() ?: 22
                            val goals = VariableSet(
                                volume = Formatters.toDouble(volume).coerceAtLeast(0.0),
                                credit = Formatters.toDouble(credit).coerceAtLeast(0.0),
                                warranty = Formatters.toDouble(warranty).coerceAtLeast(0.0),
                                cashCredit = Formatters.toDouble(cashCredit).coerceAtLeast(0.0),
                                phones = Formatters.toDouble(phones).coerceAtLeast(0.0)
                            )
                            viewModel.saveManualBudget(name.trim(), period.trim(), days, goals)
                            Toast.makeText(context, "Presupuesto guardado", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar manualmente")
            }
        }
    }
}

/** Convierte un Double a string entero, manejando NaN/Infinity. */
private fun safeLongString(value: Double): String {
    if (!value.isFinite() || value <= 0.0) return ""
    val capped = value.coerceAtMost(Long.MAX_VALUE.toDouble())
    return capped.toLong().toString()
}
