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
    val advisors: List<String> = listOf("Asesor 1"),
    val isLoaded: Boolean = false
)

/**
 * VM del modo Gerencia. El estado se deriva DIRECTAMENTE de Room para evitar
 * doble fuente de verdad y race conditions. Los nombres de asesores se manejan
 * en memoria (no se persisten al budget).
 */
class ManagerViewModel(
    application: Application,
    private val repository: SalesRepository
) : AndroidViewModel(application) {

    private val advisorNames = MutableStateFlow<List<String>>(listOf("Asesor 1"))

    init {
        // Reacción separada al budget: ajusta el tamaño de advisorNames cuando
        // cambia advisorCount. No mutamos desde dentro del combine para evitar
        // race conditions entre downstream y upstream.
        viewModelScope.launch {
            repository.observeManagerBudget().collect { budget ->
                val safeCount = (budget?.advisorCount ?: 1).coerceIn(1, 50)
                val current = advisorNames.value
                if (current.size != safeCount) {
                    advisorNames.value = adjustList(current, safeCount)
                }
            }
        }
    }

    val state: StateFlow<ManagerUiState> = combine(
        repository.observeManagerBudget(),
        advisorNames
    ) { budget, names ->
        val workingDays = (budget?.workingDays ?: 22).coerceIn(1, 31)
        val advisorCount = (budget?.advisorCount ?: 1).coerceIn(1, 50)
        val totalGoals = budget?.toGoals() ?: VariableSet.ZERO
        val perAdvisor = totalGoals / advisorCount
        val perDay = if (workingDays > 0) perAdvisor / workingDays else perAdvisor
        val syncedNames = adjustList(names, advisorCount)
        ManagerUiState(
            budget = budget,
            branchName = budget?.branchName.orEmpty(),
            period = Formatters.safePeriod(budget?.period),
            workingDays = workingDays,
            advisorCount = advisorCount,
            totalGoals = totalGoals,
            perAdvisor = perAdvisor,
            perDayPerAdvisor = perDay,
            advisors = syncedNames,
            isLoaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManagerUiState())

    fun updateAdvisorName(index: Int, name: String) {
        val list = advisorNames.value.toMutableList()
        while (list.size <= index) list.add("Asesor ${list.size + 1}")
        list[index] = name
        advisorNames.value = list
    }

    private fun adjustList(current: List<String>, size: Int): List<String> {
        if (size <= 0) return listOf("Asesor 1")
        val list = current.toMutableList()
        while (list.size < size) list.add("Asesor ${list.size + 1}")
        while (list.size > size) list.removeAt(list.lastIndex)
        return list
    }

    /**
     * Guarda el presupuesto. Suspende hasta que la escritura a Room se completa.
     * La UI debe llamarla desde un scope.launch para esperar antes de navegar.
     */
    suspend fun saveBranchBudget(
        branchName: String,
        period: String,
        workingDays: Int,
        advisorCount: Int,
        totalGoals: VariableSet
    ) {
        repository.saveManagerBudget(
            BudgetEntity(
                ownerName = "",
                branchName = branchName,
                period = Formatters.safePeriod(period),
                workingDays = workingDays.coerceIn(1, 31),
                advisorCount = advisorCount.coerceIn(1, 50),
                goalVolume = totalGoals.volume,
                goalCredit = totalGoals.credit,
                goalWarranty = totalGoals.warranty,
                goalCashCredit = totalGoals.cashCredit,
                goalPhones = totalGoals.phones
            )
        )
    }

    /** Borra el presupuesto de la sucursal. */
    suspend fun resetAll() {
        repository.resetManagerAll()
        advisorNames.value = listOf("Asesor 1")
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
