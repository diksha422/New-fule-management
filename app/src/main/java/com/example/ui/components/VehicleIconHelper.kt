package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.VehicleType
import com.example.ui.theme.VehicleBikeBg
import com.example.ui.theme.VehicleBikeFg
import com.example.ui.theme.VehicleCarBg
import com.example.ui.theme.VehicleCarFg
import com.example.ui.theme.VehicleOtherBg
import com.example.ui.theme.VehicleOtherFg
import com.example.ui.theme.VehicleScooterBg
import com.example.ui.theme.VehicleScooterFg

object VehicleIconHelper {
    fun getIconForType(type: VehicleType): ImageVector {
        return when (type) {
            VehicleType.SCOOTER -> Icons.Default.ElectricScooter
            VehicleType.MOTORCYCLE -> Icons.Default.TwoWheeler
            VehicleType.CAR -> Icons.Default.DirectionsCar
            VehicleType.OTHER -> Icons.Default.DirectionsBike
        }
    }

    fun getNaturalBgColor(type: VehicleType): Color {
        return when (type) {
            VehicleType.SCOOTER -> VehicleScooterBg
            VehicleType.MOTORCYCLE -> VehicleBikeBg
            VehicleType.CAR -> VehicleCarBg
            VehicleType.OTHER -> VehicleOtherBg
        }
    }

    fun getNaturalFgColor(type: VehicleType): Color {
        return when (type) {
            VehicleType.SCOOTER -> VehicleScooterFg
            VehicleType.MOTORCYCLE -> VehicleBikeFg
            VehicleType.CAR -> VehicleCarFg
            VehicleType.OTHER -> VehicleOtherFg
        }
    }
}

