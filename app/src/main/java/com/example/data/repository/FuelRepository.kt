package com.example.data.repository

import android.content.Context
import com.example.data.local.FuelDatabase
import com.example.data.model.FuelEntry
import com.example.data.model.Household
import com.example.data.model.Vehicle
import com.example.data.model.VehicleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FuelRepository(
    private val database: FuelDatabase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val allVehicles: Flow<List<Vehicle>> = database.vehicleDao().getAllActiveVehicles()
    val allEntries: Flow<List<FuelEntry>> = database.fuelEntryDao().getAllEntries()
    val householdFlow: Flow<Household?> = database.householdDao().getHouseholdFlow()

    init {
        // Ensure initial data is populated if empty
        coroutineScope.launch {
            checkAndSeedInitialData()
        }
    }

    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        val vehicleCount = database.vehicleDao().getCount()
        if (vehicleCount == 0) {
            FuelDatabase.populateInitialData(database)
        }
    }

    fun getEntriesForVehicle(vehicleId: String): Flow<List<FuelEntry>> {
        return database.fuelEntryDao().getEntriesForVehicle(vehicleId)
    }

    fun getVehicleFlow(vehicleId: String): Flow<Vehicle?> {
        return database.vehicleDao().getVehicleFlow(vehicleId)
    }

    suspend fun getVehicleById(vehicleId: String): Vehicle? = withContext(Dispatchers.IO) {
        database.vehicleDao().getVehicleById(vehicleId)
    }

    suspend fun getLatestEntryForVehicle(vehicleId: String): FuelEntry? = withContext(Dispatchers.IO) {
        database.fuelEntryDao().getLatestEntryForVehicle(vehicleId)
    }

    suspend fun insertVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        database.vehicleDao().insertVehicle(vehicle.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        database.vehicleDao().updateVehicle(vehicle.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        database.vehicleDao().deleteVehicle(vehicle)
        database.fuelEntryDao().deleteEntriesForVehicle(vehicle.id)
    }

    suspend fun insertFuelEntry(entry: FuelEntry) = withContext(Dispatchers.IO) {
        database.fuelEntryDao().insertEntry(entry.copy(updatedAt = System.currentTimeMillis()))
        updateHouseholdSyncTimestamp()
    }

    suspend fun updateFuelEntry(entry: FuelEntry) = withContext(Dispatchers.IO) {
        database.fuelEntryDao().updateEntry(entry.copy(updatedAt = System.currentTimeMillis()))
        updateHouseholdSyncTimestamp()
    }

    suspend fun deleteFuelEntry(entry: FuelEntry) = withContext(Dispatchers.IO) {
        database.fuelEntryDao().deleteEntry(entry)
        updateHouseholdSyncTimestamp()
    }

    suspend fun updateHousehold(household: Household) = withContext(Dispatchers.IO) {
        database.householdDao().insertHousehold(household.copy(lastSyncedAt = System.currentTimeMillis()))
    }

    suspend fun getHousehold(): Household? = withContext(Dispatchers.IO) {
        database.householdDao().getHousehold()
    }

    private suspend fun updateHouseholdSyncTimestamp() {
        val current = database.householdDao().getHousehold()
        if (current != null) {
            database.householdDao().updateHousehold(current.copy(lastSyncedAt = System.currentTimeMillis()))
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FuelRepository? = null

        fun getInstance(context: Context): FuelRepository {
            return INSTANCE ?: synchronized(this) {
                val db = FuelDatabase.getDatabase(context)
                val instance = FuelRepository(db)
                INSTANCE = instance
                instance
            }
        }
    }
}
