package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ProcessedFuelEntry(
    val entry: FuelEntry,
    val previousOdometer: Double,
    val distanceTravelled: Double, // Current - Previous (km)
    val mileage: Double, // km / Liters
    val costPerKm: Double // Amount / distanceTravelled (₹/km)
)

data class VehicleStats(
    val vehicle: Vehicle,
    // Current Month Metrics
    val currentMonthExpense: Double = 0.0,
    val currentMonthFuel: Double = 0.0,
    val currentMonthDistance: Double = 0.0,
    val currentMonthMileage: Double = 0.0,
    val currentMonthCostPerKm: Double = 0.0,
    val currentMonthEntriesCount: Int = 0,
    // All-time Metrics
    val totalExpense: Double = 0.0,
    val totalFuel: Double = 0.0,
    val totalDistance: Double = 0.0,
    val averageMileage: Double = 0.0,
    val averageCostPerKm: Double = 0.0,
    val totalEntriesCount: Int = 0,
    val latestOdometer: Double = 0.0,
    val lastEntryDate: Long? = null
)

data class MonthlyVehicleReport(
    val vehicleId: String,
    val vehicleName: String,
    val vehicleType: VehicleType,
    val colorHex: Long,
    val year: Int,
    val month: Int, // 0-11
    val monthLabel: String,
    val totalExpense: Double,
    val totalFuel: Double,
    val totalDistance: Double,
    val averageMileage: Double,
    val costPerKm: Double,
    val totalEntriesCount: Int,
    val entries: List<ProcessedFuelEntry>
)

data class HouseholdMonthlyReport(
    val year: Int,
    val month: Int,
    val monthLabel: String,
    val totalExpense: Double,
    val totalFuel: Double,
    val totalDistance: Double,
    val overallAverageMileage: Double,
    val overallCostPerKm: Double,
    val totalEntriesCount: Int,
    val vehicleReports: List<MonthlyVehicleReport>
)

object FuelCalculations {

    fun processEntriesForVehicle(
        vehicle: Vehicle,
        entries: List<FuelEntry>
    ): List<ProcessedFuelEntry> {
        // Sort chronologically ascending by odometer and date
        val sorted = entries.sortedWith(compareBy<FuelEntry> { it.date }.thenBy { it.odometerReading })
        val result = mutableListOf<ProcessedFuelEntry>()

        var prevOdo = vehicle.initialOdometer

        for (entry in sorted) {
            val dist = if (entry.odometerReading > prevOdo) entry.odometerReading - prevOdo else 0.0
            val mileage = if (entry.fuelQuantity > 0 && dist > 0) dist / entry.fuelQuantity else 0.0
            val costKm = if (dist > 0) entry.amountPaid / dist else 0.0

            result.add(
                ProcessedFuelEntry(
                    entry = entry,
                    previousOdometer = prevOdo,
                    distanceTravelled = dist,
                    mileage = mileage,
                    costPerKm = costKm
                )
            )

            if (entry.odometerReading > prevOdo) {
                prevOdo = entry.odometerReading
            }
        }

        // Return descending for UI presentation (latest first)
        return result.reversed()
    }

    fun calculateVehicleStats(
        vehicle: Vehicle,
        allEntries: List<FuelEntry>,
        currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    ): VehicleStats {
        val vehicleEntries = allEntries.filter { it.vehicleId == vehicle.id }
        val processed = processEntriesForVehicle(vehicle, vehicleEntries)

        if (processed.isEmpty()) {
            return VehicleStats(
                vehicle = vehicle,
                latestOdometer = vehicle.initialOdometer
            )
        }

        val cal = Calendar.getInstance()

        // All-time aggregates
        val totalExpense = processed.sumOf { it.entry.amountPaid }
        val totalFuel = processed.sumOf { it.entry.fuelQuantity }
        val totalDistance = processed.sumOf { it.distanceTravelled }
        val avgMileage = if (totalFuel > 0 && totalDistance > 0) totalDistance / totalFuel else 0.0
        val avgCostKm = if (totalDistance > 0) totalExpense / totalDistance else 0.0
        val latestOdo = processed.maxOfOrNull { it.entry.odometerReading } ?: vehicle.initialOdometer
        val latestDate = processed.maxOfOrNull { it.entry.date }

        // Current Month aggregates
        val currentMonthProcessed = processed.filter {
            cal.timeInMillis = it.entry.date
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }

        val monthExpense = currentMonthProcessed.sumOf { it.entry.amountPaid }
        val monthFuel = currentMonthProcessed.sumOf { it.entry.fuelQuantity }
        val monthDistance = currentMonthProcessed.sumOf { it.distanceTravelled }
        val monthMileage = if (monthFuel > 0 && monthDistance > 0) monthDistance / monthFuel else 0.0
        val monthCostKm = if (monthDistance > 0) monthExpense / monthDistance else 0.0

        return VehicleStats(
            vehicle = vehicle,
            currentMonthExpense = monthExpense,
            currentMonthFuel = monthFuel,
            currentMonthDistance = monthDistance,
            currentMonthMileage = monthMileage,
            currentMonthCostPerKm = monthCostKm,
            currentMonthEntriesCount = currentMonthProcessed.size,
            totalExpense = totalExpense,
            totalFuel = totalFuel,
            totalDistance = totalDistance,
            averageMileage = avgMileage,
            averageCostPerKm = avgCostKm,
            totalEntriesCount = processed.size,
            latestOdometer = latestOdo,
            lastEntryDate = latestDate
        )
    }

    fun generateMonthlyReport(
        vehicles: List<Vehicle>,
        allEntries: List<FuelEntry>,
        year: Int,
        month: Int
    ): HouseholdMonthlyReport {
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        cal.set(year, month, 1)
        val monthLabel = monthFormat.format(cal.time)

        val vehicleReports = vehicles.map { vehicle ->
            val vEntries = allEntries.filter { it.vehicleId == vehicle.id }
            val processed = processEntriesForVehicle(vehicle, vEntries)
            val monthEntries = processed.filter {
                cal.timeInMillis = it.entry.date
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
            }

            val exp = monthEntries.sumOf { it.entry.amountPaid }
            val fuel = monthEntries.sumOf { it.entry.fuelQuantity }
            val dist = monthEntries.sumOf { it.distanceTravelled }
            val mil = if (fuel > 0 && dist > 0) dist / fuel else 0.0
            val costKm = if (dist > 0) exp / dist else 0.0

            MonthlyVehicleReport(
                vehicleId = vehicle.id,
                vehicleName = vehicle.name,
                vehicleType = vehicle.type,
                colorHex = vehicle.colorHex,
                year = year,
                month = month,
                monthLabel = monthLabel,
                totalExpense = exp,
                totalFuel = fuel,
                totalDistance = dist,
                averageMileage = mil,
                costPerKm = costKm,
                totalEntriesCount = monthEntries.size,
                entries = monthEntries
            )
        }

        val totalExp = vehicleReports.sumOf { it.totalExpense }
        val totalFuel = vehicleReports.sumOf { it.totalFuel }
        val totalDist = vehicleReports.sumOf { it.totalDistance }
        val overallMil = if (totalFuel > 0 && totalDist > 0) totalDist / totalFuel else 0.0
        val overallCostKm = if (totalDist > 0) totalExp / totalDist else 0.0
        val totalCount = vehicleReports.sumOf { it.totalEntriesCount }

        return HouseholdMonthlyReport(
            year = year,
            month = month,
            monthLabel = monthLabel,
            totalExpense = totalExp,
            totalFuel = totalFuel,
            totalDistance = totalDist,
            overallAverageMileage = overallMil,
            overallCostPerKm = overallCostKm,
            totalEntriesCount = totalCount,
            vehicleReports = vehicleReports
        )
    }
}
