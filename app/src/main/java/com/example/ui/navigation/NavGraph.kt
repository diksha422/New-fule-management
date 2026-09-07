package com.example.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.FuelEntry
import com.example.data.model.Vehicle
import com.example.ui.dialogs.HouseholdSyncDialog
import com.example.ui.screens.AddEditFuelEntryScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.VehicleDetailScreen
import com.example.ui.viewmodel.FuelViewModel
import kotlinx.coroutines.flow.collectLatest

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Vehicles")
    object Reports : Screen("reports", "Reports")
    object AddFuelEntry : Screen("add_fuel_entry?vehicleId={vehicleId}&entryId={entryId}", "Add Fuel") {
        fun createRoute(vehicleId: String? = null, entryId: String? = null): String {
            val vParam = if (vehicleId != null) "vehicleId=$vehicleId" else ""
            val eParam = if (entryId != null) "entryId=$entryId" else ""
            val params = listOf(vParam, eParam).filter { it.isNotBlank() }.joinToString("&")
            return if (params.isNotBlank()) "add_fuel_entry?$params" else "add_fuel_entry"
        }
    }
    object VehicleDetail : Screen("vehicle_detail/{vehicleId}", "Vehicle Details") {
        fun createRoute(vehicleId: String): String = "vehicle_detail/$vehicleId"
    }
}

@Composable
fun MainAppNavigation(
    viewModel: FuelViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val vehicles by viewModel.vehicles.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()
    val household by viewModel.household.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val vehicleStatsList by viewModel.vehicleStatsList.collectAsState()
    val monthlyReport by viewModel.currentMonthlyReport.collectAsState()
    val selectedReportYear by viewModel.selectedReportYear.collectAsState()
    val selectedReportMonth by viewModel.selectedReportMonth.collectAsState()

    var showHouseholdDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(Screen.Dashboard.route, Screen.Reports.route)

    fun handleExportExcel() {
        try {
            val intent = viewModel.getExcelExportIntent(context)
            context.startActivity(android.content.Intent.createChooser(intent, "Share Excel Fuel Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun handleExportCsv() {
        try {
            val intent = viewModel.getCsvExportIntent(context)
            context.startActivity(android.content.Intent.createChooser(intent, "Share CSV Fuel Data"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    val navItemColors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (currentRoute == Screen.Dashboard.route) Icons.Filled.DirectionsCar else Icons.Outlined.DirectionsCar,
                                contentDescription = "Vehicles"
                            )
                        },
                        label = { Text("Vehicles") },
                        selected = currentRoute == Screen.Dashboard.route,
                        colors = navItemColors,
                        onClick = {
                            if (currentRoute != Screen.Dashboard.route) {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (currentRoute == Screen.Reports.route) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                contentDescription = "Reports"
                            )
                        },
                        label = { Text("Reports") },
                        selected = currentRoute == Screen.Reports.route,
                        colors = navItemColors,
                        onClick = {
                            if (currentRoute != Screen.Reports.route) {
                                navController.navigate(Screen.Reports.route) {
                                    popUpTo(Screen.Dashboard.route)
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Outlined.Group,
                                contentDescription = "Household"
                            )
                        },
                        label = { Text("2-User Sync") },
                        selected = false,
                        colors = navItemColors,
                        onClick = {
                            showHouseholdDialog = true
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(250)) }
        ) {
            // 1. Dashboard Screen
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    vehicles = vehicles,
                    vehicleStatsList = vehicleStatsList,
                    household = household,
                    syncStatus = syncStatus,
                    onVehicleClick = { vehicle ->
                        navController.navigate(Screen.VehicleDetail.createRoute(vehicle.id))
                    },
                    onAddFuelClick = {
                        navController.navigate(Screen.AddFuelEntry.createRoute())
                    },
                    onNavigateToReports = {
                        navController.navigate(Screen.Reports.route)
                    },
                    onAddVehicle = { name, type, regNo, fuelType, initialOdo, colorHex ->
                        viewModel.addVehicle(name, type, regNo, fuelType, initialOdo, colorHex)
                    },
                    onSwitchUser = { userKey ->
                        viewModel.switchActiveUser(userKey)
                    },
                    onUpdateHousehold = { name, u1, u2, code ->
                        viewModel.updateHouseholdProfile(name, u1, u2, code)
                    },
                    onJoinHousehold = { code ->
                        viewModel.joinHouseholdWithCode(code)
                    },
                    onSyncNow = {
                        viewModel.triggerManualSync()
                    },
                    onExportExcel = { handleExportExcel() }
                )
            }

            // 2. Reports Screen
            composable(Screen.Reports.route) {
                ReportsScreen(
                    monthlyReport = monthlyReport,
                    selectedYear = selectedReportYear,
                    selectedMonth = selectedReportMonth,
                    onMonthChange = { y, m -> viewModel.setReportMonth(y, m) },
                    onExportExcel = { handleExportExcel() },
                    onExportCsv = { handleExportCsv() }
                )
            }

            // 3. Vehicle Details Screen
            composable(
                route = Screen.VehicleDetail.route,
                arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
                val vehicle = vehicles.find { it.id == vehicleId }

                if (vehicle != null) {
                    VehicleDetailScreen(
                        vehicle = vehicle,
                        allEntries = allEntries,
                        onNavigateBack = { navController.popBackStack() },
                        onAddEntryForVehicle = { vId ->
                            navController.navigate(Screen.AddFuelEntry.createRoute(vehicleId = vId))
                        },
                        onEditEntry = { entry ->
                            navController.navigate(Screen.AddFuelEntry.createRoute(vehicleId = entry.vehicleId, entryId = entry.id))
                        },
                        onDeleteEntry = { entry ->
                            viewModel.deleteFuelEntry(entry)
                        },
                        onUpdateVehicle = { updatedVehicle ->
                            viewModel.updateVehicle(updatedVehicle)
                        },
                        onDeleteVehicle = { vToDelete ->
                            viewModel.deleteVehicle(vToDelete)
                        }
                    )
                }
            }

            // 4. Add / Edit Fuel Entry Screen
            composable(
                route = Screen.AddFuelEntry.route,
                arguments = listOf(
                    navArgument("vehicleId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("entryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val vehicleId = backStackEntry.arguments?.getString("vehicleId")
                val entryId = backStackEntry.arguments?.getString("entryId")
                val entryToEdit = allEntries.find { it.id == entryId }

                AddEditFuelEntryScreen(
                    vehicles = vehicles,
                    initialVehicleId = vehicleId,
                    entryToEdit = entryToEdit,
                    household = household,
                    allEntries = allEntries,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveEntry = { vId, amt, qty, odo, pump, notes, date ->
                        viewModel.addFuelEntry(vId, amt, qty, odo, pump, notes, date)
                    },
                    onUpdateEntry = { entry ->
                        viewModel.updateFuelEntry(entry)
                    },
                    onDeleteEntry = { entry ->
                        viewModel.deleteFuelEntry(entry)
                    }
                )
            }
        }
    }

    if (showHouseholdDialog) {
        HouseholdSyncDialog(
            household = household,
            syncStatus = syncStatus,
            onDismiss = { showHouseholdDialog = false },
            onSwitchUser = { userKey -> viewModel.switchActiveUser(userKey) },
            onUpdateHousehold = { name, u1, u2, code -> viewModel.updateHouseholdProfile(name, u1, u2, code) },
            onJoinHousehold = { code -> viewModel.joinHouseholdWithCode(code) },
            onSyncNow = { viewModel.triggerManualSync() }
        )
    }
}
