package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Household
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {
    @Query("SELECT * FROM household_profiles LIMIT 1")
    fun getHouseholdFlow(): Flow<Household?>

    @Query("SELECT * FROM household_profiles LIMIT 1")
    suspend fun getHousehold(): Household?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousehold(household: Household)

    @Update
    suspend fun updateHousehold(household: Household)
}
