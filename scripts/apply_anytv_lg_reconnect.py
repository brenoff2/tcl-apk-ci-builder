#!/usr/bin/env python3
"""LG-only pairing state and bounded reconnect after the existing LG apps patch."""
from pathlib import Path

vm = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TvRemoteViewModel.kt")

def replace_one(old: str, new: str) -> None:
    content = vm.read_text(encoding="utf-8")
    if content.count(old) != 1:
        i = content.find("override fun onDeviceRemoved")
        excerpt = content[i:i+780] if i >= 0 else "method absent"
        raise AssertionError(f"LG_RECONNECT_ANCHOR_DRIFT {content.count(old)} {old[:100]!r} CONTEXT: {excerpt}")
    vm.write_text(content.replace(old, new, 1), encoding="utf-8")

replace_one(
    "    private fun connectLgWebOs(device: ConnectableDevice) {",
    '''    // Never mix Android TV TLS host/secret reconnect with webOS client-key pairing.
    private var lgReconnectTarget: String? = null
    private var lgReconnectEnabled: Boolean = false
    private var lgReconnectJob: Job? = null

    private fun cancelLgReconnect() {
        lgReconnectEnabled = false
        lgReconnectTarget = null
        lgReconnectJob?.cancel()
        lgReconnectJob = null
    }

    private fun scheduleLgReconnect(targetId: String) {
        if (!lgReconnectEnabled || lgReconnectTarget != targetId ||
            lgReconnectJob?.isActive == true) return
        lgReconnectJob = viewModelScope.launch(Dispatchers.Main) {
            // Allow the TV to sleep/wake and SSDP to rediscover it.
            for (attempt in 1..6) {
                delay((attempt * 2000L).coerceAtMost(8000L))
                if (!lgReconnectEnabled || lgReconnectTarget != targetId ||
                    activeDeviceId != null) break
                if (_uiState.value.isConnecting) continue
                val tv = synchronized(discoveredDevices) { discoveredDevices[targetId] }
                if (tv?.isLgWebOsDevice() == true) {
                    Timber.i("[LG Reconnect] webOS attempt $attempt/6 for $targetId")
                    connectToDeviceInternal(targetId, userInitiated = false)
                }
            }
            lgReconnectJob = null
        }
    }

    private fun connectLgWebOs(device: ConnectableDevice) {'''
)
replace_one(
    '''                activeDeviceId = connectableDevice.id
                pendingDeviceId = null
                connectionWatchdogJob?.cancel()''',
    '''                activeDeviceId = connectableDevice.id
                pendingDeviceId = null
                lgReconnectEnabled = true
                lgReconnectTarget = connectableDevice.id
                // A completed FIRST_SCREEN authorization is durable, independent of TCP state.
                prefs.edit { putBoolean("anytv_lg_paired_" + connectableDevice.id, true) }
                lgReconnectJob?.cancel()
                lgReconnectJob = null
                connectionWatchdogJob?.cancel()'''
)
replace_one(
    '''    private fun handleLgWebOsDisconnected() {
        // A stored Android TV host is never an LG reconnect target.
        connectionGeneration += 1''',
    '''    private fun handleLgWebOsDisconnected() {
        // Distinguish temporary websocket/SSDP loss from a voluntary disconnect.
        val lostId = activeDeviceId ?: pendingDeviceId ?: lgReconnectTarget
        connectionGeneration += 1'''
)
replace_one(
    '''                statusMessage = "LG desconectada. Escolha a televisão novamente.",
                isError = true
            )
        }
        publishDeviceSnapshot()
    }''',
    '''                statusMessage = "LG desconectada. Tentando reconectar...",
                isError = true
            )
        }
        publishDeviceSnapshot()
        if (lostId != null && lgReconnectEnabled) scheduleLgReconnect(lostId)
    }'''
)
replace_one(
    '''            override fun onConnectionFailed(
                connectableDevice: ConnectableDevice,
                error: ServiceCommandError
            ) {
                if (!isActiveConnection(token)) return
                pendingDeviceId = null
                _uiState.update {
                    it.copy(
                        isConnecting = false, pairingRequired = false,
                        statusMessage = "LG webOS: " + (error.message ?: "Conexão recusada"),
                        isError = true
                    )
                }
                publishDeviceSnapshot()
            }''',
    '''            override fun onConnectionFailed(
                connectableDevice: ConnectableDevice,
                error: ServiceCommandError
            ) {
                if (!isActiveConnection(token)) return
                pendingDeviceId = null
                _uiState.update {
                    it.copy(
                        isConnecting = false, pairingRequired = false,
                        statusMessage = "LG webOS: " + (error.message ?: "Conexão recusada"),
                        isError = true
                    )
                }
                publishDeviceSnapshot()
                if (lgReconnectEnabled && lgReconnectTarget == connectableDevice.id)
                    scheduleLgReconnect(connectableDevice.id)
            }'''
)
replace_one(
    '''        if (userInitiated) {
            autoReconnectScheduleJob?.cancel()''',
    '''        if (userInitiated) {
            // A deliberate TV selection supersedes an in-flight LG reconnect.
            cancelLgReconnect()
            autoReconnectScheduleJob?.cancel()'''
)
replace_one(
    '''    private fun disconnectCurrentDeviceInternal(manual: Boolean) {
        connectionGeneration += 1''',
    '''    private fun disconnectCurrentDeviceInternal(manual: Boolean) {
        // Explicit disconnect or switching TVs must not resurrect a previous LG.
        cancelLgReconnect()
        connectionGeneration += 1'''
)
replace_one(
    '''    private fun currentConnectableDevice(): ConnectableDevice? {
        val currentId = activeDeviceId''',
    '''    private fun currentConnectableDevice(): ConnectableDevice? {
        // SSDP may replace a registry entry while the original LG socket stays live.
        // Keep commands bound to the approved, connected service instance.
        val lgActive = Constant.connectableDevice?.takeIf { device ->
            device.id == activeDeviceId &&
                (device.getServiceByName(WebOSTVService.ID) as? WebOSTVService)?.isConnected == true
        }
        if (lgActive != null) return lgActive
        val currentId = activeDeviceId'''
)
replace_one(
    '''        if (activeDeviceId == device.id) {
            disconnectCurrentDevice()
        } else {
            publishDeviceSnapshot()
        }
    }

    override fun onDiscoveryFailed''',
    '''        if (activeDeviceId == device.id && device.isLgWebOsDevice()) {
            // SSDP TTL expiry does NOT mean the connected webOS socket dropped.
            if ((device.getServiceByName(WebOSTVService.ID) as? WebOSTVService)?.isConnected == true)
                publishDeviceSnapshot()
            else handleLgWebOsDisconnected()
        } else if (activeDeviceId == device.id) {
            disconnectCurrentDevice()
        } else {
            publishDeviceSnapshot()
        }
    }

    override fun onDiscoveryFailed'''
)
replace_one(
    '''        super.onCleared()
        autoReconnectScheduleJob?.cancel()''',
    '''        super.onCleared()
        cancelLgReconnect()
        autoReconnectScheduleJob?.cancel()'''
)
replace_one(
    '''isPaired = (device.id == activeDeviceId && device.isLgWebOsDevice()) ||
                    (device.ipAddress == pairedHost && prefs.getBoolean(Constant.PIN, false)),''',
    '''isPaired = (device.isLgWebOsDevice() &&
                        prefs.getBoolean("anytv_lg_paired_" + device.id, false)) ||
                    (device.ipAddress == pairedHost && prefs.getBoolean(Constant.PIN, false)),'''
)
# Trigger LG restoration when the SSDP discovery registry recovers after a disconnect.
replace_one(
    '''        publishDeviceSnapshot()
        scheduleAutoReconnect(delayMs = 500L)
    }

    override fun onDeviceUpdated''',
    '''        publishDeviceSnapshot()
        if (device.isLgWebOsDevice() && lgReconnectTarget == device.id)
            scheduleLgReconnect(device.id)
        else scheduleAutoReconnect(delayMs = 500L)
    }

    override fun onDeviceUpdated'''
)
replace_one(
    '''        publishDeviceSnapshot()
        scheduleAutoReconnect(delayMs = 500L)
    }

    override fun onDeviceRemoved''',
    '''        publishDeviceSnapshot()
        if (device.isLgWebOsDevice() && lgReconnectTarget == device.id)
            scheduleLgReconnect(device.id)
        else scheduleAutoReconnect(delayMs = 500L)
    }

    override fun onDeviceRemoved'''
)
print("LG_RECONNECT_PAIRING_PATCH_APPLIED_TCL_PATH_PRESERVED")
