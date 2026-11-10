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
    "import com.connectsdk.service.capability.Launcher\n"
    "import org.json.JSONArray\n"
    "import org.json.JSONObject")
patch(vm,
    "    val cast: CastPlaybackUiState = CastPlaybackUiState(),",
    "    val lgWebOsApps: List<LgWebOsApp> = emptyList(),\n"
    "    val lgWebOsAppsDeviceId: String? = null,\n"
    "    val lgWebOsAppsBusy: Boolean = false,\n"
    "    val lgWebOsAppsError: String? = null,\n"
    "    val cast: CastPlaybackUiState = CastPlaybackUiState(),")

# Use only actual launcher IDs from the most recent device query.
# Ignore asynchronous responses from a TV that is no longer the active TV.
methods = '''    private var lgAppsRequestSeq = 0

    // Only store launch points actually returned by this LG. Never reuse a TCL cache.
    private fun restoreLgWebOsApps(deviceId: String) {
        lgAppsRequestSeq += 1
        val raw = prefs.getString("anytv_lg_apps_" + deviceId, null)
        val cached = raw?.let { saved -> runCatching {
            val entries = JSONArray(saved)
            (0 until entries.length()).mapNotNull { index ->
                val item = entries.optJSONObject(index) ?: return@mapNotNull null
                val appId = item.optString("id").trim()
                if (appId.isEmpty()) null else LgWebOsApp(
                    appId, item.optString("name", appId), item.optString("icon"))
            }.distinctBy { it.id }
        }.getOrNull() }
        _uiState.update { it.copy(lgWebOsAppsDeviceId = deviceId,
            lgWebOsApps = cached.orEmpty(), lgWebOsAppsBusy = false, lgWebOsAppsError = null) }
        // A successfully cached empty list is still a completed query; refresh manually.
        if (cached == null) refreshLgWebOsApps()
    }

    fun refreshLgWebOsApps() {
        val device = currentConnectableDevice()?.takeIf { it.id == activeDeviceId } ?: return
        val service = device.getServiceByName(WebOSTVService.ID) as? WebOSTVService ?: return
        if (!service.isConnected || _uiState.value.lgWebOsAppsBusy) return
        val deviceId = device.id
        val requestId = ++lgAppsRequestSeq
        val connectionToken = connectionGeneration
        _uiState.update { state -> state.copy(lgWebOsAppsBusy = true,
            lgWebOsAppsDeviceId = deviceId, lgWebOsAppsError = null,
            lgWebOsApps = if (state.lgWebOsAppsDeviceId == deviceId)
                state.lgWebOsApps else emptyList()) }
        service.getAppList(object : Launcher.AppListListener {
            override fun onSuccess(result: MutableList<AppInfo>?) {
                if (activeDeviceId != deviceId || requestId != lgAppsRequestSeq ||
                    connectionToken != connectionGeneration) return
                val entries = result.orEmpty().mapNotNull { item ->
                    val appId = item.id?.trim().orEmpty()
                    if (appId.isBlank()) null else LgWebOsApp(
                        appId, item.name?.trim()?.takeIf { it.isNotBlank() } ?: appId,
                        item.rawData?.optString("largeIcon").orEmpty().ifBlank {
                            item.rawData?.optString("icon").orEmpty() })
                }.distinctBy { it.id }
                val saved = JSONArray()
                entries.forEach { app -> saved.put(JSONObject()
                    .put("id", app.id).put("name", app.name).put("icon", app.icon)) }
                prefs.edit { putString("anytv_lg_apps_" + deviceId, saved.toString()) }
                _uiState.update { it.copy(lgWebOsApps = entries,
                    lgWebOsAppsBusy = false, lgWebOsAppsError = null) }
            }
            override fun onError(error: ServiceCommandError?) {
                if (activeDeviceId != deviceId || requestId != lgAppsRequestSeq ||
                    connectionToken != connectionGeneration) return
                _uiState.update { it.copy(lgWebOsAppsBusy = false,
                    lgWebOsAppsError = "Não foi possível consultar aplicativos da LG: " +
                        (error?.message ?: "tente novamente")) }
            }
        })
        // The ConnectSDK callback can be lost when the socket drops during a query.
        viewModelScope.launch(Dispatchers.Main) {
            delay(10_000L)
            if (activeDeviceId == deviceId && requestId == lgAppsRequestSeq &&
                connectionToken == connectionGeneration && _uiState.value.lgWebOsAppsBusy) {
                lgAppsRequestSeq += 1
                _uiState.update { it.copy(lgWebOsAppsBusy = false,
                    lgWebOsAppsError = "A LG não respondeu. Toque em Atualizar para tentar novamente.") }
            }
        }
    }

    fun launchLgNetflixTitle(contentId: String) {
        // ConnectSDK has a dedicated Netflix content-ID launcher for webOS.
        // Do not confuse TMDB IDs with Netflix IDs, or claim playback success.
        if (!contentId.matches(Regex("[0-9]{6,9}"))) {
            showStatus("Identificador Netflix inválido.", isError = true)
            return
        }
        val device = currentConnectableDevice()?.takeIf { it.id == activeDeviceId }
        val service = device?.getServiceByName(WebOSTVService.ID) as? WebOSTVService
        if (service == null || !service.isConnected ||
            _uiState.value.lgWebOsApps.none { it.name.equals("Netflix", ignoreCase = true) }) {
            showStatus("Netflix indisponível na LG conectada.", isError = true)
            return
        }
        val deviceId = device.id
        service.launchNetflix(contentId, object : Launcher.AppLaunchListener {
            override fun onSuccess(session: LaunchSession?) {
                if (activeDeviceId == deviceId) showStatus(
                    "Ficha Netflix solicitada na LG. Confirme na TV.", isError = false)
            }
            override fun onError(error: ServiceCommandError?) {
                if (activeDeviceId == deviceId) showStatus(
                    "LG não aceitou a ficha Netflix: " +
                        (error?.message ?: "abra o app e procure o título"), isError = true)
            }
        })
    }

    fun launchLgWebOsApp(id: String) {
        val device = currentConnectableDevice()?.takeIf { it.id == activeDeviceId }
        if (device == null) {
            showStatus("Conecte a LG para abrir o aplicativo.", isError = true)
            return
        }
        val service = device.getServiceByName(WebOSTVService.ID) as? WebOSTVService
        if (service == null || !service.isConnected) {
            showStatus("A sessão webOS está desconectada. Reconecte a LG.", isError = true)
            return
        }
        if (id !in _uiState.value.lgWebOsApps.map { it.id }) {
            showStatus("Aplicativo não identificado nesta LG. Atualize a lista.", isError = true)
            return
        }
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
# Restore per-TV app cache only after the webOS session actually becomes ready.
patch(vm,
    "                publishDeviceSnapshot()\n            }\n\n            override fun onPairingRequired(",
    "                publishDeviceSnapshot()\n"
    "                restoreLgWebOsApps(connectableDevice.id)\n"
    "            }\n\n            override fun onPairingRequired(")
# Do not blindly launch Android shortcut names on webOS.
patch(vm,
    '            showStatus("Atalhos de apps LG ainda não disponíveis; utilize Home.", isError = false)',
    '            showStatus("Abra o aplicativo desejado na lista real da LG.", isError = false)')
patch(nav,
    "                onQuickApp = tvRemoteViewModel::launchQuickApp,",
    "                onQuickApp = tvRemoteViewModel::launchQuickApp,\n"
    "                onLgAppsRefresh = tvRemoteViewModel::refreshLgWebOsApps,\n"
    "                onLgAppLaunch = tvRemoteViewModel::launchLgWebOsApp,\n"
    "                onLgNetflixTitle = tvRemoteViewModel::launchLgNetflixTitle,")
patch(home,
    "    onQuickApp: (String) -> Unit,",
    "    onQuickApp: (String) -> Unit,\n"
    "    onLgAppsRefresh: () -> Unit,\n"
    "    onLgAppLaunch: (String) -> Unit,\n"
    "    onLgNetflixTitle: (String) -> Unit,")
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
                            onLaunch = onLgAppLaunch,
                            onOpenStreaming = { lgStreaming = true }
                        )
                    } else TclInstalledAppsBar(''')
# Keep the existing shared Streaming catalogue route. It displays content without
# pretending that LG uses the TCL companion or that a banner launches a title.
# The LG catalogue must receive REAL app inventory from the LG session, not an
# empty TCL companion list. Prefix webOS IDs so no Android package is fabricated.
patch(home,
    '''                            apps = emptyList(), host = "", pairing = "",''',
    '''                            apps = sessionState.lgWebOsApps.map { app ->
                                TvOfficialApp("lg:" + app.id, app.name, app.icon)
                            }, host = "", pairing = "",
                            isLgWebOs = true,
                            onLaunchLgApp = onLgAppLaunch,
                            onLaunchLgNetflixTitle = onLgNetflixTitle,''')
destination = root / "ui/app/LgWebOsAppsBar.kt"
assert not destination.exists(), "LG_APP_FILE_ALREADY_EXISTS"
destination.write_text(Path("overrides/LgWebOsAppsBar.kt").read_text(encoding="utf-8"), encoding="utf-8")
print("LG_WEBOS_APP_LIST_AND_LAUNCH_APPLIED_NO_TCL")
