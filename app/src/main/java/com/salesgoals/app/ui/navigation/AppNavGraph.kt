package com.salesgoals.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.salesgoals.app.ui.screens.HomeScreen
import com.salesgoals.app.ui.screens.advisor.AdvisorDashboardScreen
import com.salesgoals.app.ui.screens.advisor.AdvisorViewModel
import com.salesgoals.app.ui.screens.advisor.DailyEntryScreen
import com.salesgoals.app.ui.screens.advisor.HistoryScreen
import com.salesgoals.app.ui.screens.advisor.ImportBudgetScreen
import com.salesgoals.app.ui.screens.common.DaysConfigScreen
import com.salesgoals.app.ui.screens.manager.BudgetSetupScreen
import com.salesgoals.app.ui.screens.manager.DistributionScreen
import com.salesgoals.app.ui.screens.manager.ManagerDashboardScreen

object Routes {
    const val HOME = "home"
    const val ADVISOR_DASH = "advisor/dashboard"
    const val ADVISOR_DAILY = "advisor/daily"
    const val ADVISOR_DAILY_EDIT = "advisor/daily/{date}"
    const val ADVISOR_HISTORY = "advisor/history"
    const val ADVISOR_IMPORT = "advisor/import"
    const val ADVISOR_DAYS = "advisor/days"

    const val MANAGER_DASH = "manager/dashboard"
    const val MANAGER_SETUP = "manager/setup"
    const val MANAGER_DIST = "manager/distribution"
    const val MANAGER_DAYS = "manager/days"
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onPickAdvisor = { navController.navigate(Routes.ADVISOR_DASH) },
                onPickManager = { navController.navigate(Routes.MANAGER_DASH) }
            )
        }

        // ASESOR
        composable(Routes.ADVISOR_DASH) {
            AdvisorDashboardScreen(
                onOpenDailyEntry = { navController.navigate(Routes.ADVISOR_DAILY) },
                onOpenHistory = { navController.navigate(Routes.ADVISOR_HISTORY) },
                onOpenImport = { navController.navigate(Routes.ADVISOR_IMPORT) },
                onOpenDaysConfig = { navController.navigate(Routes.ADVISOR_DAYS) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ADVISOR_DAILY) {
            DailyEntryScreen(date = null, onBack = { navController.popBackStack() })
        }
        composable(Routes.ADVISOR_DAILY_EDIT) { entry ->
            val date = entry.arguments?.getString("date")
            DailyEntryScreen(date = date, onBack = { navController.popBackStack() })
        }
        composable(Routes.ADVISOR_HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onEdit = { date -> navController.navigate("advisor/daily/$date") }
            )
        }
        composable(Routes.ADVISOR_IMPORT) {
            ImportBudgetScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
        composable(Routes.ADVISOR_DAYS) {
            val vm: AdvisorViewModel = viewModel(factory = AdvisorViewModel.Factory)
            val state by vm.state.collectAsStateWithLifecycle()
            DaysConfigScreen(
                initialDays = state.workingDays,
                onSave = {
                    vm.updateWorkingDays(it)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // GERENCIA
        composable(Routes.MANAGER_DASH) {
            ManagerDashboardScreen(
                onBack = { navController.popBackStack() },
                onOpenSetup = { navController.navigate(Routes.MANAGER_SETUP) },
                onOpenDistribution = { navController.navigate(Routes.MANAGER_DIST) }
            )
        }
        composable(Routes.MANAGER_SETUP) {
            BudgetSetupScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(Routes.MANAGER_DIST) {
                        popUpTo(Routes.MANAGER_DASH)
                    }
                }
            )
        }
        composable(Routes.MANAGER_DIST) {
            DistributionScreen(onBack = { navController.popBackStack() })
        }
    }
}
