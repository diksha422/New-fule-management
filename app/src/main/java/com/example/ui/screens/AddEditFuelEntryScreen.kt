package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FuelEntry
import com.example.data.model.Household
import com.example.data.model.Vehicle
import com.example.ui.components.VehicleIconHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFuelEntryScreen(
    vehicles: List<Vehicle>,
    initialVehicleId: String? = null,
    entryToEdit: FuelEntry? = null,
    household: Household?,
    allEntries: List<FuelEntry>,
    onNavigateBack: () -> Unit,
    onSaveEntry: (
        vehicleId: String,
        amount: Double,
        fuelQuantity: Double,
        odometer: Double,
        petrolPump: String,
        notes: String,
        date: Long
    ) -> Unit,
    onUpdateEntry: (FuelEntry) -> Unit,
    onDeleteEntry: ((FuelEntry) -> Unit)? = null
) {
    val context = LocalContext.current

    var selectedVehicleId by remember {
        mutableStateOf(
            entryToEdit?.vehicleId
                ?: initialVehicleId
                ?: vehicles.firstOrNull()?.id
                ?: ""
        )
    }

    var selectedDate by remember {
        mutableLongStateOf(entryToEdit?.date ?: System.currentTimeMillis())
    }

    var amountText by remember {
        mutableStateOf(entryToEdit?.amountPaid?.toString()?.replace(".0", "") ?: "")
    }

    var quantityText by remember {
        mutableStateOf(entryToEdit?.fuelQuantity?.toString()?.replace(".0", "") ?: "")
    }

    var odometerText by remember {
        mutableStateOf(entryToEdit?.odometerReading?.toString()?.replace(".0", "") ?: "")
    }

    var pumpName by remember {
        mutableStateOf(entryToEdit?.petrolPumpName ?: "")
    }

    var notes by remember {
        mutableStateOf(entryToEdit?.notes ?: "")
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val popularPumps = listOf(
        "IndianOil", "HP", "Bharat Petroleum", "Shell", "Jio-bp", "Nayara"
    )

    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }

    // Find previous odometer reading for this vehicle
    val previousOdo by remember(selectedVehicleId, allEntries, entryToEdit) {
        derivedStateOf {
            val v = vehicles.find { it.id == selectedVehicleId }
            val vEntries = allEntries
                .filter { it.vehicleId == selectedVehicleId && (entryToEdit == null || it.id != entryToEdit.id) }
                .sortedByDescending { it.odometerReading }

            vEntries.firstOrNull()?.odometerReading ?: v?.initialOdometer ?: 0.0
        }
    }

    val currentOdoValue = odometerText.toDoubleOrNull() ?: 0.0
    val distanceCalculated = if (currentOdoValue > previousOdo) currentOdoValue - previousOdo else 0.0
    val quantityValue = quantityText.toDoubleOrNull() ?: 0.0
    val amountValue = amountText.toDoubleOrNull() ?: 0.0

    val calculatedMileage = if (quantityValue > 0 && distanceCalculated > 0) {
        distanceCalculated / quantityValue
    } else 0.0

    val pricePerLiter = if (quantityValue > 0 && amountValue > 0) {
        amountValue / quantityValue
    } else 0.0

    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }

    // Date Picker Dialog
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val newCal = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            selectedDate = newCal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (showDeleteConfirm && entryToEdit != null && onDeleteEntry != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Fuel Entry?") },
            text = { Text("Are you sure you want to delete this fuel record? Calculations and monthly stats will be updated.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteEntry(entryToEdit)
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (entryToEdit == null) "Add Fuel Entry" else "Edit Fuel Entry",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (entryToEdit != null && onDeleteEntry != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_entry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Entry",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Vehicle Selector Chips
            Text(
                text = "Select Vehicle",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                vehicles.forEach { vehicle ->
                    val isSelected = vehicle.id == selectedVehicleId
                    val vehicleColor = Color(vehicle.colorHex)

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedVehicleId = vehicle.id }
                            .testTag("select_vehicle_${vehicle.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) vehicleColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, vehicleColor) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = VehicleIconHelper.getIconForType(vehicle.type),
                                contentDescription = null,
                                tint = if (isSelected) vehicleColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = vehicle.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Date Selection Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { datePickerDialog.show() }
                    .testTag("date_picker_button"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Entry Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateFormatter.format(Date(selectedDate)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. Amount & Quantity Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Paid (₹)") },
                    placeholder = { Text("e.g. 500") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("amount_input")
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity (L)") },
                    placeholder = { Text("e.g. 4.8") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quantity_input")
                )
            }

            // Price per liter indicator
            if (pricePerLiter > 0) {
                Text(
                    text = "Calculated Fuel Price: ₹${String.format(Locale.US, "%.2f", pricePerLiter)} / Liter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 4. Odometer Reading & Live Distance Calculation Card
            OutlinedTextField(
                value = odometerText,
                onValueChange = { odometerText = it },
                label = { Text("Current Odometer Reading (km)") },
                placeholder = { Text("e.g. ${String.format(Locale.US, "%.0f", previousOdo + 150)}") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("odometer_input"),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )

            // Dynamic Distance & Mileage Calculation Helper Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Previous Odometer:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.0f", previousOdo)} km",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Distance Travelled:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (distanceCalculated > 0) "+${String.format(Locale.US, "%.1f", distanceCalculated)} km" else "--",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (distanceCalculated > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (calculatedMileage > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Estimated Trip Mileage:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", calculatedMileage)} km/L",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // 5. Petrol Pump Name & Quick Chips
            Column {
                OutlinedTextField(
                    value = pumpName,
                    onValueChange = { pumpName = it },
                    label = { Text("Petrol Pump / Station Name (Optional)") },
                    placeholder = { Text("e.g. IndianOil City Center, HP AutoCare") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pump_name_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(popularPumps) { pump ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { pumpName = pump },
                            shape = RoundedCornerShape(8.dp),
                            color = if (pumpName.contains(pump)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = pump,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pumpName.contains(pump)) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 6. Notes (Optional)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("e.g. Full tank refill, Highway trip, Tyre pressure checked") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_input"),
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 7. Submit / Save Button
            val isFormValid = selectedVehicleId.isNotBlank() &&
                    (amountText.toDoubleOrNull() ?: 0.0) > 0 &&
                    (quantityText.toDoubleOrNull() ?: 0.0) > 0 &&
                    (odometerText.toDoubleOrNull() ?: 0.0) > 0

            Button(
                onClick = {
                    if (isFormValid) {
                        val amt = amountText.toDouble()
                        val qty = quantityText.toDouble()
                        val odo = odometerText.toDouble()

                        if (entryToEdit == null) {
                            onSaveEntry(
                                selectedVehicleId,
                                amt,
                                qty,
                                odo,
                                pumpName,
                                notes,
                                selectedDate
                            )
                        } else {
                            onUpdateEntry(
                                entryToEdit.copy(
                                    vehicleId = selectedVehicleId,
                                    amountPaid = amt,
                                    fuelQuantity = qty,
                                    odometerReading = odo,
                                    petrolPumpName = pumpName.trim(),
                                    notes = notes.trim(),
                                    date = selectedDate
                                )
                            )
                        }
                        onNavigateBack()
                    } else {
                        Toast.makeText(context, "Please fill Amount, Quantity, and Odometer.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_fuel_entry_button"),
                enabled = isFormValid,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (entryToEdit == null) "Save & Synchronize Fuel Entry" else "Update Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
