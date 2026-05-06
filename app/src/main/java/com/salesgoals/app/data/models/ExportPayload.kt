package com.salesgoals.app.data.models

import kotlinx.serialization.Serializable

/**
 * Payload exportado por la Gerencia y consumido por el Asesor.
 */
@Serializable
data class AdvisorBudgetPayload(
    val version: Int = 1,
    val advisorName: String = "",
    val branchName: String = "",
    val period: String = "",            // YYYY-MM
    val workingDays: Int = 22,
    val goals: VariableSet = VariableSet.ZERO,
    val notes: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

/**
 * Resultado de la división del presupuesto de sucursal.
 */
@Serializable
data class BranchDistribution(
    val branchName: String = "",
    val period: String = "",
    val workingDays: Int = 22,
    val advisorCount: Int = 1,
    val totalGoals: VariableSet = VariableSet.ZERO,
    val perAdvisorGoals: VariableSet = VariableSet.ZERO,
    val advisors: List<String> = emptyList()
)
