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
        mutableStateOf(host.isNotBlank() && prefs.getBoolean("adb_enabled:" + host, true))
    }
    var inventoryVerified by remember(host, useAdb) { mutableStateOf(false) }
    var showAdbSetup by remember(host) { mutableStateOf(false) }
    var onboardingSeen by remember(host) {
        mutableStateOf(prefs.getBoolean("adb_onboarded:" + host, false))
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
        inventoryVerified = false
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
                inventoryVerified = true
                allApps = list
                storeApps(list)
                if (useAdb) {
                    onboardingSeen = true
                    prefs.edit().putBoolean("adb_onboarded:" + host, true).apply()
                } else storePair(pairing)
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
# No permanent "Use ADB Wi-Fi" banner in the home screen. ADB is the chosen
# TCL mode by default, but connection setup stays accessible under "Editar".
# Only a fresh inventory from the actual connected TV unlocks app icons.
text = replace_once(text,
    '''    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val byPackage = remember(allApps) { allApps.associateBy { it.packageName } }''',
    '''    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val verifiedApps = if (inventoryVerified && host.isNotBlank()) allApps else emptyList()
        val byPackage = verifiedApps.associateBy { it.packageName }''',
    "Only verified inventory appears")

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
            onTclAdbTitleLink = { pkg, url -> adbApps.openTitleLink(host, pkg, url) },
            tvName = tvName.orEmpty(), onDismiss = { showStreaming = false },''',
    "Streaming ADB route")

# Replace the old disconnected CTA and cached-app shelf with an Edit-only shelf.
# A saved cache is never evidence that the current TV is connected.
shelf_start = text.index('''        if (selectedApps.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),''')
shelf_end = text.index('''        // Keep TV streaming app icons above the dynamic 75dp movie banner.''', shelf_start)
old_shelf = text[shelf_start:shelf_end]
assert old_shelf.count('Text("Editar"') == 1, "edit shelf anchor changed"
assert old_shelf.count('OutlinedButton(') == 1, "empty-state anchor changed"
text = text[:shelf_start] + '''        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            selectedApps.forEach { app ->
                Column(
                    modifier = Modifier.width(56.dp).clickable {
                        if (!useAdb && pairing.isBlank()) {
                            showPair = true
                        } else scope.launch {
                            errorText = null
                            runCatching {
                                if (useAdb) adbApps.launch(host, app.packageName)
                                else TclTvAppApi.open(host, pairing, app.packageName)
                            }.onFailure {
                                errorText = it.message ?: ("Não foi possível abrir " + app.name)
                            }.onSuccess { opened ->
                                if (!opened) errorText = "A TCL não confirmou abertura de " + app.name
                            }
                        }
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    OfficialTvIcon(app = app, size = 52.dp)
                    Text(app.name, fontSize = 10.sp, maxLines = 1,
                        color = androidx.compose.ui.graphics.Color.White,
                        textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFF1C1E25))
                    .clickable {
                        if (inventoryVerified) showManager = true
                        else if (useAdb) showAdbSetup = true
                        else showPair = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Editar", color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
''' + text[shelf_end:]
# Do not display legacy companion connection errors below the film banner.
# The current error is available in the Edit/setup dialog instead.
text = replace_once(text,
    '''        if (errorText != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {''',
    '''        if (errorText != null && inventoryVerified) {
            Row(verticalAlignment = Alignment.CenterVertically) {''',
    "Hide disconnected error banner")
text = replace_once(text,
    '''        TclStreamingScreens(apps = allApps, host = host, pairing = pairing,''',
    '''        TclStreamingScreens(apps = if (inventoryVerified) allApps else emptyList(),
            host = host, pairing = pairing,''',
    "No stale apps in Streaming screen")
text = replace_once(text,
    '''    if (showPair) {
        AlertDialog(''',
    '''    if (showAdbSetup) {
        AlertDialog(
            onDismissRequest = { showAdbSetup = false },
            title = { Text("Conectar aplicativos da TCL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!onboardingSeen) {
                        Text("1. Na TCL, ative Depuração USB nas opções do desenvolvedor.",
                            fontSize = 13.sp)
                        Text("2. Conecte celular e TCL na mesma rede Wi-Fi.",
                            fontSize = 13.sp)
                        Text("3. Toque em Conectar e autorize a chave deste celular na TV.",
                            fontSize = 13.sp)
                    } else {
                        Text("A leitura dos aplicativos não está conectada. " +
                            "Use Conectar para tentar novamente.", fontSize = 13.sp)
                    }
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    if (!errorText.isNullOrBlank()) {
                        Text(errorText.orEmpty(), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    errorText = null
                    refresh++
                    showAdbSetup = false
                }) { Text("Conectar") }
            },
            dismissButton = {
                TextButton(onClick = { showAdbSetup = false }) { Text("Cancelar") }
            }
        )
    }

    if (showPair) {
        AlertDialog(''',
    "First connection instructions only in Edit dialog")
# Keep the fallback switch inside the favorites manager, not on the home shelf.
text = replace_once(text,
    '''                    Text("Editar aplicativos da TV", fontWeight = FontWeight.Bold, fontSize = 20.sp)''',
    '''                    Text("Editar aplicativos da TV", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    TextButton(onClick = {
                        useAdb = !useAdb
                        prefs.edit().putBoolean("adb_enabled:" + host, useAdb).apply()
                        showManager = false
                        errorText = null
                        refresh++
                    }) {
                        Text(if (useAdb) "Usar conexão antiga da TCL" else "Usar ADB Wi-Fi")
                    }''',
    "Fallback inside editor")
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
    onTclAdbTitleLink: suspend (String, String) -> Boolean = { _, _ -> false },
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
                    // Prefer a provider-specific HTTPS title link when one is
                    // available; a launcher command alone cannot open a title.
                    val direct = runCatching {
                        AnyTvPersonalLinks.resolve(context, title, packageName)
                    }.getOrNull()
                    if (direct is AnyTvPersonalResult.NotInSubscription) {
                        titleActionFeedback = "Este título não está confirmado na assinatura escolhida."
                        return@launch
                    }
                    val personalUrl = (direct as? AnyTvPersonalResult.Direct)?.url
                    val appName = apps.firstOrNull { it.packageName == packageName }?.name
                        ?: "serviço selecionado"
                    val officialUrl = if (personalUrl == null) {
                        val available = runCatching {
                            StreamingCatalog.titleAvailableInBrazil(token, title, appName)
                        }.getOrNull()
                        if (available == true) runCatching {
                            StreamingCatalog.titleDeepLink(token, title, packageName)
                        }.getOrNull() else null
                    } else null
                    val url = personalUrl ?: officialUrl
                    val requested = url != null && runCatching {
                        onTclAdbTitleLink(packageName, url)
                    }.getOrDefault(false)
                    if (requested) {
                        titleActionFeedback = "Link do título enviado ao aplicativo por ADB. " +
                            "Confira na TCL se a ficha correta abriu."
                    } else {
                        val ok = onTclAdbLaunch(packageName)
                        titleActionFeedback = if (ok) {
                            "Aplicativo aberto via ADB. O link específico do título não " +
                                "foi aceito ou não está disponível; procure " + title.name + " na TV."
                        } else {
                            "A TCL não confirmou a abertura do aplicativo. Confira a TV."
                        }
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
