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
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class AdvisorViewModel(
    application: Application,
    val repository: SalesRepository
) : AndroidViewModel(application) {

    val state: StateFlow<AdvisorUiState> = repository.observeBudget()
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
        val workingDays = budget?.workingDays ?: 22
        val daysElapsed = Formatters.elapsedWorkingDays(workingDays).coerceAtLeast(1)
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
            isLoading = false
        )
    }

    fun saveDailyEntry(entry: DailyEntryEntity) {
        viewModelScope.launch { repository.saveEntry(entry) }
    }

    fun deleteEntry(date: String) {
        viewModelScope.launch { repository.deleteEntry(date) }
    }

    fun updateWorkingDays(days: Int) {
        viewModelScope.launch {
            val current = repository.getBudget()
                ?: BudgetEntity(period = Formatters.currentPeriod())
            repository.saveBudget(
                current.copy(
                    workingDays = days.coerceIn(1, 31),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun applyImportedPayload(payload: AdvisorBudgetPayload) {
        viewModelScope.launch { repository.applyImportedBudget(payload) }
    }

    fun saveManualBudget(
        advisorName: String,
        period: String,
        workingDays: Int,
        goals: VariableSet
    ) {
        viewModelScope.launch {
            repository.saveBudget(
                BudgetEntity(
                    id = 1,
                    ownerName = advisorName,
                    branchName = "",
                    period = period,
                    workingDays = workingDays,
                    advisorCount = 1,
                    goalVolume = goals.volume,
                    goalCredit = goals.credit,
                    goalWarranty = goals.warranty,
                    goalCashCredit = goals.cashCredit,
                    goalPhones = goals.phones,
                    isManagerMode = false
                )
            )
        }
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
