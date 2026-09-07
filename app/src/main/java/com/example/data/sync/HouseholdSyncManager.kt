package com.example.data.sync

import com.example.data.model.FuelEntry
import com.example.data.model.Household
import com.example.data.model.Vehicle
import com.example.data.repository.FuelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SyncState {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE_PERSISTED,
    ERROR
}

data class SyncStatus(
    val state: SyncState = SyncState.SYNCED,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val message: String = "Data saved locally on this device"
)

class HouseholdSyncManager(
    private val repository: FuelRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun triggerCloudSync() {
        scope.launch {
            _syncStatus.value = SyncStatus(
                state = SyncState.SYNCING,
                message = "Saving locally..."
            )
            delay(300)
            _syncStatus.value = SyncStatus(
                state = SyncState.SYNCED,
                lastSyncTime = System.currentTimeMillis(),
                message = "Saved on this device (no cloud sync configured)"
            )
        }
    }

    suspend fun switchActiveUser(userKey: String) {
        val currentHousehold = repository.getHousehold() ?: return
        val currentUserName = if (userKey == "user_1") currentHousehold.currentUserName else currentHousehold.partnerUserName
        repository.updateHousehold(
            currentHousehold.copy(
                activeUserKey = userKey,
                lastSyncedAt = System.currentTimeMillis()
            )
        )
        triggerCloudSync()
    }

    suspend fun updateHouseholdDetails(name: String, user1Name: String, user2Name: String, accessCode: String) {
        val currentHousehold = repository.getHousehold() ?: Household()
        repository.updateHousehold(
            currentHousehold.copy(
                name = name,
                currentUserName = user1Name,
                partnerUserName = user2Name,
                accessCode = accessCode,
                lastSyncedAt = System.currentTimeMillis()
            )
        )
        triggerCloudSync()
    }

    suspend fun joinHousehold(code: String) {
        _syncStatus.value = SyncStatus(
            state = SyncState.SYNCING,
            message = "Saving household code $code..."
        )
        delay(300)
        val currentHousehold = repository.getHousehold() ?: Household()
        repository.updateHousehold(
            currentHousehold.copy(
                accessCode = code.trim().uppercase(),
                lastSyncedAt = System.currentTimeMillis()
            )
        )
        _syncStatus.value = SyncStatus(
            state = SyncState.SYNCED,
            lastSyncTime = System.currentTimeMillis(),
            message = "Household code $code saved locally (single-device only)"
        )
    }
}
