package com.salesgoals.app.ui.screens.advisor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.salesgoals.app.SalesGoalsApplication
import com.salesgoals.app.data.entities.BudgetEntity
import com.salesgoals.app.data.entities.DailyEntryEntity
import com.salesgoals.app.data.models.AdvisorBudgetPayload
import com.salesgoals.app.data.models.VariableProgress
import com.salesgoals.app.data.models.VariableSet
import com.salesgoals.app.data.models.VariableType
import com.salesgoals.app.data.repository.SalesRepository
import com.salesgoals.app.data.repository.toGoals
import com.salesgoals.app.data.repository.toSet
import com.salesgoals.app.utils.Formatters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdvisorUiState(
    val budget: BudgetEntity? = null,
    val entries: List<DailyEntryEntity> = emptyList(),
    val workingDays: Int = 22,
    val daysElapsed: Int = 1,
    val progressByVariable: List<VariableProgress> = emptyList(),
    val isLoaded: Boolean = false
)

/**
 * VM del modo Asesor. Estado derivado de Room: observeAdvisorBudget + observeEntries.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdvisorViewModel(
    application: Application,
    private val repository: SalesRepository
) : AndroidViewModel(application) {

    /** Lee una entrada diaria desde Room. Para hidratar inputs en DailyEntryScreen. */
    suspend fun getEntry(date: String): DailyEntryEntity? = repository.getEntry(date)

    val state: StateFlow<AdvisorUiState> = repository.observeAdvisorBudget()
        // Solo re-suscribir a observeEntries cuando cambia el budget en algo
        // significativo (no en cada updatedAt menor). Evita parpadeos.
        .map { budget ->
            val period = budget?.period?.let { Formatters.safePeriod(it) } ?: Formatters.currentPeriod()
            budget to period
        }
        .distinctUntilChanged()
        .flatMapLatest { (budget, period) ->
            repository.observeEntries(period).map { entries -> budget to entries }
        }
        .map { (budget, entries) -> buildUiState(budget, entries) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdvisorUiState())

    private fun buildUiState(
        budget: BudgetEntity?,
        entries: List<DailyEntryEntity>
    ): AdvisorUiState {
        val workingDays = (budget?.workingDays ?: 22).coerceIn(1, 31)
        val period = budget?.period?.let { Formatters.safePeriod(it) } ?: Formatters.currentPeriod()
        val daysElapsed = Formatters.elapsedWorkingDays(workingDays, period).coerceAtLeast(1)
        val accumulated = entries.fold(VariableSet.ZERO) { acc, e -> acc + e.toSet() }
        val goals = budget?.toGoals() ?: VariableSet.ZERO
        val progress = VariableType.values().map { type ->
            VariableProgress(
                type = type,
                monthlyGoal = type.valueOf(goals),
                accumulated = type.valueOf(accumulated),
                workingDays = workingDays,
                daysElapsed = daysElapsed
            )
        }
        return AdvisorUiState(
            budget = budget,
            entries = entries,
            workingDays = workingDays,
            daysElapsed = daysElapsed,
            progressByVariable = progress,
            isLoaded = true
        )
    }

    /** Save fire-and-forget para inputs sin navegación */
    fun saveDailyEntryAsync(entry: DailyEntryEntity) {
        viewModelScope.launch { repository.saveEntry(entry) }
    }

    /** Save suspend para flujos donde la UI necesita esperar */
    suspend fun saveDailyEntry(entry: DailyEntryEntity) {
        repository.saveEntry(entry)
    }

    fun deleteEntry(date: String) {
        viewModelScope.launch { repository.deleteEntry(date) }
    }

    fun updateWorkingDays(days: Int) {
        viewModelScope.launch {
            val current = repository.getAdvisorBudget()
                ?: BudgetEntity(period = Formatters.currentPeriod())
            repository.saveAdvisorBudget(current.copy(workingDays = days.coerceIn(1, 31)))
        }
    }

    suspend fun applyImportedPayload(payload: AdvisorBudgetPayload) {
        repository.applyImportedBudget(payload)
    }

    /** Construye el backup completo (presupuesto + cargas) para exportar. */
    suspend fun buildFullBackup(): com.salesgoals.app.data.models.FullBackupPayload =
        repository.buildFullBackup()

    /** Restaura un backup completo: sobrescribe el presupuesto y las cargas diarias. */
    suspend fun restoreFullBackup(payload: com.salesgoals.app.data.models.FullBackupPayload) {
        repository.restoreFullBackup(payload)
    }

    /** Borra todo: presupuesto del asesor + cargas diarias. */
    suspend fun resetAll() {
        repository.resetAdvisorAll()
    }

    /**
     * Suma incremental al objetivo: lee la carga de HOY (si existe), suma el monto
     * a la variable indicada y guarda. Permite ir actualizando en vivo sin abrir
     * el formulario completo.
     *
     * Devuelve `true` si se aplicó, `false` si HOY no pertenece al período del
     * presupuesto (la UI debe avisarle al usuario y no escribir).
     */
    suspend fun quickAddToVariable(type: VariableType, amount: Double): Boolean {
        if (amount <= 0.0 || !amount.isFinite()) return false
        val budget = repository.getAdvisorBudget()
        val budgetPeriod = budget?.period?.let { Formatters.safePeriod(it) }
            ?: Formatters.currentPeriod()
        val today = Formatters.today()
        if (today.substring(0, 7) != budgetPeriod) {
            return false
        }
        val existing = repository.getEntry(today)
        // Helper para sumar resguardando contra NaN/Infinity tras la suma.
        fun safeAdd(a: Double, b: Double): Double {
            val r = a + b
            return if (r.isFinite()) r else a
        }
        val newEntry = if (existing != null) {
            when (type) {
                VariableType.VOLUME -> existing.copy(volume = safeAdd(existing.volume, amount))
                VariableType.CREDIT -> existing.copy(credit = safeAdd(existing.credit, amount))
                VariableType.WARRANTY -> existing.copy(warranty = safeAdd(existing.warranty, amount))
                VariableType.CASH_CREDIT -> existing.copy(cashCredit = safeAdd(existing.cashCredit, amount))
                VariableType.PHONES -> existing.copy(phones = safeAdd(existing.phones, amount))
            }.copy(updatedAt = System.currentTimeMillis())
        } else {
            DailyEntryEntity(
                date = today,
                volume = if (type == VariableType.VOLUME) amount else 0.0,
                credit = if (type == VariableType.CREDIT) amount else 0.0,
                warranty = if (type == VariableType.WARRANTY) amount else 0.0,
                cashCredit = if (type == VariableType.CASH_CREDIT) amount else 0.0,
                phones = if (type == VariableType.PHONES) amount else 0.0
            )
        }
        repository.saveEntry(newEntry)
        return true
    }

    suspend fun saveManualBudget(
        advisorName: String,
        period: String,
        workingDays: Int,
        goals: VariableSet
    ) {
        repository.saveAdvisorBudget(
            BudgetEntity(
                ownerName = advisorName,
                branchName = "",
                period = Formatters.safePeriod(period),
                workingDays = workingDays.coerceIn(1, 31),
                advisorCount = 1,
                goalVolume = goals.volume,
                goalCredit = goals.credit,
                goalWarranty = goals.warranty,
                goalCashCredit = goals.cashCredit,
                goalPhones = goals.phones
            )
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SalesGoalsApplication
                return AdvisorViewModel(app, app.repository) as T
            }
        }
    }
}
