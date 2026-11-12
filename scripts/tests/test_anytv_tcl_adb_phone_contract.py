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
        self.assertIn('val list = if (useAdb) adbApps.inventory(host)', s)
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
        self.assertIn('apk.allIcons.filterIsInstance<Icon>()', s)
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

    def test_overlay_only_in_dev(self):
        s = PATCH.read_text()
        self.assertIn('copyfile("overrides/TclAdbInstalledApps.kt"', s)
        self.assertIn('implementation("com.tananaev:adblib:1.3")', s)
        self.assertIn('implementation("net.dongliu:apk-parser:2.6.10")', s)
        self.assertNotIn('updates/android/', s)
        self.assertNotIn('signingConfig', s)


if __name__ == "__main__":
    unittest.main()
