#!/usr/bin/env python3
"""Opt-in DEV overlay for TCL's companion-free ADB-LAN application shelf.

Run AFTER prepare_tcl.py and the existing LG overlays. Never invoked by OTA.
"""
from pathlib import Path
from shutil import copyfile

ROOT = Path("source")
SRC = ROOT / "app/src/main/java/com/hari/androidtvremote/ui/app"
SOURCE = SRC / "TclInstalledApps.kt"
assert SOURCE.is_file(), "Run approved base prepare first"
assert not (SRC / "TclAdbInstalledApps.kt").exists(), "ADB overlay already applied"


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one anchor, got {count}")
    return text.replace(old, new, 1)


copyfile("overrides/TclAdbInstalledApps.kt", SRC / "TclAdbInstalledApps.kt")
text = SOURCE.read_text(encoding="utf-8")
text = replace_once(text,
    "    var showPair by remember(deviceKey, host) { mutableStateOf(false) }",
    '''    val adbApps = remember(context) { TclAdbInstalledApps(context.applicationContext) }
    var useAdb by remember(host) {
        mutableStateOf(host.isNotBlank() && prefs.getBoolean("adb_enabled:" + host, false))
    }
    var showPair by remember(deviceKey, host) { mutableStateOf(false) }''', "ADB state")
text = replace_once(text,
    "if (host.isNotBlank() && pairing.isBlank()) {",
    "if (host.isNotBlank() && pairing.isBlank() && !useAdb) {", "migration guard")
text = replace_once(text,
    '''    LaunchedEffect(host, pairing, refresh) {
        if (host.isNotBlank() && pairing.isNotBlank()) {
            busy = true
            val firstAttempt = runCatching { TclTvAppApi.apps(host, pairing) }
            val result = if (firstAttempt.isFailure && refresh == 0) {
                // Companion may still be starting after TV reconnect.
                kotlinx.coroutines.delay(1400L)
                runCatching { TclTvAppApi.apps(host, pairing) }
            } else firstAttempt
            result.onSuccess { list ->
                allApps = list
                storeApps(list)
                storePair(pairing)
                errorText = null
            }.onFailure {
                errorText = it.message ?: "Não foi possível consultar aplicativos da TCL."
                if (it.message?.contains("Código incorreto") == true) {
                    codeEntry = pairing
                    showPair = true
                }
                // Keep the last confirmed TV icons visible during outages.
            }
            busy = false
        }
    }''',
    '''    LaunchedEffect(host, pairing, refresh, useAdb) {
        if (host.isNotBlank() && (useAdb || pairing.isNotBlank())) {
            busy = true
            val firstAttempt = runCatching {
                if (useAdb) adbApps.inventory(host) else TclTvAppApi.apps(host, pairing)
            }
            val result = if (!useAdb && firstAttempt.isFailure && refresh == 0) {
                kotlinx.coroutines.delay(1400L)
                runCatching { TclTvAppApi.apps(host, pairing) }
            } else firstAttempt
            result.onSuccess { list ->
                allApps = list
                storeApps(list)
                if (!useAdb) storePair(pairing)
                errorText = null
            }.onFailure {
                errorText = it.message ?: "Não foi possível consultar aplicativos da TCL."
                if (!useAdb && it.message?.contains("Código incorreto") == true) {
                    codeEntry = pairing
                    showPair = true
                }
                // Keep last confirmed apps visible, never replace with invented data.
            }
            busy = false
        }
    }''', "ADB fetch and fallback")
text = replace_once(text,
    '''    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val byPackage''',
    '''    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (host.isNotBlank()) {
            TextButton(onClick = {
                useAdb = !useAdb
                prefs.edit().putBoolean("adb_enabled:" + host, useAdb).apply()
                errorText = null
                refresh++
            }) {
                Text(if (useAdb) "ADB Wi-Fi: ativado (toque para usar complemento)"
                    else "Usar ADB Wi-Fi (depuração autorizada na TV)",
                    fontSize = 11.sp)
            }
        }
        val byPackage''', "ADB opt-in toggle")
text = replace_once(text,
    '''                            if (pairing.isBlank()) {
                                showPair = true
                            } else scope.launch {
                                errorText = null
                                runCatching { TclTvAppApi.open(host, pairing, app.packageName) }''',
    '''                            if (!useAdb && pairing.isBlank()) {
                                showPair = true
                            } else scope.launch {
                                errorText = null
                                runCatching {
                                    if (useAdb) adbApps.launch(host, app.packageName)
                                    else TclTvAppApi.open(host, pairing, app.packageName)
                                }''', "ADB app launch")
text = replace_once(text,
    '''                    else if (pairing.isBlank()) showPair = true
                    else if (allApps.isEmpty()) refresh++''',
    '''                    else if (!useAdb && pairing.isBlank()) showPair = true
                    else if (allApps.isEmpty()) refresh++''', "ADB empty action")
text = replace_once(text,
    '''                     else if (pairing.isBlank()) "Vincular leitura de aplicativos da TV"''',
    '''                     else if (!useAdb && pairing.isBlank()) "Vincular leitura de aplicativos da TV"''',
    "ADB empty label")
text = replace_once(text,
    '''                    if (pairing.isNotBlank()) refresh++ else showPair = true''',
    '''                    if (useAdb || pairing.isNotBlank()) refresh++ else showPair = true''',
    "ADB retry")
# Share the selected TCL transport with the Streaming film/series screens.
# Android TV Remote v2 and ADB app launch are distinct from title-specific deep links.
text = replace_once(text,
    '''        TclStreamingScreens(apps = allApps, host = host, pairing = pairing,
            tvName = tvName.orEmpty(), onDismiss = { showStreaming = false },''',
    '''        TclStreamingScreens(apps = allApps, host = host, pairing = pairing,
            useTclAdb = useAdb,
            onTclAdbLaunch = { pkg -> adbApps.launch(host, pkg) },
            tvName = tvName.orEmpty(), onDismiss = { showStreaming = false },''',
    "Streaming ADB route")
SOURCE.write_text(text, encoding="utf-8")

gradle = ROOT / "app/build.gradle.kts"
build = gradle.read_text(encoding="utf-8")
assert "com.tananaev:adblib:1.3" not in build
assert "dependencies {" in build
build = build.replace("dependencies {",
    'dependencies {\n    implementation("com.tananaev:adblib:1.3")', 1)
gradle.write_text(build, encoding="utf-8")

# Inject a standalone, fail-closed TCL ADB branch in the shared streaming screen.
# Do not change LG routes or the existing companion title/deeplink behavior.
STREAM = SRC / "TclStreamingScreens.kt"
stream = STREAM.read_text(encoding="utf-8")
stream = replace_once(stream,
    '''    apps: List<TvOfficialApp>, host: String, pairing: String,
    tvName: String, onDismiss: () -> Unit,''',
    '''    apps: List<TvOfficialApp>, host: String, pairing: String,
    useTclAdb: Boolean = false,
    onTclAdbLaunch: suspend (String) -> Boolean = { false },
    tvName: String, onDismiss: () -> Unit,''',
    "Streaming route parameters")
stream = replace_once(stream,
    '''        if (host.isBlank() || pairing.isBlank()) {
            message = "Conecte o AnyTV Connect e vincule a TV antes de abrir aplicativos."''',
    '''        if (useTclAdb) {
            if (host.isBlank() || apps.none { it.packageName == app.packageName }) {
                message = "Aplicativo não encontrado na TCL conectada."
            } else scope.launch {
                message = "Enviando abertura do aplicativo por ADB Wi-Fi..."
                runCatching { onTclAdbLaunch(app.packageName) }
                    .onSuccess { ok ->
                        message = if (ok) "Comando ADB enviado à TCL; confira a TV."
                            else "A TCL não confirmou o comando ADB para este aplicativo."
                    }
                    .onFailure { error ->
                        message = error.message ?: "Não foi possível abrir pelo ADB Wi-Fi."
                    }
            }
            return
        }
        if (host.isBlank() || pairing.isBlank()) {
            message = "Conecte o AnyTV Connect e vincule a TV antes de abrir aplicativos."''',
    "Streaming launch by ADB")
stream = replace_once(stream,
    '''        if (host.isBlank() || pairing.isBlank()) {
            titleActionFeedback = "Vincule o AnyTV Connect com o código de oito dígitos. " +
                "A conexão do controle remoto é independente."
            return
        }''',
    '''        if (useTclAdb) {
            if (host.isBlank() || apps.none { it.packageName == packageName }) {
                titleActionFeedback = "Selecione um aplicativo instalado na TCL."
                return
            }
            titleActionPending = true
            showPlaybackRemote = true
            remoteMinimized = false
            titleActionFeedback = "Abrindo aplicativo por ADB Wi-Fi..."
            scope.launch {
                try {
                    val ok = onTclAdbLaunch(packageName)
                    titleActionFeedback = if (ok) {
                        "Comando de abertura enviado ao aplicativo na TCL. " +
                            "A ficha de " + title.name +
                            " não foi aberta automaticamente; procure o título na TV."
                    } else {
                        "A TCL não confirmou a abertura do aplicativo. Confira a TV."
                    }
                } catch (error: Exception) {
                    titleActionFeedback = error.message
                        ?: "Falha ao enviar abertura por ADB Wi-Fi."
                } finally {
                    titleActionPending = false
                }
            }
            return
        }
        if (host.isBlank() || pairing.isBlank()) {
            titleActionFeedback = "Vincule o AnyTV Connect com o código de oito dígitos. " +
                "A conexão do controle remoto é independente."
            return
        }''',
    "TCL title fallback to ADB app launch")
STREAM.write_text(stream, encoding="utf-8")

print("ANYTV_TCL_ADB_PHONE_DEV_OPT_IN_PREPARED")
