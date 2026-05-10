package com.salesgoals.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.salesgoals.app.data.models.VariableType

@Composable
fun VariableInput(
    type: VariableType,
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { txt ->
                // Permite dígitos + un único separador decimal (último , o .).
                // Acepta otros , o . solo si actúan como separadores de miles
                // (no se descartan acá; el parser luego decide).
                val filtered = txt.filter { c -> c.isDigit() || c == ',' || c == '.' }
                onValueChange(filtered)
            },
            label = { Text(label ?: type.displayName) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(if (type.isCurrency) "Monto en $ (sin símbolos)" else "Cantidad de unidades")
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}
