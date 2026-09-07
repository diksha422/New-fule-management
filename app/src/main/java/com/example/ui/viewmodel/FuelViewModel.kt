package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.export.ExcelExporter
import com.example.data.model.FuelCalculations
import com.example.data.model.FuelEntry
import com.example.data.model.Household
import com.example.data.model.HouseholdMonthlyReport
import com.example.data.model.ProcessedFuelEntry
import com.example.data.model.Vehicle
import com.example.data.model.VehicleStats
import com.example.data.model.VehicleType
import com.example.data.repository.FuelRepository
import com.example.data.sync.HouseholdSyncManager
import com.example.data.sync.SyncStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class FuelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FuelRepository.getInstance(application)
    private val syncManager = HouseholdSyncManager(repository, viewModelScope)

    val vehicles: StateFlow<List<Vehicle>> = repository.allVehicles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allEntries: StateFlow<List<FuelEntry>> = repository.allEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val household: StateFlow<Household?> = repository.householdFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    // Reports filter state
    private val calendar = Calendar.getInstance()
    private val _selectedReportYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    val selectedReportYear: StateFlow<Int> = _selectedReportYear.asStateFlow()

    private val _selectedReportMonth = MutableStateFlow(calendar.get(Calendar.MONTH))
    val selectedReportMonth: StateFlow<Int> = _selectedReportMonth.asStateFlow()

    // Calculated overall vehicle statistics
    val vehicleStatsList: StateFlow<List<VehicleStats>> = combine(vehicles, allEntries) { vList, eList ->
        vList.map { vehicle ->
            FuelCalculations.calculateVehicleStats(vehicle, eList)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Monthly Report flow
    val currentMonthlyReport: StateFlow<HouseholdMonthlyReport> = combine(
        vehicles,
        allEntries,
        _selectedReportYear,
        _selectedReportMonth
    ) { vList, eList, year, month ->
        FuelCalculations.generateMonthlyReport(vList, eList, year, month)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HouseholdMonthlyReport(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH),
            monthLabel = "",
            totalExpense = 0.0,
            totalFuel = 0.0,
            totalDistance = 0.0,
            overallAverageMileage = 0.0,
            overallCostPerKm = 0.0,
            totalEntriesCount = 0,
            vehicleReports = emptyList()
        )
    )

    fun setReportMonth(year: Int, month: Int) {
        _selectedReportYear.value = year
        _selectedReportMonth.value = month
    }

    fun getProcessedEntriesForVehicle(vehicle: Vehicle): List<ProcessedFuelEntry> {
        val vEntries = allEntries.value.filter { it.vehicleId == vehicle.id }
        return FuelCalculations.processEntriesForVehicle(vehicle, vEntries)
    }

    fun addFuelEntry(
        vehicleId: String,
        amount: Double,
        fuelQuantity: Double,
        odometer: Double,
        petrolPump: String,
        notes: String,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val hh = household.value
            val userName = if (hh?.activeUserKey == "user_2") hh.partnerUserName else (hh?.currentUserName ?: "User 1")
            val userId = hh?.activeUserKey ?: "user_1"

            val entry = FuelEntry(
                vehicleId = vehicleId,
                householdId = hh?.id ?: "household_default",
                date = date,
                amountPaid = amount,
                fuelQuantity = fuelQuantity,
                odometerReading = odometer,
                petrolPumpName = petrolPump.trim(),
                notes = notes.trim(),
                addedByUserName = userName,
                addedByUserId = userId
            )

            repository.insertFuelEntry(entry)
            syncManager.triggerCloudSync()
            _uiMessage.emit("Fuel entry of ₹${String.format(java.util.Locale.US, "%.0f", amount)} added & synced successfully!")
        }
    }

    fun updateFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            repository.updateFuelEntry(entry)
            syncManager.triggerCloudSync()
            _uiMessage.emit("Fuel entry updated & synced!")
        }
    }

    fun deleteFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            repository.deleteFuelEntry(entry)
            syncManager.triggerCloudSync()
            _uiMessage.emit("Fuel entry deleted.")
        }
    }

    fun addVehicle(
        name: String,
        type: VehicleType,
        registrationNumber: String,
        fuelType: String,
        initialOdometer: Double,
        colorHex: Long
    ) {
        viewModelScope.launch {
            val hh = household.value
            val newVehicle = Vehicle(
                householdId = hh?.id ?: "household_default",
                name = name.trim(),
                type = type,
                registrationNumber = registrationNumber.trim().uppercase(),
                fuelType = fuelType.trim(),
                initialOdometer = initialOdometer,
                colorHex = colorHex
            )
            repository.insertVehicle(newVehicle)
            syncManager.triggerCloudSync()
            _uiMessage.emit("Vehicle \"${newVehicle.name}\" added!")
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.updateVehicle(vehicle)
            syncManager.triggerCloudSync()
            _uiMessage.emit("Vehicle \"${vehicle.name}\" updated!")
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
            syncManager.triggerCloudSync()
            _uiMessage.emit("Vehicle \"${vehicle.name}\" removed.")
        }
    }

    fun switchActiveUser(userKey: String) {
        viewModelScope.launch {
            syncManager.switchActiveUser(userKey)
            val name = if (userKey == "user_1") household.value?.currentUserName else household.value?.partnerUserName
            _uiMessage.emit("Active User switched to $name")
        }
    }

    fun updateHouseholdProfile(name: String, user1: String, user2: String, code: String) {
        viewModelScope.launch {
            syncManager.updateHouseholdDetails(name, user1, user2, code)
            _uiMessage.emit("Household settings saved!")
        }
    }

    fun joinHouseholdWithCode(code: String) {
        viewModelScope.launch {
            syncManager.joinHousehold(code)
            _uiMessage.emit("Synced with Household $code!")
        }
    }

    fun triggerManualSync() {
        syncManager.triggerCloudSync()
    }

    fun getExcelExportIntent(context: Context): Intent {
        return ExcelExporter.exportToExcelAndShare(
            context = context,
            household = household.value,
            vehicles = vehicles.value,
            allEntries = allEntries.value
        )
    }

    fun getCsvExportIntent(context: Context): Intent {
        return ExcelExporter.exportToCsvAndShare(
            context = context,
            household = household.value,
            vehicles = vehicles.value,
            allEntries = allEntries.value
        )
    }
}
