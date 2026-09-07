package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FuelEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelEntryDao {
    @Query("SELECT * FROM fuel_entries ORDER BY date DESC, odometerReading DESC")
    fun getAllEntries(): Flow<List<FuelEntry>>

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY date DESC, odometerReading DESC")
    fun getEntriesForVehicle(vehicleId: String): Flow<List<FuelEntry>>

    @Query("SELECT * FROM fuel_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: String): FuelEntry?

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY date DESC, odometerReading DESC LIMIT 1")
    suspend fun getLatestEntryForVehicle(vehicleId: String): FuelEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: FuelEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<FuelEntry>)

    @Update
    suspend fun updateEntry(entry: FuelEntry)

    @Delete
    suspend fun deleteEntry(entry: FuelEntry)

    @Query("DELETE FROM fuel_entries WHERE vehicleId = :vehicleId")
    suspend fun deleteEntriesForVehicle(vehicleId: String)

    @Query("SELECT COUNT(*) FROM fuel_entries")
    suspend fun getCount(): Int
}
