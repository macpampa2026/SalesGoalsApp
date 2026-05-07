package com.salesgoals.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Resultado y objetivo diario.
 * date format: YYYY-MM-DD
 *
 * Los campos sin prefijo son las ventas reales del día.
 * Los campos `target_*` son objetivos diarios opcionales (0 = usar default mensual/días).
 */
@Entity(tableName = "daily_entry")
data class DailyEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "date") val date: String,

    // Ventas realizadas
    @ColumnInfo(name = "volume") val volume: Double = 0.0,
    @ColumnInfo(name = "credit") val credit: Double = 0.0,
    @ColumnInfo(name = "warranty") val warranty: Double = 0.0,
    @ColumnInfo(name = "cash_credit") val cashCredit: Double = 0.0,
    @ColumnInfo(name = "phones") val phones: Double = 0.0,

    // Objetivo diario opcional (0 = usar default mensual/días)
    @ColumnInfo(name = "target_volume", defaultValue = "0") val targetVolume: Double = 0.0,
    @ColumnInfo(name = "target_credit", defaultValue = "0") val targetCredit: Double = 0.0,
    @ColumnInfo(name = "target_warranty", defaultValue = "0") val targetWarranty: Double = 0.0,
    @ColumnInfo(name = "target_cash_credit", defaultValue = "0") val targetCashCredit: Double = 0.0,
    @ColumnInfo(name = "target_phones", defaultValue = "0") val targetPhones: Double = 0.0,

    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
