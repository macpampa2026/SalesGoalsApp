package com.salesgoals.app.ui.screens.manager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.salesgoals.app.SalesGoalsApplication
import com.salesgoals.app.data.entities.BudgetEntity
import com.salesgoals.app.data.models.AdvisorBudgetPayload
import com.salesgoals.app.data.models.BranchDistribution
import com.salesgoals.app.data.models.VariableSet
import com.salesgoals.app.data.repository.SalesRepository
import com.salesgoals.app.data.repository.toGoals
import com.salesgoals.app.utils.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManagerUiState(
    val budget: BudgetEntity? = null,
    val branchName: String = "",
    val period: String = Formatters.currentPeriod(),
    val workingDays: Int = 22,
    val advisorCount: Int = 1,
    val totalGoals: VariableSet = VariableSet.ZERO,
    val perAdvisor: VariableSet = VariableSet.ZERO,
    val perDayPerAdvisor: VariableSet = VariableSet.ZERO,
    val advisors: List<String> = listOf("Asesor 1")
)

private data class ManagerLocal(
    val initialized: Boolean = false,
    val branchName: String = "",
    val period: String = Formatters.currentPeriod(),
    val workingDays: Int = 22,
    val advisorCount: Int = 1,
    val totalGoals: VariableSet = VariableSet.ZERO,
    val advisors: List<String> = listOf("Asesor 1")
)

class ManagerViewModel(
    application: Application,
    val repository: SalesRepository
) : AndroidViewModel(application) {

    private val local = MutableStateFlow(ManagerLocal())

    val state: StateFlow<ManagerUiState> = combine(
        repository.observeBudget(),
        local
    ) { budget, ov ->
        // Hidratar la primera vez con datos persistidos (si hay) y modo gerencia
        val effective = if (!ov.initialized && budget != null && budget.isManagerMode) {
            ov.copy(
                initialized = true,
                branchName = budget.branchName,
                period = budget.period.ifBlank { Formatters.currentPeriod() },
                workingDays = budget.workingDays,
                advisorCount = budget.advisorCount,
                totalGoals = budget.toGoals(),
                advisors = adjustList(ov.advisors, budget.advisorCount)
            ).also { local.value = it }
        } else if (!ov.initialized) {
            ov.copy(initialized = true).also { local.value = it }
        } else ov

        val perAdvisor = effective.totalGoals / effective.advisorCount
        val perDay = if (effective.workingDays > 0) perAdvisor / effective.workingDays else perAdvisor

        ManagerUiState(
            budget = budget,
            branchName = effective.branchName,
            period = effective.period,
            workingDays = effective.workingDays,
            advisorCount = effective.advisorCount,
            totalGoals = effective.totalGoals,
            perAdvisor = perAdvisor,
            perDayPerAdvisor = perDay,
            advisors = effective.advisors
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManagerUiState())

    fun updateBranchName(v: String) { local.value = local.value.copy(branchName = v) }
    fun updatePeriod(v: String) { local.value = local.value.copy(period = v) }
    fun updateWorkingDays(v: Int) { local.value = local.value.copy(workingDays = v.coerceIn(1, 31)) }
    fun updateAdvisorCount(v: Int) {
        val safe = v.coerceIn(1, 50)
        local.value = local.value.copy(
            advisorCount = safe,
            advisors = adjustList(local.value.advisors, safe)
        )
    }
    fun updateTotalGoals(goals: VariableSet) { local.value = local.value.copy(totalGoals = goals) }
    fun updateAdvisorName(index: Int, name: String) {
        val list = local.value.advisors.toMutableList()
        while (list.size <= index) list.add("Asesor ${list.size + 1}")
        list[index] = name
        local.value = local.value.copy(advisors = list)
    }

    private fun adjustList(current: List<String>, size: Int): List<String> {
        val list = current.toMutableList()
        while (list.size < size) list.add("Asesor ${list.size + 1}")
        while (list.size > size) list.removeAt(list.lastIndex)
        return list
    }

    fun saveBranchBudget() {
        viewModelScope.launch {
            val s = state.value
            repository.saveBudget(
                BudgetEntity(
                    id = 1,
                    ownerName = "",
                    branchName = s.branchName,
                    period = s.period,
                    workingDays = s.workingDays,
                    advisorCount = s.advisorCount,
                    goalVolume = s.totalGoals.volume,
                    goalCredit = s.totalGoals.credit,
                    goalWarranty = s.totalGoals.warranty,
                    goalCashCredit = s.totalGoals.cashCredit,
                    goalPhones = s.totalGoals.phones,
                    isManagerMode = true
                )
            )
        }
    }

    fun buildPayloadFor(advisorName: String): AdvisorBudgetPayload {
        val s = state.value
        return AdvisorBudgetPayload(
            advisorName = advisorName,
            branchName = s.branchName,
            period = s.period,
            workingDays = s.workingDays,
            goals = s.perAdvisor,
            notes = "Distribución automática (${s.advisorCount} asesores)"
        )
    }

    fun buildDistribution(): BranchDistribution {
        val s = state.value
        return BranchDistribution(
            branchName = s.branchName,
            period = s.period,
            workingDays = s.workingDays,
            advisorCount = s.advisorCount,
            totalGoals = s.totalGoals,
            perAdvisorGoals = s.perAdvisor,
            advisors = s.advisors
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SalesGoalsApplication
                return ManagerViewModel(app, app.repository) as T
            }
        }
    }
}
