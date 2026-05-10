package com.salesgoals.app.ui.screens.advisor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salesgoals.app.data.entities.DailyEntryEntity
import com.salesgoals.app.data.models.VariableType
import com.salesgoals.app.ui.components.SectionHeader
import com.salesgoals.app.ui.components.VariableInput
import com.salesgoals.app.utils.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyEntryScreen(
    date: String? = null,
    onBack: () -> Unit,
    viewModel: AdvisorViewModel = viewModel(factory = AdvisorViewModel.Factory)
) {
    val targetDate = date ?: Formatters.today()

    var volume by rememberSaveable { mutableStateOf("") }
    var credit by rememberSaveable { mutableStateOf("") }
    var warranty by rememberSaveable { mutableStateOf("") }
    var cashCredit by rememberSaveable { mutableStateOf("") }
    var phones by rememberSaveable { mutableStateOf("") }

    var tVolume by rememberSaveable { mutableStateOf("") }
    var tCredit by rememberSaveable { mutableStateOf("") }
    var tWarranty by rememberSaveable { mutableStateOf("") }
    var tCashCredit by rememberSaveable { mutableStateOf("") }
    var tPhones by rememberSaveable { mutableStateOf("") }

    var note by rememberSaveable { mutableStateOf("") }
    var hydratedDate by rememberSaveable { mutableStateOf("") }
    // NO rememberSaveable: rotación durante save dejaría el flag atascado.
    var isSaving by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(targetDate) {
        if (hydratedDate == targetDate) return@LaunchedEffect
        hydratedDate = targetDate
        val existing = viewModel.getEntry(targetDate)
        if (existing != null) {
            volume = safeLongString(existing.volume)
            credit = safeLongString(existing.credit)
            warranty = safeLongString(existing.warranty)
            cashCredit = safeLongString(existing.cashCredit)
            phones = safeLongString(existing.phones)

            tVolume = safeLongString(existing.targetVolume)
            tCredit = safeLongString(existing.targetCredit)
            tWarranty = safeLongString(existing.targetWarranty)
            tCashCredit = safeLongString(existing.targetCashCredit)
            tPhones = safeLongString(existing.targetPhones)

            note = existing.note
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cargar ${Formatters.friendlyDate(targetDate)}") },
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
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(title = "Ventas del día", subtitle = "Cargá los resultados reales")
                    VariableInput(VariableType.VOLUME, volume) { volume = it }
                    VariableInput(VariableType.CREDIT, credit) { credit = it }
                    VariableInput(VariableType.WARRANTY, warranty) { warranty = it }
                    VariableInput(VariableType.CASH_CREDIT, cashCredit) { cashCredit = it }
                    VariableInput(VariableType.PHONES, phones) { phones = it }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(
                        title = "Objetivo del día (opcional)",
                        subtitle = "Si lo dejás vacío, se usa el objetivo mensual / días laborales"
                    )
                    VariableInput(VariableType.VOLUME, tVolume, label = "Objetivo Volumen") { tVolume = it }
                    VariableInput(VariableType.CREDIT, tCredit, label = "Objetivo Crédito") { tCredit = it }
                    VariableInput(VariableType.WARRANTY, tWarranty, label = "Objetivo Garantía") { tWarranty = it }
                    VariableInput(VariableType.CASH_CREDIT, tCashCredit, label = "Objetivo Crédito Efectivo") { tCashCredit = it }
                    VariableInput(VariableType.PHONES, tPhones, label = "Objetivo Celulares") { tPhones = it }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Nota (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                enabled = !isSaving,
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    scope.launch {
                        try {
                            val entry = DailyEntryEntity(
                                date = targetDate,
                                volume = Formatters.toDouble(volume).coerceAtLeast(0.0),
                                credit = Formatters.toDouble(credit).coerceAtLeast(0.0),
                                warranty = Formatters.toDouble(warranty).coerceAtLeast(0.0),
                                cashCredit = Formatters.toDouble(cashCredit).coerceAtLeast(0.0),
                                phones = Formatters.toDouble(phones).coerceAtLeast(0.0),
                                targetVolume = Formatters.toDouble(tVolume).coerceAtLeast(0.0),
                                targetCredit = Formatters.toDouble(tCredit).coerceAtLeast(0.0),
                                targetWarranty = Formatters.toDouble(tWarranty).coerceAtLeast(0.0),
                                targetCashCredit = Formatters.toDouble(tCashCredit).coerceAtLeast(0.0),
                                targetPhones = Formatters.toDouble(tPhones).coerceAtLeast(0.0),
                                note = note.take(500)
                            )
                            viewModel.saveDailyEntry(entry)
                            snackbar.showSnackbar("Guardado correctamente")
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar")
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
