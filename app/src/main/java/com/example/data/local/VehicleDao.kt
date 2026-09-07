package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getAllActiveVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicleById(id: String): Vehicle?

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    fun getVehicleFlow(id: String): Flow<Vehicle?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<Vehicle>)

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Query("UPDATE vehicles SET isArchived = 1 WHERE id = :id")
    suspend fun archiveVehicle(id: String)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun getCount(): Int
}
