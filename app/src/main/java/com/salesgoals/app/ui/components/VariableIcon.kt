package com.salesgoals.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector
import com.salesgoals.app.data.models.VariableType

/** Icono representativo para cada variable. */
val VariableType.icon: ImageVector
    get() = when (this) {
        VariableType.VOLUME -> Icons.Default.BarChart
        VariableType.CREDIT -> Icons.Default.CreditCard
        VariableType.WARRANTY -> Icons.Default.VerifiedUser
        VariableType.CASH_CREDIT -> Icons.Default.Payments
        VariableType.PHONES -> Icons.Default.Smartphone
    }
