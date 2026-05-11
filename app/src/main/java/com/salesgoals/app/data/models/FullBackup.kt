package com.salesgoals.app.data.models

import kotlinx.serialization.Serializable

/**
 * Respaldo completo del modo Asesor: presupuesto + todas las cargas diarias.
 * Pensado para que el usuario pueda exportar antes de actualizar la app
 * y restaurar después sin perder nada.
 */
@Serializable
data class FullBackupPayload(
    val version: Int = 2,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val budget: AdvisorBudgetPayload? = null,
    val entries: List<SerializableDailyEntry> = emptyList(),
    /** Marca para distinguir de un AdvisorBudgetPayload "normal". */
    val isFullBackup: Boolean = true
)

@Serializable
data class SerializableDailyEntry(
    val date: String,
    val volume: Double = 0.0,
    val credit: Double = 0.0,
    val warranty: Double = 0.0,
    val cashCredit: Double = 0.0,
    val phones: Double = 0.0,
    val targetVolume: Double = 0.0,
    val targetCredit: Double = 0.0,
    val targetWarranty: Double = 0.0,
    val targetCashCredit: Double = 0.0,
    val targetPhones: Double = 0.0,
    val note: String = ""
)
