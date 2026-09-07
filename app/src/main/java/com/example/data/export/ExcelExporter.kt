package com.example.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.FuelCalculations
import com.example.data.model.FuelEntry
import com.example.data.model.Household
import com.example.data.model.Vehicle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ExcelExporter {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun exportToExcelAndShare(
        context: Context,
        household: Household?,
        vehicles: List<Vehicle>,
        allEntries: List<FuelEntry>
    ): Intent {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val fileName = "FuelReport_${household?.accessCode ?: "Household"}_${fileDateFormat.format(Date())}.xlsx"
        val exportFile = File(exportDir, fileName)

        // Generate clean XML Spreadsheet 2003 (.xlsx compatible format recognized by MS Excel, Google Sheets, LibreOffice)
        val xmlContent = generateExcelXml(household, vehicles, allEntries)
        FileOutputStream(exportFile).use { it.write(xmlContent.toByteArray(Charsets.UTF_8)) }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.ms-excel"
            putExtra(Intent.EXTRA_SUBJECT, "Fuel Management Report - ${household?.name ?: "Household"}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Detailed vehicle fuel records, mileage calculations, and monthly summaries from FuelTrack."
            )
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun exportToCsvAndShare(
        context: Context,
        household: Household?,
        vehicles: List<Vehicle>,
        allEntries: List<FuelEntry>
    ): Intent {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val fileName = "FuelData_${fileDateFormat.format(Date())}.csv"
        val exportFile = File(exportDir, fileName)

        val csvBuilder = StringBuilder()
        csvBuilder.append("HOUSEHOLD FUEL MANAGEMENT REPORT\n")
        csvBuilder.append("Household: ${household?.name ?: "Default"} (${household?.accessCode ?: ""})\n")
        csvBuilder.append("Export Date: ${dateFormat.format(Date())}\n\n")

        for (vehicle in vehicles) {
            csvBuilder.append("========================================\n")
            csvBuilder.append("VEHICLE: ${vehicle.name} (${vehicle.type.displayName} - ${vehicle.fuelType})\n")
            csvBuilder.append("Registration: ${vehicle.registrationNumber} | Starting Odometer: ${vehicle.initialOdometer} km\n")
            csvBuilder.append("========================================\n")
            csvBuilder.append("Date,Amount Paid (Rs),Fuel Quantity (L),Odometer (km),Distance Travelled (km),Mileage (km/L),Cost Per KM (Rs/km),Petrol Pump,Added By,Notes\n")

            val vEntries = allEntries.filter { it.vehicleId == vehicle.id }
            val processed = FuelCalculations.processEntriesForVehicle(vehicle, vEntries)

            for (item in processed.reversed()) { // Chronological
                val e = item.entry
                val dateStr = dateFormat.format(Date(e.date))
                val notesSanitized = e.notes.replace(",", ";").replace("\n", " ")
                val pumpSanitized = e.petrolPumpName.replace(",", ";")

                csvBuilder.append(
                    "\"$dateStr\",${e.amountPaid},${e.fuelQuantity},${e.odometerReading}," +
                    "${String.format(Locale.US, "%.1f", item.distanceTravelled)}," +
                    "${String.format(Locale.US, "%.2f", item.mileage)}," +
                    "${String.format(Locale.US, "%.2f", item.costPerKm)}," +
                    "\"$pumpSanitized\",\"${e.addedByUserName}\",\"$notesSanitized\"\n"
                )
            }
            csvBuilder.append("\n")
        }

        FileOutputStream(exportFile).use { it.write(csvBuilder.toString().toByteArray(Charsets.UTF_8)) }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Fuel Management CSV - ${household?.name ?: "Household"}")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun generateExcelXml(
        household: Household?,
        vehicles: List<Vehicle>,
        allEntries: List<FuelEntry>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Bottom"/>
   <Borders/>
   <Font ss:FontName="Calibri" x:Family="Swiss" ss:Size="11" ss:Color="#000000"/>
   <Interior/>
   <NumberFormat/>
   <Protection/>
  </Style>
  <Style ss:ID="Header">
   <Font ss:FontName="Calibri" ss:Size="12" ss:Color="#FFFFFF" ss:Bold="1"/>
   <Interior ss:Color="#006A60" ss:Pattern="Solid"/>
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
  </Style>
  <Style ss:ID="Title">
   <Font ss:FontName="Calibri" ss:Size="16" ss:Color="#006A60" ss:Bold="1"/>
  </Style>
  <Style ss:ID="SubTitle">
   <Font ss:FontName="Calibri" ss:Size="11" ss:Color="#555555" ss:Italic="1"/>
  </Style>
  <Style ss:ID="SummaryHeader">
   <Font ss:FontName="Calibri" ss:Size="11" ss:Color="#FFFFFF" ss:Bold="1"/>
   <Interior ss:Color="#2E638A" ss:Pattern="Solid"/>
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
  </Style>
  <Style ss:ID="NumberCell">
   <Alignment ss:Horizontal="Right"/>
  </Style>
 </Styles>
""")

        // Create individual sheets for each vehicle
        for (vehicle in vehicles) {
            val sheetName = escapeXml(vehicle.name)
            sb.append(" <Worksheet ss:Name=\"$sheetName\">\n")
            sb.append("  <Table>\n")
            sb.append("   <Column ss:Width=\"120\"/>\n")
            sb.append("   <Column ss:Width=\"90\"/>\n")
            sb.append("   <Column ss:Width=\"90\"/>\n")
            sb.append("   <Column ss:Width=\"110\"/>\n")
            sb.append("   <Column ss:Width=\"120\"/>\n")
            sb.append("   <Column ss:Width=\"100\"/>\n")
            sb.append("   <Column ss:Width=\"100\"/>\n")
            sb.append("   <Column ss:Width=\"130\"/>\n")
            sb.append("   <Column ss:Width=\"100\"/>\n")
            sb.append("   <Column ss:Width=\"160\"/>\n")

            // Title Rows
            sb.append("   <Row>\n")
            sb.append("    <Cell ss:StyleID=\"Title\"><Data ss:Type=\"String\">${escapeXml(vehicle.name)} Fuel History</Data></Cell>\n")
            sb.append("   </Row>\n")
            sb.append("   <Row>\n")
            sb.append("    <Cell ss:StyleID=\"SubTitle\"><Data ss:Type=\"String\">Type: ${vehicle.type.displayName} | Reg: ${vehicle.registrationNumber} | Starting Odo: ${vehicle.initialOdometer} km</Data></Cell>\n")
            sb.append("   </Row>\n")
            sb.append("   <Row/>\n")

            // Headers
            sb.append("   <Row ss:StyleID=\"Header\">\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Date</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Amount (Rs)</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Fuel Quantity (L)</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Odometer (km)</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Distance (km)</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Mileage (km/L)</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Cost Per KM (Rs)</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Petrol Pump</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Added By</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Notes</Data></Cell>\n")
            sb.append("   </Row>\n")

            val vEntries = allEntries.filter { it.vehicleId == vehicle.id }
            val processed = FuelCalculations.processEntriesForVehicle(vehicle, vEntries)

            for (item in processed) {
                val e = item.entry
                val dateStr = dateFormat.format(Date(e.date))

                sb.append("   <Row>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">$dateStr</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">${e.amountPaid}</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">${e.fuelQuantity}</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">${e.odometerReading}</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.1f", item.distanceTravelled)}</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", item.mileage)}</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", item.costPerKm)}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(e.petrolPumpName)}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(e.addedByUserName)}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(e.notes)}</Data></Cell>\n")
                sb.append("   </Row>\n")
            }

            sb.append("  </Table>\n")
            sb.append(" </Worksheet>\n")
        }

        // Monthly Summary Sheet
        sb.append(" <Worksheet ss:Name=\"Monthly Summary\">\n")
        sb.append("  <Table>\n")
        sb.append("   <Column ss:Width=\"120\"/>\n")
        sb.append("   <Column ss:Width=\"120\"/>\n")
        sb.append("   <Column ss:Width=\"100\"/>\n")
        sb.append("   <Column ss:Width=\"100\"/>\n")
        sb.append("   <Column ss:Width=\"110\"/>\n")
        sb.append("   <Column ss:Width=\"110\"/>\n")
        sb.append("   <Column ss:Width=\"110\"/>\n")
        sb.append("   <Column ss:Width=\"80\"/>\n")

        sb.append("   <Row>\n")
        sb.append("    <Cell ss:StyleID=\"Title\"><Data ss:Type=\"String\">Household Fuel Summary</Data></Cell>\n")
        sb.append("   </Row>\n")
        sb.append("   <Row>\n")
        sb.append("    <Cell ss:StyleID=\"SubTitle\"><Data ss:Type=\"String\">Household: ${escapeXml(household?.name ?: "Default")} | Code: ${escapeXml(household?.accessCode ?: "")}</Data></Cell>\n")
        sb.append("   </Row>\n")
        sb.append("   <Row/>\n")

        sb.append("   <Row ss:StyleID=\"SummaryHeader\">\n")
        sb.append("    <Cell><Data ss:Type=\"String\">Month</Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\">Vehicle</Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\">Total Expense (Rs)</Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\">Total Fuel (L)</Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\">Total Distance (km)</Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\">Avg Mileage (km/L)</Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\">Cost Per KM (Rs)</Data></Cell>\n")
        sb.append("    <Cell><Data ss:Type=\"String\">Entries</Data></Cell>\n")
        sb.append("   </Row>\n")

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)

        // Generate for the last 6 months
        for (monthOffset in 0..5) {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -monthOffset)
            }
            val y = targetCal.get(Calendar.YEAR)
            val m = targetCal.get(Calendar.MONTH)
            val report = FuelCalculations.generateMonthlyReport(vehicles, allEntries, y, m)

            for (vReport in report.vehicleReports) {
                sb.append("   <Row>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${vReport.monthLabel}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(vReport.vehicleName)}</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", vReport.totalExpense)}</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", vReport.totalFuel)}</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.1f", vReport.totalDistance)}</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", vReport.averageMileage)}</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", vReport.costPerKm)}</Data></Cell>\n")
                sb.append("    <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">${vReport.totalEntriesCount}</Data></Cell>\n")
                sb.append("   </Row>\n")
            }

            // Month Combined Total Row
            sb.append("   <Row>\n")
            sb.append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">${report.monthLabel} (COMBINED)</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">All Vehicles</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", report.totalExpense)}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", report.totalFuel)}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.1f", report.totalDistance)}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", report.overallAverageMileage)}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"Number\">${String.format(Locale.US, "%.2f", report.overallCostPerKm)}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"Number\">${report.totalEntriesCount}</Data></Cell>\n")
            sb.append("   </Row>\n")
            sb.append("   <Row/>\n")
        }

        sb.append("  </Table>\n")
        sb.append(" </Worksheet>\n")
        sb.append("</Workbook>\n")

        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
