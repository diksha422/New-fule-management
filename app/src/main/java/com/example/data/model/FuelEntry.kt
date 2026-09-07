package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "fuel_entries")
data class FuelEntry(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val householdId: String = "household_default",
    val date: Long = System.currentTimeMillis(),
    val amountPaid: Double, // in Rupees ₹
    val fuelQuantity: Double, // in Liters
    val odometerReading: Double, // in km
    val pricePerLiter: Double = if (fuelQuantity > 0) amountPaid / fuelQuantity else 0.0,
    val petrolPumpName: String = "",
    val notes: String = "",
    val addedByUserName: String = "User 1",
    val addedByUserId: String = "user_1",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
)
