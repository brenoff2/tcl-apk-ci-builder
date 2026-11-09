#!/usr/bin/env python3
"""LG-only app list and launcher route from ConnectSDK, after existing LG pointer patch."""
from pathlib import Path

root = Path("source/app/src/main/java/com/hari/androidtvremote")
vm = root / "ui/app/TvRemoteViewModel.kt"
nav = root / "navigation/AppNavGraph.kt"
home = root / "ui/app/TclApprovedHomeScreen.kt"

def patch(path: Path, old: str, new: str) -> None:
    s = path.read_text(encoding="utf-8")
    assert s.count(old) == 1, f"LG_APP_ANCHOR_DRIFT {path} count={s.count(old)}: {old[:110]!r}"
    path.write_text(s.replace(old, new, 1), encoding="utf-8")

patch(vm,
    "import com.connectsdk.service.WebOSTVService",
    "import com.connectsdk.service.WebOSTVService\n"
    "import com.connectsdk.core.AppInfo\n"
    "import com.connectsdk.service.capability.Launcher")
patch(vm,
    "    val cast: CastPlaybackUiState = CastPlaybackUiState(),",
    "    val lgWebOsApps: List<LgWebOsApp> = emptyList(),\n"
    "    val lgWebOsAppsBusy: Boolean = false,\n"
    "    val lgWebOsAppsError: String? = null,\n"
    "    val cast: CastPlaybackUiState = CastPlaybackUiState(),")

# Use only actual launcher IDs from the most recent device query.
# Ignore asynchronous responses from a TV that is no longer the active TV.
methods = '''    fun refreshLgWebOsApps() {
        val device = currentConnectableDevice()?.takeIf { it.id == activeDeviceId } ?: return
        val service = device.getServiceByName(WebOSTVService.ID) as? WebOSTVService ?: return
        if (!service.isConnected || _uiState.value.lgWebOsAppsBusy) return
        val deviceId = device.id
        _uiState.update { it.copy(lgWebOsAppsBusy = true,
            lgWebOsAppsError = null, lgWebOsApps = emptyList()) }
        service.getAppList(object : Launcher.AppListListener {
            override fun onSuccess(result: MutableList<AppInfo>?) {
                if (activeDeviceId != deviceId) return
                val entries = result.orEmpty().mapNotNull { item ->
                    val appId = item.id?.trim().orEmpty()
                    if (appId.isBlank()) null else LgWebOsApp(
                        appId, item.name?.trim()?.takeIf { it.isNotBlank() } ?: appId,
                        item.rawData?.optString("icon").orEmpty())
                }.distinctBy { it.id }
                _uiState.update { it.copy(lgWebOsApps = entries,
                    lgWebOsAppsBusy = false, lgWebOsAppsError = null) }
            }
            override fun onError(error: ServiceCommandError?) {
                if (activeDeviceId != deviceId) return
                _uiState.update { it.copy(lgWebOsAppsBusy = false,
                    lgWebOsAppsError = "Não foi possível consultar aplicativos da LG: " +
                        (error?.message ?: "tente novamente")) }
            }
        })
    }

    fun launchLgWebOsApp(id: String) {
        val device = currentConnectableDevice()?.takeIf { it.id == activeDeviceId } ?: return
        val service = device.getServiceByName(WebOSTVService.ID) as? WebOSTVService ?: return
        if (id !in _uiState.value.lgWebOsApps.map { it.id } || !service.isConnected) return
        val deviceId = device.id
        service.launchApp(id, object : Launcher.AppLaunchListener {
            override fun onSuccess(session: LaunchSession?) {
                if (activeDeviceId == deviceId) showStatus("Aplicativo aberto na LG.", isError = false)
            }
            override fun onError(error: ServiceCommandError?) {
                if (activeDeviceId == deviceId) showStatus(
                    "Falha ao abrir aplicativo na LG: " + (error?.message ?: "tente novamente"),
                    isError = true)
            }
        })
    }

'''
patch(vm,
    "    fun launchQuickApp(appName: String) {",
    methods + "    fun launchQuickApp(appName: String) {")
# Do not blindly launch Android shortcut names on webOS.
patch(vm,
    '            showStatus("Atalhos de apps LG ainda não disponíveis; utilize Home.", isError = false)',
    '            showStatus("Abra o aplicativo desejado na lista real da LG.", isError = false)')
patch(nav,
    "                onQuickApp = tvRemoteViewModel::launchQuickApp,",
    "                onQuickApp = tvRemoteViewModel::launchQuickApp,\n"
    "                onLgAppsRefresh = tvRemoteViewModel::refreshLgWebOsApps,\n"
    "                onLgAppLaunch = tvRemoteViewModel::launchLgWebOsApp,")
patch(home,
    "    onQuickApp: (String) -> Unit,",
    "    onQuickApp: (String) -> Unit,\n"
    "    onLgAppsRefresh: () -> Unit,\n"
    "    onLgAppLaunch: (String) -> Unit,")
patch(home,
    '''                    if (sessionState.connectedDevice?.type == "LG webOS") {
                        Button(onClick = { lgStreaming = true }) {
                            Text("Streaming")
                        }
                    } else TclInstalledAppsBar(''',
    '''                    if (sessionState.connectedDevice?.type == "LG webOS") {
                        LgWebOsAppsBar(
                            deviceId = sessionState.connectedDevice?.id.orEmpty(),
                            apps = sessionState.lgWebOsApps,
                            loading = sessionState.lgWebOsAppsBusy,
                            error = sessionState.lgWebOsAppsError,
                            onRefresh = onLgAppsRefresh,
                            onLaunch = onLgAppLaunch
                        )
                    } else TclInstalledAppsBar(''')
# Eliminate the empty fake LG streaming screen: no companion/Android app IDs on webOS.
patch(home,
    '''                    if (lgStreaming && sessionState.connectedDevice?.type == "LG webOS") {
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

''', '')
patch(home,
    "    var lgStreaming by rememberSaveable { mutableStateOf(false) }\n", "")
destination = root / "ui/app/LgWebOsAppsBar.kt"
assert not destination.exists(), "LG_APP_FILE_ALREADY_EXISTS"
destination.write_text(Path("overrides/LgWebOsAppsBar.kt").read_text(encoding="utf-8"), encoding="utf-8")
print("LG_WEBOS_APP_LIST_AND_LAUNCH_APPLIED_NO_TCL")
