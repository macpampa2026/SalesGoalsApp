package com.salesgoals.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Presupuesto del asesor (modo Asesor) o sucursal (modo Gerencia).
 * Solo guardamos el "activo" actual; usamos PrimaryKey fija = 1.
 */
@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "owner_name") val ownerName: String = "",
    @ColumnInfo(name = "branch_name") val branchName: String = "",
    @ColumnInfo(name = "period") val period: String = "",       // YYYY-MM
    @ColumnInfo(name = "working_days") val workingDays: Int = 22,
    @ColumnInfo(name = "advisor_count") val advisorCount: Int = 1,

    // Objetivo individual (asesor) o total (gerencia)
    @ColumnInfo(name = "goal_volume") val goalVolume: Double = 0.0,
    @ColumnInfo(name = "goal_credit") val goalCredit: Double = 0.0,
    @ColumnInfo(name = "goal_warranty") val goalWarranty: Double = 0.0,
    @ColumnInfo(name = "goal_cash_credit") val goalCashCredit: Double = 0.0,
    @ColumnInfo(name = "goal_phones") val goalPhones: Double = 0.0,

    @ColumnInfo(name = "is_manager_mode") val isManagerMode: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
