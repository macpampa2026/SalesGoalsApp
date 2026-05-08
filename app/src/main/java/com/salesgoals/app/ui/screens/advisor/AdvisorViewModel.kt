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
    val repository: SalesRepository
) : AndroidViewModel(application) {

    val state: StateFlow<AdvisorUiState> = repository.observeAdvisorBudget()
        .flatMapLatest { budget ->
            val period = budget?.period?.ifBlank { Formatters.currentPeriod() } ?: Formatters.currentPeriod()
            repository.observeEntries(period).map { entries -> budget to entries }
        }
        .map { (budget, entries) -> buildUiState(budget, entries) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdvisorUiState())

    private fun buildUiState(
        budget: BudgetEntity?,
        entries: List<DailyEntryEntity>
    ): AdvisorUiState {
        val workingDays = (budget?.workingDays ?: 22).coerceIn(1, 31)
        val period = budget?.period?.ifBlank { Formatters.currentPeriod() } ?: Formatters.currentPeriod()
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
                period = period.ifBlank { Formatters.currentPeriod() },
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
