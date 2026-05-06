package com.salesgoals.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salesgoals.app.data.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget WHERE id = 1 LIMIT 1")
    fun observe(): Flow<BudgetEntity?>

    @Query("SELECT * FROM budget WHERE id = 1 LIMIT 1")
    suspend fun get(): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budget")
    suspend fun clear()
}
