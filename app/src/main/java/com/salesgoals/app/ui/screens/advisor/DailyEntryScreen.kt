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

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(targetDate) {
        // Solo hidratamos una vez por fecha para no pisar inputs del usuario.
        if (hydratedDate == targetDate) return@LaunchedEffect
        hydratedDate = targetDate
        val existing = viewModel.repository.getEntry(targetDate)
        if (existing != null) {
            volume = if (existing.volume == 0.0) "" else existing.volume.toLong().toString()
            credit = if (existing.credit == 0.0) "" else existing.credit.toLong().toString()
            warranty = if (existing.warranty == 0.0) "" else existing.warranty.toLong().toString()
            cashCredit = if (existing.cashCredit == 0.0) "" else existing.cashCredit.toLong().toString()
            phones = if (existing.phones == 0.0) "" else existing.phones.toLong().toString()

            tVolume = if (existing.targetVolume == 0.0) "" else existing.targetVolume.toLong().toString()
            tCredit = if (existing.targetCredit == 0.0) "" else existing.targetCredit.toLong().toString()
            tWarranty = if (existing.targetWarranty == 0.0) "" else existing.targetWarranty.toLong().toString()
            tCashCredit = if (existing.targetCashCredit == 0.0) "" else existing.targetCashCredit.toLong().toString()
            tPhones = if (existing.targetPhones == 0.0) "" else existing.targetPhones.toLong().toString()

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
                onClick = {
                    scope.launch {
                        val entry = DailyEntryEntity(
                            date = targetDate,
                            volume = Formatters.toDouble(volume),
                            credit = Formatters.toDouble(credit),
                            warranty = Formatters.toDouble(warranty),
                            cashCredit = Formatters.toDouble(cashCredit),
                            phones = Formatters.toDouble(phones),
                            targetVolume = Formatters.toDouble(tVolume),
                            targetCredit = Formatters.toDouble(tCredit),
                            targetWarranty = Formatters.toDouble(tWarranty),
                            targetCashCredit = Formatters.toDouble(tCashCredit),
                            targetPhones = Formatters.toDouble(tPhones),
                            note = note
                        )
                        viewModel.saveDailyEntry(entry)
                        snackbar.showSnackbar("Guardado correctamente")
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
