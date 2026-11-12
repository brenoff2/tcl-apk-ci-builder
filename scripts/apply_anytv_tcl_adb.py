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
SOURCE.write_text(text, encoding="utf-8")

gradle = ROOT / "app/build.gradle.kts"
build = gradle.read_text(encoding="utf-8")
assert "com.tananaev:adblib:1.3" not in build
assert "dependencies {" in build
build = build.replace("dependencies {",
    'dependencies {\n    implementation("com.tananaev:adblib:1.3")', 1)
gradle.write_text(build, encoding="utf-8")
print("ANYTV_TCL_ADB_PHONE_DEV_OPT_IN_PREPARED")
