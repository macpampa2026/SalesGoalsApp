package com.salesgoals.app.data.models

import kotlinx.serialization.Serializable

/**
 * Estructura común de las 5 variables a controlar.
 * Volumen, Crédito, Garantía, Crédito Efectivo en pesos ($).
 * Celulares en unidades.
 */
@Serializable
data class VariableSet(
    val volume: Double = 0.0,
    val credit: Double = 0.0,
    val warranty: Double = 0.0,
    val cashCredit: Double = 0.0,
    val phones: Double = 0.0
) {
    operator fun plus(other: VariableSet) = VariableSet(
        volume = volume + other.volume,
        credit = credit + other.credit,
        warranty = warranty + other.warranty,
        cashCredit = cashCredit + other.cashCredit,
        phones = phones + other.phones
    )

    operator fun div(divisor: Int): VariableSet {
        if (divisor <= 0) return ZERO
        return VariableSet(
            volume = volume / divisor,
            credit = credit / divisor,
            warranty = warranty / divisor,
            cashCredit = cashCredit / divisor,
            phones = phones / divisor
        )
    }

    operator fun div(divisor: Double): VariableSet {
        if (divisor <= 0.0 || !divisor.isFinite()) return ZERO
        return VariableSet(
            volume = volume / divisor,
            credit = credit / divisor,
            warranty = warranty / divisor,
            cashCredit = cashCredit / divisor,
            phones = phones / divisor
        )
    }

    companion object {
        val ZERO = VariableSet()
    }
}

/**
 * Tipo de variable usado para iterar / mostrar dinámicamente.
 */
enum class VariableType(val displayName: String, val isCurrency: Boolean) {
    VOLUME("Volumen", true),
    CREDIT("Crédito", true),
    WARRANTY("Garantía", true),
    CASH_CREDIT("Crédito Efectivo", true),
    PHONES("Celulares", false);

    fun valueOf(set: VariableSet): Double = when (this) {
        VOLUME -> set.volume
        CREDIT -> set.credit
        WARRANTY -> set.warranty
        CASH_CREDIT -> set.cashCredit
        PHONES -> set.phones
    }

    fun setValue(set: VariableSet, newValue: Double): VariableSet = when (this) {
        VOLUME -> set.copy(volume = newValue)
        CREDIT -> set.copy(credit = newValue)
        WARRANTY -> set.copy(warranty = newValue)
        CASH_CREDIT -> set.copy(cashCredit = newValue)
        PHONES -> set.copy(phones = newValue)
    }
}
