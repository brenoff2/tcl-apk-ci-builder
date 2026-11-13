"""Contract tests for TCL ADB phone-only DEV candidate. No device connection."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "overrides/TclAdbInstalledApps.kt"
PATCH = ROOT / "scripts/apply_anytv_tcl_adb.py"


class PhoneAdbContract(unittest.TestCase):
    def test_opt_in_and_companion_fallback(self):
        s = PATCH.read_text()
        self.assertIn('adb_enabled:', s)
        self.assertIn('val list = if (useAdb) {', s)
        self.assertIn('mergeTclInventoryMetadata(allApps, adbApps.inventory(host))', s)
        self.assertIn('else TclTvAppApi.apps(host, pairing)', s)
        self.assertIn('if (useAdb) adbApps.launch(host, app.packageName)', s)
        self.assertIn('if (!useAdb) storePair(pairing)', s)
        self.assertNotIn('adb_enabled:", true', s)

    def test_adb_requires_tv_authorization(self):
        s = SOURCE.read_text()
        self.assertIn('AdbConnection.create(socket, keyPair())', s)
        self.assertIn('Autorize o AnyTV', s)
        self.assertIn('context.filesDir', s)
        self.assertIn('InetSocketAddress(host, 5555)', s)
        self.assertIn('ADB permitido apenas na rede local privada', s)
        self.assertNotIn('adb tcpip 5555', s)
        self.assertNotIn('adb root', s)

    def test_device_truth_not_catalog(self):
        s = SOURCE.read_text()
        self.assertIn('pm list packages', s)
        self.assertIn('android.intent.category.LEANBACK_LAUNCHER', s)
        self.assertIn('android.intent.category.LAUNCHER', s)
        self.assertIn('val launchable = leanback + standard', s)
        self.assertIn('am start -a android.intent.action.MAIN', s)
        self.assertIn('parseLaunchablePackages', s)
        self.assertIn('getPackageArchiveInfo', s)
        self.assertIn('splitSourceDirs', s)
        self.assertIn('getResourcesForApplication(appInfo)', s)
        self.assertIn('ApkFile(files[0])', s)
        self.assertIn('apk.setPreferredLocale(java.util.Locale.forLanguageTag("pt-BR"))', s)
        self.assertIn('val name = meta.label?.trim()', s)
        self.assertIn('apk.allIcons.flatMap { face ->', s)
        self.assertIn('is AdaptiveIcon -> listOfNotNull(face.foreground)', s)
        self.assertIn('val icon = parsed?.second?.let', s)
        self.assertIn('java.util.zip.ZipFile(split)', s)
        self.assertIn('it.name.endsWith("/" + filename)', s)
        self.assertIn('BitmapFactory.Options()', s)
        self.assertIn('lastInventoryWarnings = warnings.take(6)', s)
        self.assertIn('am start -a android.intent.action.MAIN', s)
        self.assertNotIn('TvOfficialApp(pkg, "Netflix"', s)
        self.assertIn('require(appInfo.icon != 0)', s)
        inventory = s[s.index('suspend fun inventory('):s.index('suspend fun openTitleLink(')]
        self.assertNotIn('com.netflix.ninja', inventory)
        self.assertNotIn('com.spotify.tv.android', inventory)

    def test_scope_and_bounded_data(self):
        s = SOURCE.read_text()
        for phrase in ('maxShellBytes', 'maxApkBytes', 'maxApksPerApp',
                       'file.writeBytes', 'files.forEach { runCatching { it.delete() } }',
                       'require(packagePattern.matches(packageName))'):
            self.assertIn(phrase, s)
        self.assertNotIn('pm uninstall', s)
        self.assertNotIn('pm install', s)

    def test_streaming_uses_selected_adb_transport(self):
        s = PATCH.read_text()
        self.assertIn('useTclAdb = useAdb', s)
        self.assertIn('onTclAdbLaunch = { pkg -> adbApps.launch(host, pkg) }', s)
        self.assertIn('if (useTclAdb)', s)
        self.assertIn('onTclAdbLaunch(packageName)', s)
        self.assertIn('onTclAdbLaunch(app.packageName)', s)
        self.assertIn('Link do título enviado ao aplicativo por ADB.', s)
        self.assertIn('foi aceito ou não está disponível', s)
        self.assertIn('STREAM = SRC / "TclStreamingScreens.kt"', s)

    def test_disconnected_shelf_shows_only_edit(self):
        p = PATCH.read_text()
        self.assertIn('val verifiedApps = if (inventoryVerified && host.isNotBlank()) allApps else emptyList()', p)
        self.assertIn('val byPackage = verifiedApps.associateBy { it.packageName }', p)
        self.assertIn('if (inventoryVerified) allApps else emptyList()', p)
        self.assertIn('Text("Editar"', p)
        self.assertIn('if (inventoryVerified) showManager = true', p)
        self.assertIn('if (errorText != null && inventoryVerified && !useAdb)', p)
        self.assertNotIn('ADB Wi-Fi: ativado (toque para usar complemento)', p)

    def test_first_connection_guide_only_in_editor(self):
        p = PATCH.read_text()
        self.assertIn('if (!onboardingSeen)', p)
        self.assertIn('adb_onboarded:', p)
        self.assertIn('showAdbSetup = true', p)
        self.assertIn('Text("3. Toque em Conectar e autorize a chave deste celular na TV."', p)
        self.assertIn('var inventoryVerified by remember(host, useAdb)', p)
        self.assertIn('prefs.getBoolean("adb_onboarded:" + host, false)', p)
        self.assertIn('inventoryVerified = true', p)
        self.assertIn('prefs.getBoolean("adb_enabled:" + host, true)', p)
        self.assertIn('Text("Usar conexão antiga")', p)
        self.assertIn('else showAdbSetup = true', p)
        self.assertIn('enabled = setupHostText.matches(', p)

    def test_connection_persists_across_updates_and_recovers(self):
        p = PATCH.read_text()
        source = SOURCE.read_text()
        self.assertIn('prefs.getString("adb_last_host", "").orEmpty()', p)
        self.assertIn('prefs.edit().putString("adb_last_host", setupHostText)', p)
        self.assertIn('requestedHost = setupHostText', p)
        self.assertIn('var retryMs = 2500L', p)
        self.assertIn('retryMs = (retryMs * 2).coerceAtMost(30000L)', p)
        self.assertIn('adbApps.ping(host)', p)
        self.assertIn('var connectionActive by remember(host, useAdb)', p)
        self.assertIn('Text("Conexão da TCL / Reconectar")', p)
        self.assertIn('Text("IP da TCL na rede Wi-Fi")', p)
        self.assertIn('!prefs.getString("installed:" + host, null).isNullOrBlank()', p)
        self.assertIn('File(context.filesDir, "tcl_adb_keys")', source)
        self.assertIn('suspend fun ping(host: String)', source)
        self.assertNotIn('deleteRecursively', source)

    def test_connection_has_bounded_probe_and_no_infinite_busy_spinner(self):
        patch = PATCH.read_text()
        client = SOURCE.read_text()
        self.assertIn('adbApps.probe(host)', patch)
        self.assertIn('connectionActive = true', patch)
        self.assertIn('busy = false', patch)
        self.assertIn('"Verificando autorização ADB..."', patch)
        self.assertIn('"ADB autorizado; lendo aplicativos..."', patch)
        self.assertIn('A TV aceitou a conexão, mas a leitura dos aplicativos falhou.', patch)
        self.assertIn('private fun <T> withAdb(', client)
        self.assertIn('val watchdog = java.util.Timer(', client)
        self.assertIn('socket.close()', client)
        self.assertIn('suspend fun probe(host: String)', client)
        self.assertIn('withAdb(host, 90000L)', client)

    def test_metadata_failures_visible_without_fake_brand_assets(self):
        patch = PATCH.read_text()
        source = SOURCE.read_text()
        self.assertIn('Text("Diagnóstico da TCL: " + warnings.joinToString("; ")', patch)
        self.assertIn('Text("Atualizar nomes e ícones da TV")', patch)
        self.assertIn('warnings: MutableList<String>', source)
        self.assertIn('falha do parser APK', source)
        self.assertIn('readRealApp(adb, host, pkg, warnings)', source)

    def test_streaming_hosts_agree_across_resolver_and_tcl_adb(self):
        client = SOURCE.read_text()
        personal = (ROOT / "overrides/AnyTvPersonalLinks.kt").read_text()
        catalog = (ROOT / "overrides/TclStreamingScreens.kt").read_text()
        self.assertIn('"app.primevideo.com"', client)
        self.assertIn('"play.hbomax.com"', client)
        self.assertIn('"com.amazon.avod.thirdpartyclient"', client)
        self.assertIn('"com.netflix.mediaclient"', client)
        self.assertIn('https://app.primevideo.com/detail?gti=', personal)
        self.assertIn('https://play.hbomax.com', personal)
        self.assertIn('https://app.primevideo.com/detail?gti=', catalog)
        self.assertIn('https://play.hbomax.com/', catalog)
        # Do not allow arbitrary URLs just because host rules are aligned.
        self.assertIn('uri.userInfo == null && uri.port == -1 && domain in allowed', client)
        self.assertIn('url.none { it ==', client)

    def test_tv_home_launch_fallback_uses_leanback_category(self):
        client = SOURCE.read_text()
        patch = PATCH.read_text()
        self.assertIn('adbApps.launch(host, app.packageName)', patch)
        self.assertIn('android.intent.category.LEANBACK_LAUNCHER', client)
        self.assertIn('"monkey -p " + packageName +', client)
        self.assertIn('" -c android.intent.category.LEANBACK_LAUNCHER 1"', client)
        self.assertNotIn('shell(adb, "monkey -p " + packageName + " 1")', client)

    def test_title_link_over_adb_falls_back_to_app(self):
        p = PATCH.read_text()
        source = SOURCE.read_text()
        self.assertIn('onTclAdbTitleLink = { pkg, url -> adbApps.openTitleLink(host, pkg, url) }', p)
        self.assertIn('StreamingCatalog.titleDeepLink(token, title, packageName)', p)
        self.assertIn('onTclAdbLaunch(packageName)', p)
        self.assertIn('adbApps.lastInventoryWarnings.take(2)', p)
        self.assertIn('Link do título enviado ao aplicativo por ADB.', p)
        self.assertIn('val uri = java.net.URI(url)', source)
        self.assertIn('uri.userInfo == null', source)
        self.assertIn('domain in allowed', source)
        self.assertIn('!response.contains("Error:", ignoreCase = true)', source)

    def test_native_sync_chunks_fit_adblib_v1_payload(self):
        source = SOURCE.read_text()
        self.assertIn('while (offset < dex.size)', source)
        self.assertIn('val length = minOf(2048, dex.size - offset)', source)
        self.assertIn('ByteBuffer.allocate(8 + length)', source)
        self.assertIn('.put(dex, offset, length).array()', source)
        self.assertNotIn('dex.asList().chunked(16384)', source)
        self.assertLessEqual(2048 + 8, 4096)
        self.assertIn('val code = response.copyOfRange(0, 4)', source)
        self.assertIn('if (code == "FAIL")', source)
        self.assertIn('it.message.orEmpty().take(90)', source)

    def test_native_tv_metadata_uses_device_package_manager_without_install(self):
        native = (ROOT / "scripts/qa/AnyTvRemoteMeta.java").read_text()
        client = SOURCE.read_text()
        overlay = PATCH.read_text()
        for phrase in ('queryIntentActivities', 'CATEGORY_LEANBACK_LAUNCHER',
                       'loadLabel(pm)', 'loadIcon(pm)', 'Drawable', 'Bitmap',
                       'ANYTV_REMOTE_META_V1', 'ANYTV_REMOTE_META_END'):
            self.assertIn(phrase, native)
        self.assertIn('CLASSPATH=', client)
        self.assertIn('app_process / anytv.adb.AnyTvRemoteMeta', client)
        self.assertIn('anytv_remote_meta.dex', client)
        self.assertIn('sync:', client)
        self.assertIn('rm -f ', client)
        self.assertIn('mergeTclInventoryMetadata(allApps, adbApps.inventory(host))', overlay)
        self.assertIn('val icon = item.icon.ifBlank { cached?.icon.orEmpty() }', client)
        self.assertIn('cached?.name?.takeIf', client)
        self.assertIn('incompleteRetries < 2', overlay)
        self.assertNotIn('pm install', native)
        self.assertNotIn('pm uninstall', native)

    def test_overlay_only_in_dev(self):
        s = PATCH.read_text()
        self.assertIn('copyfile("overrides/TclAdbInstalledApps.kt"', s)
        self.assertIn('implementation("com.tananaev:adblib:1.3")', s)
        self.assertIn('implementation("net.dongliu:apk-parser:2.6.10")', s)
        self.assertNotIn('updates/android/', s)
        self.assertNotIn('signingConfig', s)


if __name__ == "__main__":
    unittest.main()
