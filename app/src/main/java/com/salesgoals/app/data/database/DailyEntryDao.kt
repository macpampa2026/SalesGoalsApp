package com.salesgoals.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salesgoals.app.data.entities.DailyEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyEntryDao {

    @Query("SELECT * FROM daily_entry WHERE substr(date, 1, 7) = :period ORDER BY date ASC")
    fun observeByPeriod(period: String): Flow<List<DailyEntryEntity>>

    @Query("SELECT * FROM daily_entry WHERE substr(date, 1, 7) = :period ORDER BY date ASC")
    suspend fun getByPeriod(period: String): List<DailyEntryEntity>

    @Query("SELECT * FROM daily_entry WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DailyEntryEntity)

    @Query("DELETE FROM daily_entry WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM daily_entry")
    suspend fun clear()
}
