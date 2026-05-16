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
        // Saneamos espacios y validamos formato YYYY-MM, así no se rompe la query
        // `substr(date, 1, 7) = :period` si llega "2026-05 " con espacio extra.
        val cleanPeriod = com.salesgoals.app.utils.Formatters.safePeriod(payload.period.trim())
        saveAdvisorBudget(
            BudgetEntity(
                ownerName = payload.advisorName.trim(),
                branchName = payload.branchName.trim(),
                period = cleanPeriod,
                workingDays = payload.workingDays.coerceIn(1, 31),
                advisorCount = 1,
                goalVolume = payload.goals.volume,
                goalCredit = payload.goals.credit,
                goalWarranty = payload.goals.warranty,
                goalCashCredit = payload.goals.cashCredit,
                goalPhones = payload.goals.phones
            )
        )
    }

    /** Construye un backup completo: presupuesto del asesor + todas las cargas diarias. */
    suspend fun buildFullBackup(): com.salesgoals.app.data.models.FullBackupPayload {
        val budget = getAdvisorBudget()
        val period = budget?.period?.ifBlank { com.salesgoals.app.utils.Formatters.currentPeriod() }
            ?: com.salesgoals.app.utils.Formatters.currentPeriod()
        val entries = getEntries(period)
        val budgetPayload = budget?.let {
            com.salesgoals.app.data.models.AdvisorBudgetPayload(
                advisorName = it.ownerName,
                branchName = it.branchName,
                period = it.period,
                workingDays = it.workingDays,
                goals = it.toGoals()
            )
        }
        return com.salesgoals.app.data.models.FullBackupPayload(
            budget = budgetPayload,
            entries = entries.map { e ->
                com.salesgoals.app.data.models.SerializableDailyEntry(
                    date = e.date,
                    volume = e.volume, credit = e.credit, warranty = e.warranty,
                    cashCredit = e.cashCredit, phones = e.phones,
                    targetVolume = e.targetVolume, targetCredit = e.targetCredit,
                    targetWarranty = e.targetWarranty, targetCashCredit = e.targetCashCredit,
                    targetPhones = e.targetPhones,
                    note = e.note
                )
            }
        )
    }

    /** Restaura un backup completo atómicamente. */
    suspend fun restoreFullBackup(payload: com.salesgoals.app.data.models.FullBackupPayload) {
        database.withTransaction {
            payload.budget?.let { applyImportedBudget(it) }
            // Reemplazamos las entries existentes con las del backup.
            dailyEntryDao.clear()
            payload.entries.forEach { e ->
                dailyEntryDao.upsert(
                    DailyEntryEntity(
                        date = e.date,
                        volume = e.volume, credit = e.credit, warranty = e.warranty,
                        cashCredit = e.cashCredit, phones = e.phones,
                        targetVolume = e.targetVolume, targetCredit = e.targetCredit,
                        targetWarranty = e.targetWarranty, targetCashCredit = e.targetCashCredit,
                        targetPhones = e.targetPhones,
                        note = e.note
                    )
                )
            }
        }
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
