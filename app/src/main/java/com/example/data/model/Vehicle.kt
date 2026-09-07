package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class VehicleType(val displayName: String) {
    SCOOTER("Scooter"),
    MOTORCYCLE("Motorcycle"),
    CAR("Car"),
    OTHER("Other")
}

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val householdId: String = "household_default",
    val name: String,
    val type: VehicleType,
    val registrationNumber: String = "",
    val fuelType: String = "Petrol",
    val initialOdometer: Double = 0.0,
    val colorHex: Long = 0xFF00897B,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
