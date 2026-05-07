package com.salesgoals.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salesgoals.app.data.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Hay dos filas posibles:
 *  - id = 1 → presupuesto del Asesor (modo Vendedor)
 *  - id = 2 → presupuesto de la Gerencia (modo Administrador)
 */
@Dao
interface BudgetDao {

    @Query("SELECT * FROM budget WHERE id = 1 LIMIT 1")
    fun observeAdvisor(): Flow<BudgetEntity?>

    @Query("SELECT * FROM budget WHERE id = 2 LIMIT 1")
    fun observeManager(): Flow<BudgetEntity?>

    @Query("SELECT * FROM budget WHERE id = 1 LIMIT 1")
    suspend fun getAdvisor(): BudgetEntity?

    @Query("SELECT * FROM budget WHERE id = 2 LIMIT 1")
    suspend fun getManager(): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budget WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM budget")
    suspend fun clearAll()
}
