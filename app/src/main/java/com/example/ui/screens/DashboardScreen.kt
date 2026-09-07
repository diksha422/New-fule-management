package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.data.model.Household
import com.example.data.model.Vehicle
import com.example.data.model.VehicleStats
import com.example.data.sync.SyncStatus
import com.example.ui.components.HeroSummaryCard
import com.example.ui.components.StatPill
import com.example.ui.components.VehicleIconHelper
import com.example.ui.dialogs.AddEditVehicleDialog
import com.example.ui.dialogs.HouseholdSyncDialog
import com.example.ui.theme.FabAccentColor
import com.example.ui.theme.FabIconColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vehicles: List<Vehicle>,
    vehicleStatsList: List<VehicleStats>,
    household: Household?,
    syncStatus: SyncStatus,
    onVehicleClick: (Vehicle) -> Unit,
    onAddFuelClick: () -> Unit,
    onNavigateToReports: () -> Unit,
    onAddVehicle: (name: String, type: com.example.data.model.VehicleType, regNo: String, fuelType: String, initialOdo: Double, colorHex: Long) -> Unit,
    onSwitchUser: (String) -> Unit,
    onUpdateHousehold: (name: String, user1: String, user2: String, code: String) -> Unit,
    onJoinHousehold: (code: String) -> Unit,
    onSyncNow: () -> Unit,
    onExportExcel: () -> Unit
) {
    val context = LocalContext.current
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showHouseholdDialog by remember { mutableStateOf(false) }

    val monthName = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    // Combined Household Metrics for this month
    val totalHouseholdExpenseThisMonth = vehicleStatsList.sumOf { it.currentMonthExpense }
    val totalHouseholdFuelThisMonth = vehicleStatsList.sumOf { it.currentMonthFuel }
    val totalHouseholdDistanceThisMonth = vehicleStatsList.sumOf { it.currentMonthDistance }
    val totalHouseholdEntriesCount = vehicleStatsList.sumOf { it.currentMonthEntriesCount }
    val overallEfficiency = if (totalHouseholdFuelThisMonth > 0 && totalHouseholdDistanceThisMonth > 0) {
        totalHouseholdDistanceThisMonth / totalHouseholdFuelThisMonth
    } else 0.0

    val activeUserName = if (household?.activeUserKey == "user_2") {
        household.partnerUserName
    } else {
        household?.currentUserName ?: "User 1"
    }

    val userInitials = activeUserName.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase(Locale.US).ifEmpty { "U" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "HOUSEHOLD FLEET",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "FuelTrack",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Export Excel Quick Action
                    IconButton(
                        onClick = onExportExcel,
                        modifier = Modifier.testTag("export_excel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export Excel Report",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // User Initials Avatar
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape)
                            .clickable { showHouseholdDialog = true }
                            .testTag("household_sync_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitials,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddFuelClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = FabIconColor) },
                text = { Text("Add Fuel Entry", fontWeight = FontWeight.SemiBold, color = FabIconColor) },
                containerColor = FabAccentColor,
                contentColor = FabIconColor,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_fuel_entry_fab")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Household 2-User Banner & Quick User Switch Bar
            item {
                HouseholdSyncStatusBar(
                    household = household,
                    syncStatus = syncStatus,
                    activeUserName = activeUserName,
                    onClick = { showHouseholdDialog = true }
                )
            }

            // 2. Current Month Overall Summary Card
            item {
                HeroSummaryCard(
                    title = "Household Fuel Summary",
                    subtitle = "$monthName • ${vehicles.size} Vehicles",
                    totalExpense = totalHouseholdExpenseThisMonth,
                    totalFuel = totalHouseholdFuelThisMonth,
                    totalDistance = totalHouseholdDistanceThisMonth,
                    averageMileage = overallEfficiency
                )
            }

            // 3. Vehicles Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Household Vehicles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap a vehicle to view detailed logs & mileage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAddVehicleDialog = true }
                            .testTag("add_vehicle_button"),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Vehicle",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 4. Vehicle Cards
            if (vehicleStatsList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Vehicles Configured",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Add Vehicle' to start tracking Activa, Honda SP125, Maruti K10, etc.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(vehicleStatsList, key = { it.vehicle.id }) { stats ->
                    VehicleDashboardCard(
                        stats = stats,
                        onClick = { onVehicleClick(stats.vehicle) }
                    )
                }
            }

            // Bottom Spacing for FAB
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    if (showAddVehicleDialog) {
        AddEditVehicleDialog(
            onDismiss = { showAddVehicleDialog = false },
            onSave = onAddVehicle
        )
    }

    if (showHouseholdDialog) {
        HouseholdSyncDialog(
            household = household,
            syncStatus = syncStatus,
            onDismiss = { showHouseholdDialog = false },
            onSwitchUser = onSwitchUser,
            onUpdateHousehold = onUpdateHousehold,
            onJoinHousehold = onJoinHousehold,
            onSyncNow = onSyncNow
        )
    }
}

@Composable
fun HouseholdSyncStatusBar(
    household: Household?,
    syncStatus: SyncStatus,
    activeUserName: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("household_status_bar"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeUserName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = household?.accessCode ?: "HH-8429",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Text(
                        text = "2-User Live Sync Active • Tap to switch user",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = "Cloud Connected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun VehicleDashboardCard(
    stats: VehicleStats,
    onClick: () -> Unit
) {
    val vehicle = stats.vehicle
    val naturalBg = VehicleIconHelper.getNaturalBgColor(vehicle.type)
    val naturalFg = VehicleIconHelper.getNaturalFgColor(vehicle.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("vehicle_card_${vehicle.name.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Vehicle Header: Icon, Name, Reg No, Type Badge, and Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(naturalBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = VehicleIconHelper.getIconForType(vehicle.type),
                            contentDescription = vehicle.name,
                            tint = naturalFg,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = vehicle.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${stats.totalEntriesCount} ${if (stats.totalEntriesCount == 1) "entry" else "entries"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            if (vehicle.registrationNumber.isNotBlank()) {
                                Text(
                                    text = " • ${vehicle.registrationNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Current Month Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "This Month's Fuel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%,.0f", stats.currentMonthExpense)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", stats.currentMonthFuel)} L",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Distance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${String.format(Locale.US, "%,.0f", stats.totalDistance)} km",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Odo: ${String.format(Locale.US, "%,.0f", stats.latestOdometer)} km",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Average Mileage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (stats.averageMileage > 0) "${String.format(Locale.US, "%.1f", stats.averageMileage)}" else "--",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (stats.averageMileage > 0) "km / L" else "Need 2+ fills",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
