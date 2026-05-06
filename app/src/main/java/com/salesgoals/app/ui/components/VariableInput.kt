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
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { txt ->
                val filtered = txt.filter { c -> c.isDigit() || c == ',' || c == '.' }
                onValueChange(filtered)
            },
            label = { Text(label ?: type.displayName) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(if (type.isCurrency) "Monto en $" else "Cantidad de unidades")
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}
