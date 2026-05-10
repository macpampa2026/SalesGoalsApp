package com.salesgoals.app.ui.screens.manager

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.salesgoals.app.utils.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: ManagerViewModel = viewModel(factory = ManagerViewModel.Factory)
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Inputs locales
    var branch by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(Formatters.currentPeriod()) }
    var days by rememberSaveable { mutableStateOf("22") }
    var advisors by rememberSaveable { mutableStateOf("1") }
    var volume by rememberSaveable { mutableStateOf("") }
    var credit by rememberSaveable { mutableStateOf("") }
    var warranty by rememberSaveable { mutableStateOf("") }
    var cashCredit by rememberSaveable { mutableStateOf("") }
    var phones by rememberSaveable { mutableStateOf("") }

    // Hidratamos los inputs SOLO la primera vez que aparece un budget
    // (o cuando se hace reset). No pisamos los inputs del usuario si Room emite
    // mientras está tipeando.
    var lastHydratedSig by rememberSaveable { mutableStateOf("") }
    // NO rememberSaveable: rotación durante save dejaría el flag atascado.
    var isSaving by remember { mutableStateOf(false) }
    val budgetSignature = state.budget?.let { "${it.id}-${it.updatedAt}" } ?: "EMPTY"
    LaunchedEffect(state.isLoaded, budgetSignature) {
        if (!state.isLoaded) return@LaunchedEffect
        // Solo hidrato si:
        // - Es la primera vez (lastHydratedSig vacío)
        // - O el budget pasó de null a no-null o viceversa (reset/load)
        val wasEmpty = lastHydratedSig == "EMPTY"
        val isEmpty = budgetSignature == "EMPTY"
        if (lastHydratedSig.isNotEmpty() && wasEmpty == isEmpty) return@LaunchedEffect

        val b = state.budget
        if (b == null) {
            branch = ""
            period = Formatters.currentPeriod()
            days = "22"
            advisors = "1"
            volume = ""; credit = ""; warranty = ""; cashCredit = ""; phones = ""
        } else {
            branch = b.branchName
            period = Formatters.safePeriod(b.period)
            days = b.workingDays.toString()
            advisors = b.advisorCount.toString()
            volume = safeLongString(b.goalVolume)
            credit = safeLongString(b.goalCredit)
            warranty = safeLongString(b.goalWarranty)
            cashCredit = safeLongString(b.goalCashCredit)
            phones = safeLongString(b.goalPhones)
        }
        lastHydratedSig = budgetSignature
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presupuesto Sucursal") },
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
            SectionHeader(title = "Datos de la sucursal", subtitle = "Configurá los parámetros generales")

            OutlinedTextField(
                value = branch, onValueChange = { branch = it.replace("\n", "") },
                label = { Text("Nombre de la sucursal") },
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = days,
                    onValueChange = { v -> days = v.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Días laborales (1-31)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = advisors,
                    onValueChange = { v -> advisors = v.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Cant. asesores (1-50)") },
                    modifier = Modifier.weight(1f)
                )
            }

            SectionHeader(title = "Presupuesto total", subtitle = "Total de la sucursal a distribuir")

            VariableInput(VariableType.VOLUME, volume) { volume = it }
            VariableInput(VariableType.CREDIT, credit) { credit = it }
            VariableInput(VariableType.WARRANTY, warranty) { warranty = it }
            VariableInput(VariableType.CASH_CREDIT, cashCredit) { cashCredit = it }
            VariableInput(VariableType.PHONES, phones) { phones = it }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                enabled = !isSaving,
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    scope.launch {
                        try {
                            val goals = VariableSet(
                                volume = Formatters.toDouble(volume).coerceAtLeast(0.0),
                                credit = Formatters.toDouble(credit).coerceAtLeast(0.0),
                                warranty = Formatters.toDouble(warranty).coerceAtLeast(0.0),
                                cashCredit = Formatters.toDouble(cashCredit).coerceAtLeast(0.0),
                                phones = Formatters.toDouble(phones).coerceAtLeast(0.0)
                            )
                            viewModel.saveBranchBudget(
                                branchName = branch.trim(),
                                period = period.trim(),
                                workingDays = days.toIntOrNull() ?: 22,
                                advisorCount = advisors.toIntOrNull() ?: 1,
                                totalGoals = goals
                            )
                            Toast.makeText(context, "Presupuesto guardado", Toast.LENGTH_SHORT).show()
                            onContinue()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Guardar y continuar")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
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
