package com.example.apptrack.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.apptrack.data.AppUsageInfo
import com.example.apptrack.ui.screens.AppDetailScreen
import com.example.apptrack.ui.screens.DashboardScreen
import com.example.apptrack.ui.screens.PermissionScreen
import com.example.apptrack.ui.viewmodel.UsageStatsViewModel

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Permission : Screen("permission")
    data class AppDetail(val packageName: String = "") : Screen("app_detail/{packageName}") {
        fun createRoute(packageName: String) = "app_detail/$packageName"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: UsageStatsViewModel
) {
    NavHost(
        navController = navController,
        startDestination = if (viewModel.hasPermission) {
            Screen.Dashboard.route
        } else {
            Screen.Permission.route
        }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onAppClick = { app ->
                    navController.navigate(Screen.AppDetail("").createRoute(app.packageName))
                }
            )
        }

        composable(Screen.Permission.route) {
            PermissionScreen(
                onPermissionGranted = {
                    // Check permission again when user returns from settings
                    if (viewModel.hasPermission) {
                        viewModel.refresh()
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Permission.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.AppDetail("").route) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            val app = viewModel.usageStats.value?.apps?.find { it.packageName == packageName }
            
            if (app != null) {
                AppDetailScreen(
                    app = app,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
