package com.salesgoals.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Resultado diario cargado por el asesor.
 * date format: YYYY-MM-DD
 */
@Entity(tableName = "daily_entry")
data class DailyEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "volume") val volume: Double = 0.0,
    @ColumnInfo(name = "credit") val credit: Double = 0.0,
    @ColumnInfo(name = "warranty") val warranty: Double = 0.0,
    @ColumnInfo(name = "cash_credit") val cashCredit: Double = 0.0,
    @ColumnInfo(name = "phones") val phones: Double = 0.0,
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
