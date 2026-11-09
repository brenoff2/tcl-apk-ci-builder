#!/usr/bin/env python3
"""Opt-in LG webOS control adapter for the AnyTV unsigned LG candidate only.

Patches the pinned, already-prepared Android TV upstream. Existing Android/TCL
transport, pairing secrets, OTA and signing files are not changed.
Fail closed if upstream patch anchors drift.
"""
from pathlib import Path

ROOT = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app")
VM = ROOT / "TvRemoteViewModel.kt"
src = VM.read_text(encoding="utf-8")

def change(old: str, new: str) -> None:
    global src
    count = src.count(old)
    assert count == 1, f"LG_WEBOS_ANCHOR_DRIFT {count}: {old[:110]!r}"
    src = src.replace(old, new, 1)

change(
    "import com.connectsdk.service.CastService as ConnectSdkCastService",
    "import com.connectsdk.service.CastService as ConnectSdkCastService\n"
    "import com.connectsdk.service.WebOSTVService\n"
    "import com.connectsdk.service.capability.KeyControl\n"
    "import com.connectsdk.service.capability.TVControl"
)

# The upstream discovery filter deliberately excludes LG, even though its
# bundled ConnectSDK already ships WebOSTVService and SSDP discovery.
change(
    "            discoveryManager?.allDevices?.values?.forEach { device ->\n                if (device.isChromecastServiceDevice()) {",
    "            discoveryManager?.allDevices?.values?.forEach { device ->\n                if (device.isChromecastServiceDevice() || device.isLgWebOsDevice()) {"
)
change(
    "if (!device.isChromecastServiceDevice() && deviceId !in manualDeviceIds) {",
    "if (!device.isChromecastServiceDevice() && !device.isLgWebOsDevice() && deviceId !in manualDeviceIds) {"
)
change(
    "            if (device.isChromecastServiceDevice()) {",
    "            if (device.isChromecastServiceDevice() || device.isLgWebOsDevice()) {"
)
change(
    "        if (!device.isChromecastServiceDevice()) {\n",
    "        if (!device.isChromecastServiceDevice() && !device.isLgWebOsDevice()) {\n"
)
change(
    "            disconnectCurrentDeviceInternal(manual = false)\n        } else {",
    "            if (device.isLgWebOsDevice()) handleLgWebOsDisconnected()\n"
    "            else disconnectCurrentDeviceInternal(manual = false)\n        } else {"
)
change(
    ".filter { it.isChromecastServiceDevice() || it.id in manualDeviceIds }",
    ".filter { it.isChromecastServiceDevice() || it.isLgWebOsDevice() || it.id in manualDeviceIds }"
)
change(
    ".filter { device -> device.id in manualDeviceIds || activeDeviceId == device.id || isDeviceReachable(device) }",
    ".filter { device -> device.isLgWebOsDevice() || device.id in manualDeviceIds || activeDeviceId == device.id || isDeviceReachable(device) }"
)

change(
    "        val isSavedPairing = prefs.getBoolean(Constant.PIN, false)",
    "        // LG uses webOS FIRST_SCREEN pairing, never Android TV TLS secrets.\n"
    "        if (device.isLgWebOsDevice()) {\n"
    "            connectLgWebOs(device)\n"
    "            return\n"
    "        }\n"
    "        val isSavedPairing = prefs.getBoolean(Constant.PIN, false)"
)
change(
    "    fun sendKey(keyCode: Remotemessage.RemoteKeyCode) {\n"
    "        if (!checkConnectionGuard()) return",
    "    fun sendKey(keyCode: Remotemessage.RemoteKeyCode) {\n"
    "        if (!checkConnectionGuard()) return\n"
    "        if (currentConnectableDevice()?.isLgWebOsDevice() == true) {\n"
    "            sendLgWebOsKey(keyCode)\n"
    "            return\n"
    "        }"
)
change(
    "    fun sendHeldKey(\n        keyCode: Remotemessage.RemoteKeyCode,\n"
    "        direction: Remotemessage.RemoteDirection\n    ) {\n"
    "        if (!checkConnectionGuard()) return",
    "    fun sendHeldKey(\n        keyCode: Remotemessage.RemoteKeyCode,\n"
    "        direction: Remotemessage.RemoteDirection\n    ) {\n"
    "        if (!checkConnectionGuard()) return\n"
    "        if (currentConnectableDevice()?.isLgWebOsDevice() == true) {\n"
    "            if (direction == Remotemessage.RemoteDirection.START_LONG) sendLgWebOsKey(keyCode)\n"
    "            return\n"
    "        }"
)
change(
    "    fun sendKeyboardText(text: String) {\n"
    "        if (!checkConnectionGuard()) return",
    "    fun sendKeyboardText(text: String) {\n"
    "        if (!checkConnectionGuard()) return\n"
    "        if (currentConnectableDevice()?.isLgWebOsDevice() == true) {\n"
    "            showStatus(\"Teclado LG requer sessão de entrada webOS; use o teclado da TV por enquanto.\", isError = false)\n"
    "            return\n"
    "        }"
)
change(
    "    fun sendKeyboardEnter() {\n"
    "        if (!checkConnectionGuard()) return",
    "    fun sendKeyboardEnter() {\n"
    "        if (!checkConnectionGuard()) return\n"
    "        if (currentConnectableDevice()?.isLgWebOsDevice() == true) {\n"
    "            sendLgWebOsKey(Remotemessage.RemoteKeyCode.KEYCODE_DPAD_CENTER)\n"
    "            return\n"
    "        }"
)
change(
    "    fun launchQuickApp(appName: String) {\n"
    "        if (!checkConnectionGuard()) return",
    "    fun launchQuickApp(appName: String) {\n"
    "        if (!checkConnectionGuard()) return\n"
    "        if (currentConnectableDevice()?.isLgWebOsDevice() == true) {\n"
    "            showStatus(\"Atalhos de apps LG ainda não disponíveis; utilize Home.\", isError = false)\n"
    "            return\n"
    "        }"
)

change(
    "        val typeName = when {\n",
    "        val typeName = when {\n"
    "            isLgWebOsDevice() -> \"LG webOS\"\n"
)
change(
    "            supportsCast = id !in manualDeviceIds,",
    "            supportsCast = id !in manualDeviceIds && !isLgWebOsDevice(),"
)
change(
    "    private fun ConnectableDevice.isChromecastServiceDevice(): Boolean {",
    """    private fun ConnectableDevice.isLgWebOsDevice(): Boolean =
        getServiceByName(WebOSTVService.ID) != null

    private fun connectLgWebOs(device: ConnectableDevice) {
        val token = ++connectionGeneration
        app.androidRemoteTv?.abort()
        app.androidRemoteTv = null
        val service = device.getServiceByName(WebOSTVService.ID) as? WebOSTVService
        if (service == null) {
            pendingDeviceId = null
            showStatus("A LG não anunciou o serviço webOS.", isError = true)
            publishDeviceSnapshot()
            return
        }
        // Pairing confirmation is shown by the LG itself, not the Android PIN modal.
        DiscoveryManager.getInstance().pairingLevel = DiscoveryManager.PairingLevel.PROTECTED
        service.setPairingType(DeviceService.PairingType.FIRST_SCREEN)
        val listener = object : ConnectableDeviceListener {
            override fun onDeviceReady(connectableDevice: ConnectableDevice) {
                if (!isActiveConnection(token) || !service.isConnected) return
                Constant.connectableDevice = connectableDevice
                Constant.isConnected.value = true
                activeDeviceId = connectableDevice.id
                pendingDeviceId = null
                connectionWatchdogJob?.cancel()
                autoReconnectScheduleJob?.cancel()
                // Pause TCL auto-reconnect while the explicitly selected LG is active.
                // Selecting Android TV later clears this flag in its existing path.
                prefs.edit { putBoolean(Constant.PREF_MANUAL_DISCONNECT, true) }
                _uiState.update {
                    it.copy(
                        connectedDevice = connectableDevice.toUiModel(
                            isPaired = true, isConnected = true, isConnecting = false
                        ),
                        isConnecting = false, pairingRequired = false,
                        pairingError = null,
                        statusMessage = "LG conectada. Controle disponível.",
                        isError = false
                    )
                }
                publishDeviceSnapshot()
            }

            override fun onPairingRequired(
                connectableDevice: ConnectableDevice,
                service: DeviceService,
                pairingType: DeviceService.PairingType
            ) {
                if (!isActiveConnection(token)) return
                _uiState.update {
                    it.copy(
                        pairingRequired = false,
                        isConnecting = true,
                        statusMessage = "Autorize a conexão do AnyTV na tela da LG.",
                        isError = false
                    )
                }
            }

            override fun onDeviceDisconnected(connectableDevice: ConnectableDevice) {
                if (isActiveConnection(token)) handleLgWebOsDisconnected()
            }

            override fun onConnectionFailed(
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
            }

            override fun onCapabilityUpdated(
                connectableDevice: ConnectableDevice,
                added: List<String>,
                removed: List<String>
            ) = Unit
        }
        currentConnectableListener = listener
        device.addListener(listener)
        device.connect()
    }

    private fun handleLgWebOsDisconnected() {
        // A stored Android TV host is never an LG reconnect target.
        connectionGeneration += 1
        pendingDeviceId = null
        activeDeviceId = null
        Constant.connectableDevice = null
        Constant.isConnected.value = false
        _uiState.update {
            it.copy(
                connectedDevice = null,
                isConnecting = false,
                pairingRequired = false,
                statusMessage = "LG desconectada. Escolha a televisão novamente.",
                isError = true
            )
        }
        publishDeviceSnapshot()
    }

    private fun sendLgWebOsKey(key: Remotemessage.RemoteKeyCode) {
        val device = currentConnectableDevice()?.takeIf {
            it.id == activeDeviceId && it.isLgWebOsDevice() && it.isConnected
        } ?: return
        val listener = emptyResponseListener()
        val keys = device.getCapability(KeyControl::class.java)
        val volume = device.getCapability(VolumeControl::class.java)
        val channels = device.getCapability(TVControl::class.java)
        val media = device.getCapability(MediaControl::class.java)
        when (key) {
            Remotemessage.RemoteKeyCode.KEYCODE_DPAD_UP -> keys?.up(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_DPAD_DOWN -> keys?.down(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_DPAD_LEFT -> keys?.left(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_DPAD_RIGHT -> keys?.right(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_DPAD_CENTER -> keys?.ok(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_BACK -> keys?.back(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_HOME -> keys?.home(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_VOLUME_UP -> volume?.volumeUp(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_VOLUME_DOWN -> volume?.volumeDown(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_VOLUME_MUTE -> {
                val mute = !_uiState.value.isMuted
                volume?.setMute(mute, listener)
                if (volume != null) _uiState.update { it.copy(isMuted = mute) }
            }
            Remotemessage.RemoteKeyCode.KEYCODE_CHANNEL_UP -> channels?.channelUp(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_CHANNEL_DOWN -> channels?.channelDown(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_PLAY -> media?.play(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_PAUSE -> media?.pause(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_STOP -> media?.stop(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_REWIND -> media?.rewind(listener)
            Remotemessage.RemoteKeyCode.KEYCODE_MEDIA_FAST_FORWARD -> media?.fastForward(listener)
            else -> showStatus("Comando não disponível na LG webOS.", isError = false)
        }
    }

    private fun ConnectableDevice.isChromecastServiceDevice(): Boolean {"""
)
# A stale saved TCL Android IP must not falsely label the LG paired.
change(
    "isPaired = device.ipAddress == pairedHost && prefs.getBoolean(Constant.PIN, false),",
    "isPaired = (device.id == activeDeviceId && device.isLgWebOsDevice()) ||\n"
    "                    (device.ipAddress == pairedHost && prefs.getBoolean(Constant.PIN, false)),"
)

# Upstream App explicitly registers Cast only; without SSDP webOS is invisible.
APP = Path("source/app/src/main/java/com/hari/androidtvremote/App.kt")
app_src = APP.read_text(encoding="utf-8")
assert app_src.count("import com.connectsdk.discovery.provider.CastDiscoveryProvider") == 1
app_src = app_src.replace(
    "import com.connectsdk.discovery.provider.CastDiscoveryProvider",
    "import com.connectsdk.discovery.provider.CastDiscoveryProvider\n"
    "import com.connectsdk.discovery.provider.SSDPDiscoveryProvider\n"
    "import com.connectsdk.service.WebOSTVService", 1
)
registration = """        discoveryManager?.registerDeviceService(
            CastService::class.java,
            CastDiscoveryProvider::class.java
        )"""
assert app_src.count(registration) == 1, "LG_WEBOS_DISCOVERY_REGISTRATION_DRIFT"
app_src = app_src.replace(registration,
    registration + """
        discoveryManager?.registerDeviceService(
            WebOSTVService::class.java,
            SSDPDiscoveryProvider::class.java
        )""", 1)
APP.write_text(app_src, encoding="utf-8")

VM.write_text(src, encoding="utf-8")
assert "connectLgWebOs(device)" in VM.read_text(encoding="utf-8")

# The TCL companion is Android-only. LG keeps the same remote/streaming UI,
# but must not attempt the TCL private pairing port (18765).
HOME = ROOT / "TclApprovedHomeScreen.kt"
home = HOME.read_text(encoding="utf-8")
def home_change(old: str, new: str) -> None:
    global home
    assert home.count(old) == 1, f"LG_WEBOS_HOME_ANCHOR_DRIFT: {old[:80]!r}"
    home = home.replace(old, new, 1)

home_change(
    "import androidx.compose.material3.TextButton",
    "import androidx.compose.material3.TextButton\nimport androidx.compose.material3.Button"
)
home_change(
    "    var showKeyboard by rememberSaveable { mutableStateOf(false) }",
    "    var showKeyboard by rememberSaveable { mutableStateOf(false) }\n"
    "    var lgStreaming by rememberSaveable { mutableStateOf(false) }"
)
home_change(
    "                    TclInstalledAppsBar(\n",
    """                    if (sessionState.connectedDevice?.type == "LG webOS") {
                        Button(onClick = { lgStreaming = true }) {
                            Text("Streaming")
                        }
                    } else TclInstalledAppsBar(
"""
)
home_change(
    "                    Column(\n                        modifier = Modifier.fillMaxWidth(),\n"
    "                        verticalArrangement = Arrangement.spacedBy(6.dp)\n",
    """                    if (lgStreaming && sessionState.connectedDevice?.type == "LG webOS") {
                        TclStreamingScreens(
                            apps = emptyList(), host = "", pairing = "",
                            tvName = sessionState.connectedDevice?.name.orEmpty(),
                            onDismiss = { lgStreaming = false },
                            onRemoteKey = { remoteKey(it) },
                            onRemoteHoldStart = { key ->
                                if (isConnected) remoteKey(key)
                            },
                            onRemoteHoldEnd = { _ -> Unit },
                            onKeyboardText = { if (isConnected) onKeyboardText(it) },
                            onKeyboardEnter = { if (isConnected) onKeyboardEnter() }
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
"""
)
# webOS exposes discrete keys, not Android TV START_LONG/END_LONG frames.
# A held LG D-pad press sends one supported key on start, never a false END_LONG.
home_change(
    """                                    onHoldStart = {
                                        onKeyDirection(it, Remotemessage.RemoteDirection.START_LONG)
                                    },
                                    onHoldEnd = {
                                        onKeyDirection(it, Remotemessage.RemoteDirection.END_LONG)
                                    }""",
    """                                    onHoldStart = {
                                        if (sessionState.connectedDevice?.type == "LG webOS") remoteKey(it)
                                        else onKeyDirection(it, Remotemessage.RemoteDirection.START_LONG)
                                    },
                                    onHoldEnd = {
                                        if (sessionState.connectedDevice?.type != "LG webOS")
                                            onKeyDirection(it, Remotemessage.RemoteDirection.END_LONG)
                                    }"""
)

# The headset shortcut must not probe TCL port 18765 using an LG IP.
home_change(
    "                                val tv = sessionState.connectedDevice\n"
    "                                val host = tv?.ipAddress\n"
    "                                if (host.isNullOrBlank()) {\n"
    "                                    onOpenDiscovery()",
    "                                val isLg = sessionState.connectedDevice?.type == \"LG webOS\"\n"
    "                                val tv = sessionState.connectedDevice\n"
    "                                val host = if (isLg) null else tv?.ipAddress\n"
    "                                if (host.isNullOrBlank()) {\n"
    "                                    if (isLg) headsetStatus = \"Fones via companion disponíveis somente na TCL.\"\n"
    "                                    else onOpenDiscovery()"
)

HOME.write_text(home, encoding="utf-8")
assert "LG webOS" in HOME.read_text(encoding="utf-8")

print("ANYTV_LG_WEBOS_ADAPTER_PATCHED_NO_SIGNING_NO_OTA")
