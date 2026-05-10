package com.salesgoals.app.data.repository

import androidx.room.withTransaction
import com.salesgoals.app.data.database.AppDatabase
import com.salesgoals.app.data.database.BudgetDao
import com.salesgoals.app.data.database.DailyEntryDao
import com.salesgoals.app.data.entities.BudgetEntity
import com.salesgoals.app.data.entities.DailyEntryEntity
import com.salesgoals.app.data.models.AdvisorBudgetPayload
import com.salesgoals.app.data.models.VariableSet
import kotlinx.coroutines.flow.Flow

/**
 * Único punto de acceso a la persistencia local.
 * Asesor (id=1) y Gerencia (id=2) tienen filas separadas.
 */
class SalesRepository(
    private val database: AppDatabase,
    private val budgetDao: BudgetDao,
    private val dailyEntryDao: DailyEntryDao
) {

    // ===== Asesor =====
    fun observeAdvisorBudget(): Flow<BudgetEntity?> = budgetDao.observeAdvisor()
    suspend fun getAdvisorBudget(): BudgetEntity? = budgetDao.getAdvisor()
    suspend fun saveAdvisorBudget(budget: BudgetEntity) {
        budgetDao.upsert(budget.copy(id = 1, isManagerMode = false, updatedAt = System.currentTimeMillis()))
    }
    suspend fun clearAdvisorBudget() = budgetDao.deleteById(1)

    // ===== Gerencia =====
    fun observeManagerBudget(): Flow<BudgetEntity?> = budgetDao.observeManager()
    suspend fun getManagerBudget(): BudgetEntity? = budgetDao.getManager()
    suspend fun saveManagerBudget(budget: BudgetEntity) {
        budgetDao.upsert(budget.copy(id = 2, isManagerMode = true, updatedAt = System.currentTimeMillis()))
    }
    suspend fun clearManagerBudget() = budgetDao.deleteById(2)

    // ===== Daily entries (asesor) =====
    fun observeEntries(period: String): Flow<List<DailyEntryEntity>> =
        dailyEntryDao.observeByPeriod(period)

    suspend fun getEntries(period: String): List<DailyEntryEntity> =
        dailyEntryDao.getByPeriod(period)

    suspend fun getEntry(date: String): DailyEntryEntity? = dailyEntryDao.getByDate(date)
    suspend fun saveEntry(entry: DailyEntryEntity) = dailyEntryDao.upsert(entry)
    suspend fun deleteEntry(date: String) = dailyEntryDao.deleteByDate(date)
    suspend fun clearEntries() = dailyEntryDao.clear()

    /** Borra presupuesto del asesor + todas las cargas diarias atómicamente. */
    suspend fun resetAdvisorAll() {
        database.withTransaction {
            budgetDao.deleteById(1)
            dailyEntryDao.clear()
        }
    }

    /** Borra solo el presupuesto de la sucursal. */
    suspend fun resetManagerAll() {
        budgetDao.deleteById(2)
    }

    /** Aplica un payload importado por el asesor. */
    suspend fun applyImportedBudget(payload: AdvisorBudgetPayload) {
        saveAdvisorBudget(
            BudgetEntity(
                ownerName = payload.advisorName,
                branchName = payload.branchName,
                period = payload.period,
                workingDays = payload.workingDays,
                advisorCount = 1,
                goalVolume = payload.goals.volume,
                goalCredit = payload.goals.credit,
                goalWarranty = payload.goals.warranty,
                goalCashCredit = payload.goals.cashCredit,
                goalPhones = payload.goals.phones
            )
        )
    }
}

fun BudgetEntity.toGoals(): VariableSet = VariableSet(
    volume = goalVolume,
    credit = goalCredit,
    warranty = goalWarranty,
    cashCredit = goalCashCredit,
    phones = goalPhones
)

fun DailyEntryEntity.toSet(): VariableSet = VariableSet(
    volume = volume,
    credit = credit,
    warranty = warranty,
    cashCredit = cashCredit,
    phones = phones
)

/**
 * Devuelve los objetivos del día. Si todos los `target_*` son 0, devuelve null
 * (se debe usar el default mensual/días).
 */
fun DailyEntryEntity.toTargetsOrNull(): VariableSet? {
    val anyTarget = targetVolume + targetCredit + targetWarranty + targetCashCredit + targetPhones
    if (anyTarget <= 0.0) return null
    return VariableSet(
        volume = targetVolume,
        credit = targetCredit,
        warranty = targetWarranty,
        cashCredit = targetCashCredit,
        phones = targetPhones
    )
}
