package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "household_profiles")
data class Household(
    @PrimaryKey
    val id: String = "household_default",
    val name: String = "Sharma Household",
    val accessCode: String = "HH-8429",
    val currentUserName: String = "Rahul (User 1)",
    val partnerUserName: String = "Priya (User 2)",
    val activeUserKey: String = "user_1", // "user_1" or "user_2"
    val lastSyncedAt: Long = System.currentTimeMillis()
)

data class HouseholdMember(
    val id: String,
    val name: String,
    val role: String = "Member"
)
