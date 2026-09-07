package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FuelCalculations
import com.example.data.model.FuelEntry
import com.example.data.model.ProcessedFuelEntry
import com.example.data.model.Vehicle
import com.example.data.model.VehicleStats
import com.example.ui.components.MetricMiniBlock
import com.example.ui.components.VehicleIconHelper
import com.example.ui.dialogs.AddEditVehicleDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class HistoryFilter(val label: String) {
    ALL("All Records"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicle: Vehicle,
    allEntries: List<FuelEntry>,
    onNavigateBack: () -> Unit,
    onAddEntryForVehicle: (vehicleId: String) -> Unit,
    onEditEntry: (FuelEntry) -> Unit,
    onDeleteEntry: (FuelEntry) -> Unit,
    onUpdateVehicle: (Vehicle) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit
) {
    val context = LocalContext.current
    var showEditVehicleDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var selectedEntryForDetail by remember { mutableStateOf<ProcessedFuelEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<FuelEntry?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val vehicleEntries = remember(vehicle, allEntries) {
        allEntries.filter { it.vehicleId == vehicle.id }
    }

    val processedEntries = remember(vehicle, vehicleEntries) {
        FuelCalculations.processEntriesForVehicle(vehicle, vehicleEntries)
    }

    val stats = remember(vehicle, allEntries) {
        FuelCalculations.calculateVehicleStats(vehicle, allEntries)
    }

    val filteredEntries = remember(processedEntries, selectedFilter) {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)

        when (selectedFilter) {
            HistoryFilter.ALL -> processedEntries
            HistoryFilter.THIS_MONTH -> processedEntries.filter {
                cal.timeInMillis = it.entry.date
                cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
            }
            HistoryFilter.LAST_MONTH -> {
                val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                val targetYear = lastMonthCal.get(Calendar.YEAR)
                val targetMonth = lastMonthCal.get(Calendar.MONTH)
                processedEntries.filter {
                    cal.timeInMillis = it.entry.date
                    cal.get(Calendar.YEAR) == targetYear && cal.get(Calendar.MONTH) == targetMonth
                }
            }
        }
    }

    val accentColor = Color(vehicle.colorHex)

    // Delete Entry Confirmation
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Fuel Entry?") },
            text = { Text("Are you sure you want to delete this fuel record? Odometer and mileage calculations will adjust automatically.") },
            confirmButton = {
                Button(
                    onClick = {
                        entryToDelete?.let { onDeleteEntry(it) }
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Detail Bottom Sheet
    if (selectedEntryForDetail != null) {
        val item = selectedEntryForDetail!!
        val e = item.entry
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())

        ModalBottomSheet(
            onDismissRequest = { selectedEntryForDetail = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Fuel Fill-Up Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateFormatter.format(Date(e.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "₹${String.format(Locale.US, "%,.0f", e.amountPaid)}",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }
                }

                // Metric Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailCardItem(
                        label = "Fuel Quantity",
                        value = "${e.fuelQuantity} L",
                        modifier = Modifier.weight(1f)
                    )
                    DetailCardItem(
                        label = "Odometer",
                        value = "${String.format(Locale.US, "%,.0f", e.odometerReading)} km",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailCardItem(
                        label = "Distance Travelled",
                        value = if (item.distanceTravelled > 0) "+${String.format(Locale.US, "%.1f", item.distanceTravelled)} km" else "--",
                        modifier = Modifier.weight(1f)
                    )
                    DetailCardItem(
                        label = "Trip Mileage",
                        value = if (item.mileage > 0) "${String.format(Locale.US, "%.2f", item.mileage)} km/L" else "--",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailCardItem(
                        label = "Cost Per KM",
                        value = if (item.costPerKm > 0) "₹${String.format(Locale.US, "%.2f", item.costPerKm)} /km" else "--",
                        modifier = Modifier.weight(1f)
                    )
                    DetailCardItem(
                        label = "Price Per Liter",
                        value = "₹${String.format(Locale.US, "%.2f", e.pricePerLiter)} /L",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (e.petrolPumpName.isNotBlank()) {
                    DetailCardItem(
                        label = "Petrol Pump Station",
                        value = e.petrolPumpName,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (e.notes.isNotBlank()) {
                    DetailCardItem(
                        label = "Notes",
                        value = e.notes,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                DetailCardItem(
                    label = "Recorded By",
                    value = e.addedByUserName,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val toEdit = selectedEntryForDetail?.entry
                            selectedEntryForDetail = null
                            if (toEdit != null) onEditEntry(toEdit)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit")
                    }

                    OutlinedButton(
                        onClick = {
                            entryToDelete = selectedEntryForDetail?.entry
                            selectedEntryForDetail = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = vehicle.name,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${vehicle.type.displayName} • ${vehicle.registrationNumber.ifBlank { vehicle.fuelType }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Vehicle Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Vehicle Details") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showEditVehicleDialog = true
                            }
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
                onClick = { onAddEntryForVehicle(vehicle.id) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Fill-Up") },
                containerColor = accentColor,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_vehicle_fillup_fab")
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
            // 1. Vehicle Hero Card with Month vs All-Time Stats
            item {
                VehicleMetricsHeroCard(
                    stats = stats,
                    accentColor = accentColor
                )
            }

            // 2. Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fuel History (${filteredEntries.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HistoryFilter.values().forEach { filter ->
                            val isSelected = filter == selectedFilter
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedFilter = filter },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = filter.label,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Entries List
            if (filteredEntries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Fuel Records Found",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Add Fill-Up' to record your first petrol/diesel purchase.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredEntries, key = { it.entry.id }) { item ->
                    FuelEntryHistoryCard(
                        item = item,
                        accentColor = accentColor,
                        onClick = { selectedEntryForDetail = item },
                        onEdit = { onEditEntry(item.entry) },
                        onDelete = { entryToDelete = item.entry }
                    )
                }
            }

            // Bottom Spacing for FAB
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    if (showEditVehicleDialog) {
        AddEditVehicleDialog(
            vehicleToEdit = vehicle,
            onDismiss = { showEditVehicleDialog = false },
            onSave = { name, type, regNo, fuelType, initialOdo, colorHex ->
                onUpdateVehicle(
                    vehicle.copy(
                        name = name,
                        type = type,
                        registrationNumber = regNo,
                        fuelType = fuelType,
                        initialOdometer = initialOdo,
                        colorHex = colorHex
                    )
                )
            },
            onDelete = {
                onDeleteVehicle(it)
                onNavigateBack()
            }
        )
    }
}

@Composable
fun VehicleMetricsHeroCard(
    stats: VehicleStats,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                .padding(18.dp)
        ) {
            // Month Summary Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Month Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "₹${String.format(Locale.US, "%,.0f", stats.currentMonthExpense)}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Month 4-grid stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniBlock(
                    label = "Month Fuel",
                    value = "${String.format(Locale.US, "%.1f", stats.currentMonthFuel)} L",
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBlock(
                    label = "Month Distance",
                    value = "${String.format(Locale.US, "%.0f", stats.currentMonthDistance)} km",
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBlock(
                    label = "Month Mileage",
                    value = if (stats.currentMonthMileage > 0) "${String.format(Locale.US, "%.1f", stats.currentMonthMileage)} km/L" else "--",
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBlock(
                    label = "Cost / KM",
                    value = if (stats.currentMonthCostPerKm > 0) "₹${String.format(Locale.US, "%.1f", stats.currentMonthCostPerKm)}" else "--",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // All-Time Summary Header & Stats
            Text(
                text = "All-Time Vehicle Performance",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Total Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "₹${String.format(Locale.US, "%,.0f", stats.totalExpense)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "Total Fuel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${String.format(Locale.US, "%.1f", stats.totalFuel)} L", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "Total Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${String.format(Locale.US, "%,.0f", stats.totalDistance)} km", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Avg Mileage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (stats.averageMileage > 0) "${String.format(Locale.US, "%.1f", stats.averageMileage)} km/L" else "--",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun FuelEntryHistoryCard(
    item: ProcessedFuelEntry,
    accentColor: Color,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val e = item.entry
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("entry_card_${e.id}"),
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
                .padding(14.dp)
        ) {
            // Top Row: Date, Added By, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(e.date)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = e.addedByUserName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "₹${String.format(Locale.US, "%,.0f", e.amountPaid)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row: Liters, Odometer, Distance, Mileage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Fuel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(text = "${e.fuelQuantity} L", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                Column {
                    Text(text = "Odometer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(text = "${String.format(Locale.US, "%,.0f", e.odometerReading)} km", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                Column {
                    Text(text = "Trip Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(
                        text = if (item.distanceTravelled > 0) "+${String.format(Locale.US, "%.0f", item.distanceTravelled)} km" else "--",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.distanceTravelled > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Trip Efficiency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(
                        text = if (item.mileage > 0) "${String.format(Locale.US, "%.1f", item.mileage)} km/L" else "--",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.mileage > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (e.petrolPumpName.isNotBlank() || e.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (e.petrolPumpName.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = e.petrolPumpName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    if (e.notes.isNotBlank()) {
                        if (e.petrolPumpName.isNotBlank()) {
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = e.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailCardItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
