"""AnyTV account, TV dialogs and selected streaming controls share one UI contract."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
STREAM = ROOT / "overrides/TclStreamingScreens.kt"
TV = ROOT / "overrides/TclInstalledApps.kt"
GRAPHITE = ROOT / "scripts/apply_anytv_graphite_settings.py"

class AnyTvUxConsistency(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.stream = STREAM.read_text(encoding="utf-8")
        cls.tv = TV.read_text(encoding="utf-8")
        cls.settings = GRAPHITE.read_text(encoding="utf-8")

    def test_active_chips_are_solid_red_with_white_content(self):
        s = self.stream
        chip = s.split('private fun AnyTvCompactChip(', 1)[1].split('private fun AnyTvGenreViewIcon(', 1)[0]
        view = s.split('private fun AnyTvGenreViewIcon(', 1)[1].split('internal fun TclStreamingScreens(', 1)[0]
        for component in (chip, view):
            self.assertIn('Color(0xFFE9003B)', component)
            self.assertIn('Color.White', component)
            self.assertNotIn('Color(0xFF422128)', component)
            self.assertNotIn('.copy(alpha = .32f)', component)
        self.assertIn('selectedContainerColor = Color(0xFFE9003B)', s)
        self.assertIn('containerColor = if (chosen) Color(0xFFE9003B)', s)

    def test_account_shows_verified_tv_library_and_private_data_sections(self):
        account = self.stream.split('private fun AnyTvAccountPage(', 1)[1].split('private fun AnyTvStreamOutlinedButton(', 1)[0]
        for item in ('Minhas TVs', 'TV conectada', 'Minha biblioteca', 'Favoritos', 'Assistidos',
                     'Minhas séries', 'Dados e privacidade', 'Gerenciar sessão',
                     'onLibrary: (String) -> Unit', 'onRemote: () -> Unit'):
            self.assertIn(item, account)
        self.assertIn('onLibrary = { route ->', self.stream)
        self.assertIn('onRemote = onDismiss', self.stream)
        self.assertIn('tvName = tvName', self.stream)
        self.assertIn('tvConnected =', self.stream)
        self.assertIn('clearAccountSection = it', self.stream)
        self.assertIn('A exclusão definitiva exige uma operação verificada no servidor.', account)

    def test_tv_pairing_and_installed_apps_preserve_real_source(self):
        tv = self.tv
        self.assertIn('Código de 8 dígitos', tv)
        self.assertIn('Aplicativos da TV', tv)
        self.assertIn('Código ou conexão inválidos.', tv)
        self.assertIn('runCatching { TclTvAppApi.apps(host, codeEntry) }', tv)
        self.assertIn('allApps.sortedWith(', tv)
        self.assertIn('prefs.edit().putString("anytv_catalog_hidden_installed"', tv)

    def test_native_discovery_polish_preserves_protocol(self):
        script = (ROOT / "scripts/apply_anytv_internal_pages_ux.py").read_text(encoding="utf-8")
        for fragment in ('DeviceDiscoveryScreen.kt', 'SettingsScreen.kt',
                         'Color(0xFFE9003B)', 'contentColor = Color.White',
                         'checkedTrackColor = Color(0xFFE9003B)',
                         'TV action targets', 'raise RuntimeError'):
            self.assertIn(fragment, script)
        self.assertNotIn('onConnect(device.id) =', script)
        self.assertNotIn('onSubmitPairingCode =', script)

    def test_session_dialog_retains_real_gateway_and_graphite(self):
        session = (ROOT / "overrides/AnyTvAccountFlow.kt").read_text(encoding="utf-8")
        self.assertIn('containerColor = androidx.compose.ui.graphics.Color(0xFF292929)', session)
        self.assertIn('gateway.restoreSession()', session)
        self.assertIn('gateway.signOut()', session)
        self.assertIn('gateway.signInWithGoogle()', session)

    def test_native_settings_and_updater_remain_single_source(self):
        self.assertIn('Settings group renderer changed', self.settings)
        self.assertIn('AppBackdrop', self.settings)
        self.assertIn('"settings" -> {', self.stream)
        self.assertIn('onOpenAppSettings()', self.stream)

if __name__ == "__main__":
    unittest.main()
