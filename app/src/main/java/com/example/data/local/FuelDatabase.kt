package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.FuelEntry
import com.example.data.model.Household
import com.example.data.model.Vehicle
import com.example.data.model.VehicleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromVehicleType(value: VehicleType): String = value.name

    @TypeConverter
    fun toVehicleType(value: String): VehicleType = try {
        VehicleType.valueOf(value)
    } catch (e: Exception) {
        VehicleType.OTHER
    }
}

@Database(
    entities = [Vehicle::class, FuelEntry::class, Household::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FuelDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao
    abstract fun fuelEntryDao(): FuelEntryDao
    abstract fun householdDao(): HouseholdDao

    companion object {
        @Volatile
        private var INSTANCE: FuelDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): FuelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FuelDatabase::class.java,
                    "fuel_management_db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: FuelDatabase) {
            val vehicleDao = database.vehicleDao()
            val entryDao = database.fuelEntryDao()
            val householdDao = database.householdDao()

            // 1. Initial Household
            val household = Household(
                id = "household_default",
                name = "Sharma Household",
                accessCode = "HH-7842",
                currentUserName = "Rahul (User 1)",
                partnerUserName = "Priya (User 2)",
                activeUserKey = "user_1",
                lastSyncedAt = System.currentTimeMillis()
            )
            householdDao.insertHousehold(household)

            // 2. Initial 3 Vehicles specified by user
            val activaId = "veh_activa"
            val sp125Id = "veh_sp125"
            val marutiK10Id = "veh_maruti_k10"

            val initialVehicles = listOf(
                Vehicle(
                    id = activaId,
                    householdId = "household_default",
                    name = "Activa",
                    type = VehicleType.SCOOTER,
                    registrationNumber = "MH 12 AB 4521",
                    fuelType = "Petrol",
                    initialOdometer = 12400.0,
                    colorHex = 0xFF00897B // Teal
                ),
                Vehicle(
                    id = sp125Id,
                    householdId = "household_default",
                    name = "Honda SP125",
                    type = VehicleType.MOTORCYCLE,
                    registrationNumber = "MH 12 CD 7890",
                    fuelType = "Petrol",
                    initialOdometer = 8100.0,
                    colorHex = 0xFFE65100 // Amber / Orange
                ),
                Vehicle(
                    id = marutiK10Id,
                    householdId = "household_default",
                    name = "Maruti K10",
                    type = VehicleType.CAR,
                    registrationNumber = "MH 12 EF 1234",
                    fuelType = "Petrol",
                    initialOdometer = 34500.0,
                    colorHex = 0xFF283593 // Indigo / Deep Blue
                )
            )

            vehicleDao.insertVehicles(initialVehicles)

            // 3. Initial Sample Entries to give the users realistic numbers right away
            val now = Calendar.getInstance()

            fun getPastTimestamp(daysAgo: Int): Long {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, -daysAgo)
                return c.timeInMillis
            }

            val initialEntries = listOf(
                // Activa Entries
                FuelEntry(
                    id = UUID.randomUUID().toString(),
                    vehicleId = activaId,
                    date = getPastTimestamp(24),
                    amountPaid = 480.0,
                    fuelQuantity = 4.6,
                    odometerReading = 12590.0,
                    petrolPumpName = "IndianOil - City Center",
                    notes = "Monthly commute fill",
                    addedByUserName = "Rahul (User 1)",
                    addedByUserId = "user_1"
                ),
                FuelEntry(
                    id = UUID.randomUUID().toString(),
                    vehicleId = activaId,
                    date = getPastTimestamp(14),
                    amountPaid = 520.0,
                    fuelQuantity = 5.0,
                    odometerReading = 12810.0,
                    petrolPumpName = "Bharat Petroleum",
                    notes = "Full tank",
                    addedByUserName = "Priya (User 2)",
                    addedByUserId = "user_2"
                ),
                FuelEntry(
                    id = UUID.randomUUID().toString(),
                    vehicleId = activaId,
                    date = getPastTimestamp(3),
                    amountPaid = 490.0,
                    fuelQuantity = 4.7,
                    odometerReading = 13020.0,
                    petrolPumpName = "HP Petrol Pump",
                    notes = "Weekly grocery & office run",
                    addedByUserName = "Rahul (User 1)",
                    addedByUserId = "user_1"
                ),

                // Honda SP125 Entries (High mileage commuter)
                FuelEntry(
                    id = UUID.randomUUID().toString(),
                    vehicleId = sp125Id,
                    date = getPastTimestamp(28),
                    amountPaid = 800.0,
                    fuelQuantity = 7.7,
                    odometerReading = 8560.0,
                    petrolPumpName = "Shell Fuel Stop",
                    notes = "Premium petrol test",
                    addedByUserName = "Rahul (User 1)",
                    addedByUserId = "user_1"
                ),
                FuelEntry(
                    id = UUID.randomUUID().toString(),
                    vehicleId = sp125Id,
                    date = getPastTimestamp(12),
                    amountPaid = 850.0,
                    fuelQuantity = 8.1,
                    odometerReading = 9070.0,
                    petrolPumpName = "IndianOil Highway",
                    notes = "Highway ride to Pune outskirts",
                    addedByUserName = "Rahul (User 1)",
                    addedByUserId = "user_1"
                ),
                FuelEntry(
                    id = UUID.randomUUID().toString(),
                    vehicleId = sp125Id,
                    date = getPastTimestamp(2),
                    amountPaid = 830.0,
                    fuelQuantity = 8.0,
                    odometerReading = 9580.0,
                    petrolPumpName = "HP AutoCare",
                    notes = "Regular tank fill",
                    addedByUserName = "Priya (User 2)",
                    addedByUserId = "user_2"
                ),

                // Maruti K10 Entries (Car)
                FuelEntry(
                    id = UUID.randomUUID().toString(),
                    vehicleId = marutiK10Id,
                    date = getPastTimestamp(25),
                    amountPaid = 2600.0,
                    fuelQuantity = 25.0,
                    odometerReading = 35020.0,
                    petrolPumpName = "Jio-bp Express",
                    notes = "Weekend family trip fill",
                    addedByUserName = "Rahul (User 1)",
                    addedByUserId = "user_1"
                ),
                FuelEntry(
                    id = UUID.randomUUID().toString(),
                    vehicleId = marutiK10Id,
                    date = getPastTimestamp(10),
                    amountPaid = 2800.0,
                    fuelQuantity = 26.8,
                    odometerReading = 35590.0,
                    petrolPumpName = "Bharat Petroleum City",
                    notes = "Office commute + market trips",
                    addedByUserName = "Priya (User 2)",
                    addedByUserId = "user_2"
                ),
                FuelEntry(
                    id = UUID.randomUUID().toString(),
                    vehicleId = marutiK10Id,
                    date = getPastTimestamp(1),
                    amountPaid = 2500.0,
                    fuelQuantity = 24.0,
                    odometerReading = 36110.0,
                    petrolPumpName = "IndianOil AutoCare",
                    notes = "Full tank refill",
                    addedByUserName = "Rahul (User 1)",
                    addedByUserId = "user_1"
                )
            )

            entryDao.insertEntries(initialEntries)
        }
    }
}
