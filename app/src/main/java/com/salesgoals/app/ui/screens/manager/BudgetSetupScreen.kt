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
import androidx.compose.runtime.remember
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: ManagerViewModel = viewModel(factory = ManagerViewModel.Factory)
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var branch by remember { mutableStateOf(state.branchName) }
    var period by remember { mutableStateOf(state.period) }
    var days by remember { mutableStateOf(state.workingDays.toString()) }
    var advisors by remember { mutableStateOf(state.advisorCount.toString()) }
    var volume by remember { mutableStateOf(if (state.totalGoals.volume == 0.0) "" else state.totalGoals.volume.toLong().toString()) }
    var credit by remember { mutableStateOf(if (state.totalGoals.credit == 0.0) "" else state.totalGoals.credit.toLong().toString()) }
    var warranty by remember { mutableStateOf(if (state.totalGoals.warranty == 0.0) "" else state.totalGoals.warranty.toLong().toString()) }
    var cashCredit by remember { mutableStateOf(if (state.totalGoals.cashCredit == 0.0) "" else state.totalGoals.cashCredit.toLong().toString()) }
    var phones by remember { mutableStateOf(if (state.totalGoals.phones == 0.0) "" else state.totalGoals.phones.toLong().toString()) }

    LaunchedEffect(state) {
        if (branch.isEmpty()) branch = state.branchName
        if (volume.isEmpty() && state.totalGoals.volume > 0) volume = state.totalGoals.volume.toLong().toString()
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
                value = branch, onValueChange = { branch = it },
                label = { Text("Nombre de la sucursal") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = period, onValueChange = { period = it },
                label = { Text("Periodo (YYYY-MM)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = days, onValueChange = { days = it.filter { c -> c.isDigit() } },
                    label = { Text("Días laborales (1-31)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = advisors, onValueChange = { advisors = it.filter { c -> c.isDigit() } },
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
                onClick = {
                    viewModel.updateBranchName(branch)
                    viewModel.updatePeriod(period)
                    viewModel.updateWorkingDays(days.toIntOrNull() ?: 22)
                    viewModel.updateAdvisorCount(advisors.toIntOrNull() ?: 1)
                    viewModel.updateTotalGoals(
                        VariableSet(
                            volume = Formatters.toDouble(volume),
                            credit = Formatters.toDouble(credit),
                            warranty = Formatters.toDouble(warranty),
                            cashCredit = Formatters.toDouble(cashCredit),
                            phones = Formatters.toDouble(phones)
                        )
                    )
                    viewModel.saveBranchBudget()
                    Toast.makeText(context, "Presupuesto guardado", Toast.LENGTH_SHORT).show()
                    onContinue()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Continuar a Distribución")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}
