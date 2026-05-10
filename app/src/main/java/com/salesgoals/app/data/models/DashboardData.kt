package com.salesgoals.app.data.models

/**
 * Estado de cumplimiento del objetivo (semáforo).
 */
enum class PerformanceStatus {
    ON_TRACK,    // Verde – cumpliendo
    WARNING,     // Amarillo – riesgo
    BEHIND       // Rojo – atrasado
}

/**
 * Datos calculados para mostrar en el dashboard por variable.
 */
data class VariableProgress(
    val type: VariableType,
    val monthlyGoal: Double,
    val accumulated: Double,
    val workingDays: Int,
    val daysElapsed: Int,
) {
    val remaining: Double get() = (monthlyGoal - accumulated).coerceAtLeast(0.0)
    val percent: Double get() = when {
        monthlyGoal <= 0.0 -> 0.0
        else -> ((accumulated / monthlyGoal) * 100.0).coerceIn(0.0, 9999.0)
    }

    /** Objetivo diario base = total / días laborales */
    val dailyGoal: Double get() = if (workingDays > 0) monthlyGoal / workingDays else 0.0

    /** Días restantes (al menos 1 para evitar divisiones absurdas) */
    val daysRemaining: Int get() = (workingDays - daysElapsed).coerceAtLeast(0)

    /** Ritmo necesario por día restante para alcanzar la meta */
    val requiredPace: Double get() {
        val dr = daysRemaining
        return if (dr > 0) remaining / dr else 0.0
    }

    /** Ritmo actual = acumulado / días transcurridos */
    val currentPace: Double get() = if (daysElapsed > 0) accumulated / daysElapsed else 0.0

    /** Proyección a fin de mes */
    val projection: Double get() = currentPace * workingDays

    /** Diferencia entre el ritmo actual y el esperado */
    val paceDelta: Double get() = currentPace - dailyGoal

    /** Estado semáforo */
    val status: PerformanceStatus get() {
        if (monthlyGoal <= 0.0) return PerformanceStatus.ON_TRACK
        val expected = if (workingDays > 0) (monthlyGoal * daysElapsed) / workingDays else monthlyGoal
        val ratio = if (expected > 0) accumulated / expected else 1.0
        return when {
            ratio >= 0.95 -> PerformanceStatus.ON_TRACK
            ratio >= 0.80 -> PerformanceStatus.WARNING
            else -> PerformanceStatus.BEHIND
        }
    }
}
