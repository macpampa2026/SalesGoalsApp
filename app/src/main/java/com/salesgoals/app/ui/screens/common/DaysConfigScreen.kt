package com.salesgoals.app.ui.screens.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salesgoals.app.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaysConfigScreen(
    initialDays: Int,
    onSave: (Int) -> Unit,
    onBack: () -> Unit
) {
    var slider by remember { mutableStateOf(initialDays.toFloat().coerceIn(1f, 31f)) }
    var text by remember { mutableStateOf(initialDays.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Días Laborales") },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(
                title = "Días reales del mes",
                subtitle = "Restá domingos, francos, feriados y vacaciones para obtener los días efectivos de venta."
            )

            OutlinedTextField(
                value = text,
                onValueChange = { v ->
                    val filtered = v.filter { it.isDigit() }.take(2)
                    text = filtered
                    val n = filtered.toIntOrNull() ?: 0
                    if (n in 1..31) slider = n.toFloat()
                },
                label = { Text("Cantidad de días (1-31)") },
                modifier = Modifier.fillMaxWidth()
            )

            Slider(
                value = slider,
                onValueChange = {
                    slider = it
                    text = it.toInt().toString()
                },
                valueRange = 1f..31f,
                steps = 29
            )
            Text("Días seleccionados: ${slider.toInt()}", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onSave(slider.toInt()) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Guardar")
            }
        }
    }
}
