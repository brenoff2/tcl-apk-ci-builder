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
    private var lgListenerDevice: ConnectableDevice? = null
    private var lgListener: ConnectableDeviceListener? = null

    private fun clearLgListener() {
        val previousDevice = lgListenerDevice
        val previousListener = lgListener
        if (previousDevice != null && previousListener != null)
            previousDevice.removeListener(previousListener)
        if (previousListener != null && currentConnectableListener === previousListener)
            currentConnectableListener = null
        lgListenerDevice = null
        lgListener = null
    }

    private fun armLgConnectWatchdog(
        device: ConnectableDevice, token: Int, timeoutMs: Long, message: String
    ) {
        connectionWatchdogJob?.cancel()
        connectionWatchdogJob = viewModelScope.launch(Dispatchers.Main) {
            delay(timeoutMs)
            if (isActiveConnection(token) && pendingDeviceId == device.id &&
                _uiState.value.isConnecting) {
                connectionGeneration += 1
                pendingDeviceId = null
                clearLgListener()
                device.disconnect()
                _uiState.update { it.copy(isConnecting = false, pairingRequired = false,
                    statusMessage = message, isError = true) }
                publishDeviceSnapshot()
                if (lgReconnectEnabled && lgReconnectTarget == device.id)
                    scheduleLgReconnect(device.id)
            }
        }
    }

    private fun cancelLgReconnect() {
        lgReconnectEnabled = false
        lgReconnectTarget = null
        lgReconnectJob?.cancel()
        lgReconnectJob = null
    }

    private fun scheduleLgReconnect(targetId: String) {
        if (!lgReconnectEnabled || lgReconnectTarget != targetId ||
            activeDeviceId != null || lgReconnectJob?.isActive == true) return
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
        clearLgListener()
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
            // Involuntary network/discovery loss must NEVER store PREF_MANUAL_DISCONNECT.
            if (device.isLgWebOsDevice()) handleLgWebOsDisconnected()
            else disconnectCurrentDeviceInternal(manual = false)
        } else {
            publishDeviceSnapshot()
        }
    }

    override fun onDiscoveryFailed''',
    '''        if (activeDeviceId == device.id && device.isLgWebOsDevice()) {
            // A newly discovered entry may not own the still-live webOS socket.
            val activeLg = Constant.connectableDevice?.takeIf { it.id == activeDeviceId }
            val activeService = activeLg?.getServiceByName(WebOSTVService.ID) as? WebOSTVService
            if (activeService?.isConnected == true) publishDeviceSnapshot()
            else handleLgWebOsDisconnected()
        } else if (activeDeviceId == device.id) {
            // SSDP loss is involuntary: preserve TCL pairing and auto-reconnect.
            disconnectCurrentDeviceInternal(manual = false)
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
        clearLgListener()
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

# Connected means the selected, actively connected TV, not a stored TCL host.
# A stale Android pairing must never give a powered-off TCL a Disconnect button.
replace_one(
    '''                    val isConnected = isDeviceConnected && (
                        connectedId == device.id ||
                        (!pairedHost.isNullOrBlank() && device.ipAddress == pairedHost)
                    )''',
    '''                    val isConnected = isDeviceConnected && connectedId == device.id'''
)

# ConnectSDK can report an unrelated service/discovery event after SSDP refresh.
# Only a lost active webOS socket may tear down the LG session.
replace_one(
    '''            override fun onDeviceDisconnected(connectableDevice: ConnectableDevice) {
                if (isActiveConnection(token)) handleLgWebOsDisconnected()
            }''',
    '''            override fun onDeviceDisconnected(connectableDevice: ConnectableDevice) {
                if (!isActiveConnection(token)) return
                val active = Constant.connectableDevice?.takeIf { it.id == activeDeviceId }
                val socket = active?.getServiceByName(WebOSTVService.ID) as? WebOSTVService
                if (socket?.isConnected != true) handleLgWebOsDisconnected()
            }'''
)

# Retry loop cannot progress if ConnectSDK never invokes ready/failure.
# Bound each webOS handshake independently of Android/TCL TLS watchdog semantics.
replace_one(
    '''        currentConnectableListener = listener
        device.addListener(listener)
        device.connect()
    }

    private fun handleLgWebOsDisconnected()''',
    '''        clearLgListener()
        currentConnectableListener = listener
        lgListenerDevice = device
        lgListener = listener
        device.addListener(listener)
        armLgConnectWatchdog(device, token, 12_000L, "LG não respondeu. Tentando reconectar...")
        device.connect()
    }

    private fun handleLgWebOsDisconnected()'''
)
replace_one(
    '''                pendingDeviceId = null
                _uiState.update {
                    it.copy(
                        isConnecting = false, pairingRequired = false,
                        statusMessage = "LG webOS: " + (error.message ?: "Conexão recusada"),''',
    '''                pendingDeviceId = null
                connectionWatchdogJob?.cancel()
                clearLgListener()
                connectableDevice.disconnect()
                _uiState.update {
                    it.copy(
                        isConnecting = false, pairingRequired = false,
                        statusMessage = "LG webOS: " + (error.message ?: "Conexão recusada"),'''
)

# A FIRST_SCREEN prompt is not a network handshake failure. Keep time bounded,
# but grant the user an authorization window before timing out and disconnecting.
replace_one(
    '''                if (!isActiveConnection(token)) return
                _uiState.update {
                    it.copy(
                        pairingRequired = false,
                        isConnecting = true,
                        statusMessage = "Autorize a conexão do AnyTV na tela da LG.",''',
    '''                if (!isActiveConnection(token) || pendingDeviceId != device.id) return
                // Human confirmation may take longer than the 12-second socket watchdog.
                armLgConnectWatchdog(device, token, 120_000L,
                    "LG: autorização não confirmada em 2 minutos. Tente reconectar.")
                _uiState.update {
                    it.copy(
                        pairingRequired = false,
                        isConnecting = true,
                        statusMessage = "Autorize a conexão do AnyTV na tela da LG.",'''
)

# Remove the LG listener on involuntary socket loss; keep TCL session untouched.
replace_one(
    '''        val lostId = activeDeviceId ?: pendingDeviceId ?: lgReconnectTarget
        connectionGeneration += 1
        pendingDeviceId = null''',
    '''        val lostId = activeDeviceId ?: pendingDeviceId ?: lgReconnectTarget
        connectionWatchdogJob?.cancel()
        clearLgListener()
        connectionGeneration += 1
        pendingDeviceId = null'''
)

# Keep one listener per LG attempt: a new connection must release the old session.
# Clear the listener on manual disconnect, failures, and ViewModel lifecycle too.
# The pinned ConnectSDK destroys a connected webOS socket when an SSDP entry
# times out: onServiceRemoved -> removeServiceWithId -> disconnect. Keep the
# active webOS service until its socket actually closes; all non-LG services
# retain the upstream removal behavior. On rediscovery, the SDK updates the
# existing service description for the same webOS UUID.
sdk_discovery = Path("source/ConnectSDK/src/main/java/com/connectsdk/discovery/DiscoveryManager.java")
sdk_source = sdk_discovery.read_text(encoding="utf-8")
sdk_old = '''        if (device != null) { 
            device.removeServiceWithId(serviceDescription.getServiceID());

            if (device.getServices().isEmpty()) {'''
sdk_new = '''        if (device != null) {
            if (com.connectsdk.service.WebOSTVService.ID.equals(serviceDescription.getServiceID())) {
                DeviceService webOsService =
                    device.getServiceByName(com.connectsdk.service.WebOSTVService.ID);
                if (webOsService != null && webOsService.isConnected()) {
                    Log.d(Util.T, "Keeping active webOS socket despite expired SSDP announcement");
                    return;
                }
            }
            device.removeServiceWithId(serviceDescription.getServiceID());

            if (device.getServices().isEmpty()) {'''
assert sdk_source.count(sdk_old) == 1, "LG_SSDP_SDK_ANCHOR_DRIFT"
sdk_discovery.write_text(sdk_source.replace(sdk_old, sdk_new, 1), encoding="utf-8")

print("LG_RECONNECT_PAIRING_PATCH_APPLIED_TCL_PATH_PRESERVED")
