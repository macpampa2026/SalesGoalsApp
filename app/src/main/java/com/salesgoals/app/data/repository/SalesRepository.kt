package com.salesgoals.app.data.repository

import com.salesgoals.app.data.database.BudgetDao
import com.salesgoals.app.data.database.DailyEntryDao
import com.salesgoals.app.data.entities.BudgetEntity
import com.salesgoals.app.data.entities.DailyEntryEntity
import com.salesgoals.app.data.models.AdvisorBudgetPayload
import com.salesgoals.app.data.models.VariableSet
import kotlinx.coroutines.flow.Flow

/**
 * Único punto de acceso a la persistencia local.
 */
class SalesRepository(
    private val budgetDao: BudgetDao,
    private val dailyEntryDao: DailyEntryDao
) {

    fun observeBudget(): Flow<BudgetEntity?> = budgetDao.observe()
    suspend fun getBudget(): BudgetEntity? = budgetDao.get()
    suspend fun saveBudget(budget: BudgetEntity) = budgetDao.upsert(budget)
    suspend fun clearBudget() = budgetDao.clear()

    fun observeEntries(period: String): Flow<List<DailyEntryEntity>> =
        dailyEntryDao.observeByPeriod(period)

    suspend fun getEntries(period: String): List<DailyEntryEntity> =
        dailyEntryDao.getByPeriod(period)

    suspend fun getEntry(date: String): DailyEntryEntity? = dailyEntryDao.getByDate(date)
    suspend fun saveEntry(entry: DailyEntryEntity) = dailyEntryDao.upsert(entry)
    suspend fun deleteEntry(date: String) = dailyEntryDao.deleteByDate(date)
    suspend fun clearEntries() = dailyEntryDao.clear()

    /**
     * Aplica un payload importado por el asesor.
     * Sobrescribe el presupuesto vigente y limpia entries previas
     * de un periodo distinto al recibido.
     */
    suspend fun applyImportedBudget(payload: AdvisorBudgetPayload) {
        val budget = BudgetEntity(
            id = 1,
            ownerName = payload.advisorName,
            branchName = payload.branchName,
            period = payload.period,
            workingDays = payload.workingDays,
            advisorCount = 1,
            goalVolume = payload.goals.volume,
            goalCredit = payload.goals.credit,
            goalWarranty = payload.goals.warranty,
            goalCashCredit = payload.goals.cashCredit,
            goalPhones = payload.goals.phones,
            isManagerMode = false,
            updatedAt = System.currentTimeMillis()
        )
        budgetDao.upsert(budget)
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
